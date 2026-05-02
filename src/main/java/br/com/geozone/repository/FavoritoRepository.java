package br.com.geozone.repository;

import br.com.geozone.model.Favorito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoritoRepository extends JpaRepository<Favorito, Long> {

    List<Favorito> findByUsuarioIdOrderBycriadoEmDesc(Long usuarioId);

    Optional<Favorito> findByIdAndUsuarioId(Long id, Long usuarioId);

    boolean existsByUsuarioIdAndLatitudeAndLongitude(Long usuarioId, Double lat, Double lng);

    void deleteByIdAndUsuarioId(Long id, Long usuarioId);

    long countByUsuarioId(Long usuarioId);
}
