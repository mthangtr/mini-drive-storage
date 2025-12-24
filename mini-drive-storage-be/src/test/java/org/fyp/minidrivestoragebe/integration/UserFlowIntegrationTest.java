package org.fyp.minidrivestoragebe.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.fyp.minidrivestoragebe.dto.auth.AuthResponse;
import org.fyp.minidrivestoragebe.dto.auth.LoginRequest;
import org.fyp.minidrivestoragebe.dto.auth.RegisterRequest;
import org.fyp.minidrivestoragebe.dto.file.CreateFolderRequest;
import org.fyp.minidrivestoragebe.dto.file.FileItemResponse;
import org.fyp.minidrivestoragebe.dto.file.ShareFileRequest;
import org.fyp.minidrivestoragebe.dto.response.ApiResponse;
import org.fyp.minidrivestoragebe.entity.User;
import org.fyp.minidrivestoragebe.enums.PermissionLevel;
import org.fyp.minidrivestoragebe.repository.FileItemRepository;
import org.fyp.minidrivestoragebe.repository.FilePermissionRepository;
import org.fyp.minidrivestoragebe.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Integration Tests - Full User Flow")
class UserFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileItemRepository fileItemRepository;

    @Autowired
    private FilePermissionRepository filePermissionRepository;

    private String userToken;
    private String otherUserToken;
    private String testUserEmail = "testuser@integration.com";
    private String otherUserEmail = "otheruser@integration.com";

    @BeforeEach
    void setUp() throws Exception {
        // Clean up data before each test
        filePermissionRepository.deleteAll();
        fileItemRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Complete user flow: Register -> Login -> Upload -> Share -> Download")
    void completeUserFlow() throws Exception {
        // Step 1: Register first user
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email(testUserEmail)
                .password("password123")
                .fullName("Test User")
                .build();

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").exists())
                .andReturn();

        String registerResponse = registerResult.getResponse().getContentAsString();
        ApiResponse<AuthResponse> authApiResponse = objectMapper.readValue(
                registerResponse,
                objectMapper.getTypeFactory().constructParametricType(
                        ApiResponse.class, AuthResponse.class)
        );
        userToken = authApiResponse.getData().getToken();

        assertThat(userToken).isNotNull();

        // Step 2: Login with same user
        LoginRequest loginRequest = LoginRequest.builder()
                .email(testUserEmail)
                .password("password123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").exists());

        // Step 3: Create a folder
        CreateFolderRequest folderRequest = CreateFolderRequest.builder()
                .name("Test Folder")
                .parentId(null)
                .build();

        MvcResult folderResult = mockMvc.perform(post("/api/v1/files")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(folderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Test Folder"))
                .andReturn();

        String folderResponse = folderResult.getResponse().getContentAsString();
        ApiResponse<FileItemResponse> folderApiResponse = objectMapper.readValue(
                folderResponse,
                objectMapper.getTypeFactory().constructParametricType(
                        ApiResponse.class, FileItemResponse.class)
        );
        String folderId = folderApiResponse.getData().getId();

        // Step 4: Upload a file
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Hello World".getBytes()
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/files")
                        .file(file)
                        .param("parentId", folderId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andReturn();

        // Step 5: Register second user for sharing
        RegisterRequest otherRegisterRequest = RegisterRequest.builder()
                .email(otherUserEmail)
                .password("password123")
                .fullName("Other User")
                .build();

        MvcResult otherRegisterResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otherRegisterRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String otherRegisterResponse = otherRegisterResult.getResponse().getContentAsString();
        ApiResponse<AuthResponse> otherAuthApiResponse = objectMapper.readValue(
                otherRegisterResponse,
                objectMapper.getTypeFactory().constructParametricType(
                        ApiResponse.class, AuthResponse.class)
        );
        otherUserToken = otherAuthApiResponse.getData().getToken();

        // Step 6: Share folder with other user
        ShareFileRequest shareRequest = ShareFileRequest.builder()
                .email(otherUserEmail)
                .permission(PermissionLevel.VIEW)
                .build();

        mockMvc.perform(post("/api/v1/files/" + folderId + "/share")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shareRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sharedWithEmail").value(otherUserEmail));

        // Step 7: Other user checks shared files
        mockMvc.perform(get("/api/v1/files/shared-with-me")
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("Test Folder"));

        // Step 8: List files
        mockMvc.perform(get("/api/v1/files")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        // Step 9: Get usage statistics
        mockMvc.perform(get("/api/v1/analytics/usage")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalFiles").value(1))
                .andExpect(jsonPath("$.data.totalFolders").value(1));

        // Step 10: Delete folder (soft delete)
        mockMvc.perform(delete("/api/v1/files/" + folderId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Should handle unauthorized access")
    void shouldHandleUnauthorizedAccess() throws Exception {
        // Try to access protected endpoint without token
        mockMvc.perform(get("/api/v1/files"))
                .andExpect(status().isForbidden());

        // Try to access with invalid token
        mockMvc.perform(get("/api/v1/files")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should prevent access to other user's files")
    void shouldPreventAccessToOtherUsersFiles() throws Exception {
        // Register two users
        RegisterRequest user1Request = RegisterRequest.builder()
                .email("user1@test.com")
                .password("password123")
                .fullName("User 1")
                .build();

        MvcResult user1Result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user1Request)))
                .andExpect(status().isCreated())
                .andReturn();

        String user1Response = user1Result.getResponse().getContentAsString();
        ApiResponse<AuthResponse> user1AuthResponse = objectMapper.readValue(
                user1Response,
                objectMapper.getTypeFactory().constructParametricType(
                        ApiResponse.class, AuthResponse.class)
        );
        String user1Token = user1AuthResponse.getData().getToken();

        RegisterRequest user2Request = RegisterRequest.builder()
                .email("user2@test.com")
                .password("password123")
                .fullName("User 2")
                .build();

        MvcResult user2Result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user2Request)))
                .andExpect(status().isCreated())
                .andReturn();

        String user2Response = user2Result.getResponse().getContentAsString();
        ApiResponse<AuthResponse> user2AuthResponse = objectMapper.readValue(
                user2Response,
                objectMapper.getTypeFactory().constructParametricType(
                        ApiResponse.class, AuthResponse.class)
        );
        String user2Token = user2AuthResponse.getData().getToken();

        // User 1 creates a file
        CreateFolderRequest folderRequest = CreateFolderRequest.builder()
                .name("Private Folder")
                .parentId(null)
                .build();

        MvcResult folderResult = mockMvc.perform(post("/api/v1/files")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(folderRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String folderResponse = folderResult.getResponse().getContentAsString();
        ApiResponse<FileItemResponse> folderApiResponse = objectMapper.readValue(
                folderResponse,
                objectMapper.getTypeFactory().constructParametricType(
                        ApiResponse.class, FileItemResponse.class)
        );
        String folderId = folderApiResponse.getData().getId();

        // User 2 tries to access User 1's file - should be unauthorized
        mockMvc.perform(get("/api/v1/files/" + folderId)
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isUnauthorized());
    }
}
