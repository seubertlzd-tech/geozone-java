package br.com.geozone.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AdminLoginRequest {

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    private String senha;

    @NotBlank(message = "Código 2FA é obrigatório")
    @Pattern(regexp = "\\d{6}", message = "Código 2FA deve ter exatamente 6 dígitos")
    private String token2fa;
}
