package org.fyp.minidrivestoragebe.dto.file;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.minidrivestoragebe.enums.PermissionLevel;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareFileRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotNull(message = "Permission level is required")
    private PermissionLevel permission;
}
