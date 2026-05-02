package br.com.geozone.controller;

import br.com.geozone.dto.ApiResponse;
import br.com.geozone.dto.FavoritoDTO;
import br.com.geozone.service.FavoritoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favoritos")
@RequiredArgsConstructor
public class FavoritoController {

    private final FavoritoService favoritoService;

    /** GET /api/favoritos */
    @GetMapping
    public ResponseEntity<ApiResponse<List<FavoritoDTO.Response>>> listar(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<FavoritoDTO.Response> lista = favoritoService.listar(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(lista));
    }

    /** POST /api/favoritos */
    @PostMapping
    public ResponseEntity<ApiResponse<FavoritoDTO.Response>> salvar(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody FavoritoDTO.Request req) {
        FavoritoDTO.Response salvo = favoritoService.salvar(userDetails.getUsername(), req);
        return ResponseEntity.status(201).body(ApiResponse.ok("Favorito salvo!", salvo));
    }

    /** DELETE /api/favoritos/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> remover(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        favoritoService.remover(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.ok("Favorito removido!", null));
    }
}
