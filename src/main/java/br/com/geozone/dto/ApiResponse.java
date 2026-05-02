package br.com.geozone.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private boolean sucesso;
    private String mensagem;
    private T dados;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public static <T> ApiResponse<T> ok(T dados) {
        return ApiResponse.<T>builder()
                .sucesso(true)
                .mensagem("Sucesso")
                .dados(dados)
                .build();
    }

    public static <T> ApiResponse<T> ok(String mensagem, T dados) {
        return ApiResponse.<T>builder()
                .sucesso(true)
                .mensagem(mensagem)
                .dados(dados)
                .build();
    }

    public static <T> ApiResponse<T> erro(String mensagem) {
        return ApiResponse.<T>builder()
                .sucesso(false)
                .mensagem(mensagem)
                .build();
    }
}
