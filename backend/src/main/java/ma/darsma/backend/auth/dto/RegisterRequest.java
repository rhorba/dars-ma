package ma.darsma.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ma.darsma.backend.auth.Role;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 10) String password,
        @NotNull Role role,
        @NotBlank String fullName
) {
}
