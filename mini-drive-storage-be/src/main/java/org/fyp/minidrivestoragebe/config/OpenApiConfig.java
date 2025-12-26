package org.fyp.minidrivestoragebe.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Mini-Drive Storage Platform API",
                version = "1.0",
                description = """
                        RESTful API for a cloud storage platform similar to Google Drive.
                        
                        ## Features
                        - User authentication with JWT
                        - File and folder management with unified endpoint
                        - Async folder download with ZIP compression
                        - File sharing with permission control (VIEW/EDIT)
                        - Recursive permission for folders
                        - Search and filtering capabilities
                        - Storage usage analytics
                        - Scheduled cleanup of deleted files
                        
                        ## Authentication
                        Most endpoints require JWT authentication. Use the `/api/v1/auth/login` or 
                        `/api/v1/auth/register` endpoint to obtain a token, then include it in the 
                        Authorization header as: `Bearer {token}`
                        
                        ## Quick Start
                        1. Register a new account: `POST /api/v1/auth/register`
                        2. Login to get token: `POST /api/v1/auth/login`
                        3. Create folder: `POST /api/v1/files` (JSON body)
                        4. Upload files: `POST /api/v1/files` (multipart/form-data)
                        5. Share with others: `POST /api/v1/files/{id}/share`
                        """,
                contact = @Contact(
                        name = "API Support",
                        email = "support@minidrive.com"
                ),
                license = @License(
                        name = "MIT License",
                        url = "https://opensource.org/licenses/MIT"
                )
        ),
        servers = {
                @Server(
                        description = "Local Development",
                        url = "http://localhost:8080"
                ),
                @Server(
                        description = "Production",
                        url = "https://api.minidrive.example.com"
                )
        },
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        description = "JWT authentication. Use the login endpoint to obtain a token.",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
}
