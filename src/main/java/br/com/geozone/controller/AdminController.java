package br.com.geozone.controller;

import br.com.geozone.dto.AdminStatsDTO;
import br.com.geozone.dto.ApiResponse;
import br.com.geozone.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    /** GET /api/admin/stats */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminStatsDTO>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getStats()));
    }

    /** GET /api/admin/usuarios */
    @GetMapping("/usuarios")
    public ResponseEntity<ApiResponse<List<AdminStatsDTO.UsuarioResumo>>> listarUsuarios() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listarUsuarios()));
    }

    /** PUT /api/admin/usuarios/{id}/plano?plano=PRO */
    @PutMapping("/usuarios/{id}/plano")
    public ResponseEntity<ApiResponse<Void>> alterarPlano(
            @PathVariable Long id,
            @RequestParam String plano) {
        adminService.alterarPlano(id, plano);
        return ResponseEntity.ok(ApiResponse.ok("Plano alterado com sucesso!", null));
    }

    /** PUT /api/admin/usuarios/{id}/toggle */
    @PutMapping("/usuarios/{id}/toggle")
    public ResponseEntity<ApiResponse<Void>> toggleUsuario(@PathVariable Long id) {
        adminService.toggleUsuario(id);
        return ResponseEntity.ok(ApiResponse.ok("Status do usuário alterado!", null));
    }

    /** DELETE /api/admin/usuarios/{id} */
    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<ApiResponse<Void>> removerUsuario(@PathVariable Long id) {
        adminService.removerUsuario(id);
        return ResponseEntity.ok(ApiResponse.ok("Usuário removido!", null));
    }
}
