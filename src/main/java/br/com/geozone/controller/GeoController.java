package br.com.geozone.controller;

import br.com.geozone.dto.ApiResponse;
import br.com.geozone.dto.GeoDTO;
import br.com.geozone.service.GeoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/geo")
@RequiredArgsConstructor
public class GeoController {

    private final GeoService geoService;

    /**
     * GET /api/geo/parcela?lat=&lon=
     * Retorna dados da parcela OSM mais próxima da coordenada
     */
    @GetMapping("/parcela")
    public ResponseEntity<ApiResponse<GeoDTO.ParcelaResponse>> buscarParcela(
            @RequestParam Double lat,
            @RequestParam Double lon,
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails != null ? userDetails.getUsername() : null;
        GeoDTO.ParcelaResponse resp = geoService.buscarParcela(lat, lon, email);
        return ResponseEntity.ok(ApiResponse.ok(resp));
    }

    /**
     * GET /api/geo/buscar?q=
     * Busca endereços via Nominatim (público)
     */
    @GetMapping("/buscar")
    public ResponseEntity<ApiResponse<List<GeoDTO.SearchResult>>> buscarEndereco(
            @RequestParam String q) {

        if (q == null || q.trim().length() < 2) {
            return ResponseEntity.badRequest().body(ApiResponse.erro("Mínimo de 2 caracteres para busca"));
        }
        List<GeoDTO.SearchResult> resultados = geoService.buscarEndereco(q.trim());
        return ResponseEntity.ok(ApiResponse.ok(resultados));
    }

    /**
     * GET /api/geo/reverse?lat=&lon=
     * Geocodificação reversa via Nominatim
     */
    @GetMapping("/reverse")
    public ResponseEntity<ApiResponse<GeoDTO.NominatimResult>> reverseGeocode(
            @RequestParam Double lat,
            @RequestParam Double lon) {

        GeoDTO.NominatimResult result = geoService.reverseGeocode(lat, lon);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * POST /api/geo/medir
     * Calcula distância ou área de um conjunto de pontos
     */
    @PostMapping("/medir")
    public ResponseEntity<ApiResponse<GeoDTO.MedicaoResponse>> calcularMedicao(
            @Valid @RequestBody GeoDTO.MedicaoRequest req) {

        GeoDTO.MedicaoResponse resp = geoService.calcularMedicao(req);
        return ResponseEntity.ok(ApiResponse.ok(resp));
    }
}
