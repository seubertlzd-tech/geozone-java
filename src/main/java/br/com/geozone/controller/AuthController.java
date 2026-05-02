package br.com.geozone.controller;

import br.com.geozone.dto.*;
import br.com.geozone.model.Usuario;
import br.com.geozone.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** POST /api/auth/cadastrar */
    @PostMapping("/cadastrar")
    public ResponseEntity<ApiResponse<AuthResponse>> cadastrar(@Valid @RequestBody RegisterRequest req) {
        AuthResponse resp = authService.cadastrar(req);
        return ResponseEntity.status(201).body(ApiResponse.ok("Cadastro realizado com sucesso!", resp));
    }

    /** POST /api/auth/login */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest req) {
        AuthResponse resp = authService.login(req);
        return ResponseEntity.ok(ApiResponse.ok("Login realizado com sucesso!", resp));
    }

    /** POST /api/auth/admin/login */
    @PostMapping("/admin/login")
    public ResponseEntity<ApiResponse<AuthResponse>> adminLogin(@Valid @RequestBody AdminLoginRequest req) {
        AuthResponse resp = authService.loginAdmin(req);
        return ResponseEntity.ok(ApiResponse.ok("Acesso administrativo concedido!", resp));
    }

    /** PUT /api/auth/upgrade */
    @PutMapping("/upgrade")
    public ResponseEntity<ApiResponse<AuthResponse>> upgrade(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String plano) {
        Usuario.Plano novoPlano = Usuario.Plano.valueOf(plano.toUpperCase());
        AuthResponse resp = authService.upgradePlano(userDetails.getUsername(), novoPlano);
        return ResponseEntity.ok(ApiResponse.ok("Plano atualizado para " + novoPlano, resp));
    }

    /** GET /api/auth/me */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<String>> me(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok("Autenticado como: " + userDetails.getUsername(), userDetails.getUsername()));
    }
}
