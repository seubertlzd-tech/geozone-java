package br.com.geozone.controller;

import br.com.geozone.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class InfoController {

    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> info() {
        return ApiResponse.ok(Map.of(
            "app", "GeoZone API",
            "versao", "2.0.0",
            "status", "online",
            "timestamp", LocalDateTime.now().toString(),
            "endpoints", Map.of(
                "auth", "/api/auth/cadastrar | /api/auth/login | /api/auth/admin/login",
                "geo", "/api/geo/parcela | /api/geo/buscar | /api/geo/reverse | /api/geo/medir",
                "favoritos", "/api/favoritos",
                "admin", "/api/admin/stats | /api/admin/usuarios",
                "h2", "/h2-console"
            )
        ));
    }
}
