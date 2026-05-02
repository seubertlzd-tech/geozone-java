package br.com.geozone.service;

import br.com.geozone.dto.FavoritoDTO;
import br.com.geozone.model.Favorito;
import br.com.geozone.model.Usuario;
import br.com.geozone.repository.FavoritoRepository;
import br.com.geozone.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoritoService {

    private final FavoritoRepository favoritoRepository;
    private final UsuarioRepository usuarioRepository;

    private static final int LIMITE_GRATUITO = 10;

    // ══════════════════════════════════════════
    // LISTAR
    // ══════════════════════════════════════════
    public List<FavoritoDTO.Response> listar(String email) {
        Usuario usuario = buscarUsuario(email);
        return favoritoRepository.findByUsuarioIdOrderBycriadoEmDesc(usuario.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ══════════════════════════════════════════
    // SALVAR
    // ══════════════════════════════════════════
    @Transactional
    public FavoritoDTO.Response salvar(String email, FavoritoDTO.Request req) {
        Usuario usuario = buscarUsuario(email);

        // Verificar limite do plano gratuito
        if (usuario.getPlano() == Usuario.Plano.GRATUITO) {
            long qtd = favoritoRepository.countByUsuarioId(usuario.getId());
            if (qtd >= LIMITE_GRATUITO) {
                throw new RuntimeException(
                    "Limite de " + LIMITE_GRATUITO + " favoritos atingido no plano gratuito. Faça upgrade para o Pro.");
            }
        }

        Favorito fav = Favorito.builder()
                .usuario(usuario)
                .endereco(req.getEndereco())
                .cidade(req.getCidade())
                .estado(req.getEstado())
                .tipo(req.getTipo())
                .area(req.getArea())
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .osmId(req.getOsmId())
                .landuse(req.getLanduse())
                .nome(req.getNome())
                .build();

        Favorito salvo = favoritoRepository.save(fav);
        log.info("Favorito salvo — usuário: {} | endereço: {}", email, req.getEndereco());
        return toResponse(salvo);
    }

    // ══════════════════════════════════════════
    // REMOVER
    // ══════════════════════════════════════════
    @Transactional
    public void remover(String email, Long id) {
        Usuario usuario = buscarUsuario(email);
        Favorito fav = favoritoRepository.findByIdAndUsuarioId(id, usuario.getId())
                .orElseThrow(() -> new RuntimeException("Favorito não encontrado ou não pertence ao usuário"));
        favoritoRepository.delete(fav);
        log.info("Favorito removido — usuário: {} | id: {}", email, id);
    }

    // ══════════════════════════════════════════
    // UTILS
    // ══════════════════════════════════════════
    private Usuario buscarUsuario(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + email));
    }

    private FavoritoDTO.Response toResponse(Favorito f) {
        return FavoritoDTO.Response.builder()
                .id(f.getId())
                .endereco(f.getEndereco())
                .cidade(f.getCidade())
                .estado(f.getEstado())
                .tipo(f.getTipo())
                .area(f.getArea())
                .latitude(f.getLatitude())
                .longitude(f.getLongitude())
                .osmId(f.getOsmId())
                .landuse(f.getLanduse())
                .nome(f.getNome())
                .criadoEm(f.getCriadoEm())
                .build();
    }
}
