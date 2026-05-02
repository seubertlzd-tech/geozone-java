package br.com.geozone.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

public class GeoDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParcelaResponse {
        private Long osmId;
        private String tipo;
        private String landuse;
        private String endereco;
        private String cidade;
        private String estado;
        private String cep;
        private String area;
        private Double latitude;
        private Double longitude;
        private Map<String, String> tags;
        private List<ViabilidadeItem> viabilidade;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ViabilidadeItem {
        private String label;
        private String status; // ok | warn | no
        private String valor;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NominatimResult {
        private String displayName;
        private String road;
        private String houseNumber;
        private String city;
        private String state;
        private String postcode;
        private Double lat;
        private Double lon;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchResult {
        private String nome;
        private String enderecoCurto;
        private String enderecoCompleto;
        private Double latitude;
        private Double longitude;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicaoRequest {
        private List<Ponto> pontos;
        private String tipo; // distancia | area
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Ponto {
        private Double lat;
        private Double lng;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicaoResponse {
        private String tipo;
        private Double valor;
        private String unidade;
        private String valorFormatado;
    }
}
