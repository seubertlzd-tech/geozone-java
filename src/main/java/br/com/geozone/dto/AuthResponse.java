package br.com.geozone.dto;

import br.com.geozone.model.Usuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String tipo;
    private Long id;
    private String nome;
    private String email;
    private Usuario.Plano plano;
    private Usuario.Role role;
    private Boolean isAdmin;
}
