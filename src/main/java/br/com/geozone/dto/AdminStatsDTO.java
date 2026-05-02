package br.com.geozone.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsDTO {
    private long totalUsuarios;
    private long usuariosAtivos;
    private long usuariosPro;
    private long totalFavoritos;
    private long totalConsultas;
    private String mrrFormatado;
    private List<UsuarioResumo> ultimosUsuarios;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsuarioResumo {
        private Long id;
        private String nome;
        private String email;
        private String plano;
        private String criadoEm;
    }
}
