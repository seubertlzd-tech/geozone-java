package br.com.geozone.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class FavoritoDTO {

    @Data
    public static class Request {
        @NotBlank(message = "Endereço é obrigatório")
        private String endereco;
        private String cidade;
        private String estado;
        private String tipo;
        private String area;
        @NotNull(message = "Latitude é obrigatória")
        private Double latitude;
        @NotNull(message = "Longitude é obrigatória")
        private Double longitude;
        private Long osmId;
        private String landuse;
        private String nome;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String endereco;
        private String cidade;
        private String estado;
        private String tipo;
        private String area;
        private Double latitude;
        private Double longitude;
        private Long osmId;
        private String landuse;
        private String nome;
        private LocalDateTime criadoEm;
    }
}
