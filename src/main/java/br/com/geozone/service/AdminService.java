package br.com.geozone.service;

import br.com.geozone.dto.AdminStatsDTO;
import br.com.geozone.model.Usuario;
import br.com.geozone.repository.ConsultaHistoricoRepository;
import br.com.geozone.repository.FavoritoRepository;
import br.com.geozone.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UsuarioRepository usuarioRepository;
    private final FavoritoRepository favoritoRepository;
    private final ConsultaHistoricoRepository historicoRepository;

    private static final double PRECO_PRO_MES = 39.0;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ══════════════════════════════════════════
    // STATS DO PAINEL
    // ══════════════════════════════════════════
    public AdminStatsDTO getStats() {
        long totalUsuarios = usuarioRepository.count();
        long usuariosAtivos = usuarioRepository.countAtivos();
        long usuariosPro = usuarioRepository.countPro();
        long totalFavoritos = favoritoRepository.count();
        long totalConsultas = historicoRepository.count();
        double mrr = usuariosPro * PRECO_PRO_MES;

        List<Usuario> ultimos = usuarioRepository.findAll(
                PageRequest.of(0, 5, org.springframework.data.domain.Sort.by("criadoEm").descending())
        ).getContent();

        List<AdminStatsDTO.UsuarioResumo> resumos = ultimos.stream()
                .map(u -> AdminStatsDTO.UsuarioResumo.builder()
                        .id(u.getId())
                        .nome(u.getNome())
                        .email(u.getEmail())
                        .plano(u.getPlano().name())
                        .criadoEm(u.getCriadoEm() != null ? u.getCriadoEm().format(FMT) : "—")
                        .build())
                .collect(Collectors.toList());

        return AdminStatsDTO.builder()
                .totalUsuarios(totalUsuarios)
                .usuariosAtivos(usuariosAtivos)
                .usuariosPro(usuariosPro)
                .totalFavoritos(totalFavoritos)
                .totalConsultas(totalConsultas)
                .mrrFormatado(String.format("R$ %.2f", mrr))
                .ultimosUsuarios(resumos)
                .build();
    }

    // ══════════════════════════════════════════
    // LISTAR TODOS OS USUÁRIOS
    // ══════════════════════════════════════════
    public List<AdminStatsDTO.UsuarioResumo> listarUsuarios() {
        return usuarioRepository.findAll().stream()
                .map(u -> AdminStatsDTO.UsuarioResumo.builder()
                        .id(u.getId())
                        .nome(u.getNome())
                        .email(u.getEmail())
                        .plano(u.getPlano().name())
                        .criadoEm(u.getCriadoEm() != null ? u.getCriadoEm().format(FMT) : "—")
                        .build())
                .collect(Collectors.toList());
    }

    // ══════════════════════════════════════════
    // ALTERAR PLANO DO USUÁRIO
    // ══════════════════════════════════════════
    @Transactional
    public void alterarPlano(Long usuarioId, String plano) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + usuarioId));
        usuario.setPlano(Usuario.Plano.valueOf(plano.toUpperCase()));
        usuarioRepository.save(usuario);
        log.info("Admin alterou plano do usuário {} para {}", usuarioId, plano);
    }

    // ══════════════════════════════════════════
    // ATIVAR / DESATIVAR USUÁRIO
    // ══════════════════════════════════════════
    @Transactional
    public void toggleUsuario(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + usuarioId));
        usuario.setAtivo(!usuario.getAtivo());
        usuarioRepository.save(usuario);
        log.info("Admin {} usuário {}", usuario.getAtivo() ? "ativou" : "desativou", usuarioId);
    }

    // ══════════════════════════════════════════
    // REMOVER USUÁRIO
    // ══════════════════════════════════════════
    @Transactional
    public void removerUsuario(Long usuarioId) {
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new RuntimeException("Usuário não encontrado: " + usuarioId);
        }
        usuarioRepository.deleteById(usuarioId);
        log.info("Admin removeu usuário {}", usuarioId);
    }
}
