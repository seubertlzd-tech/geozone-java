package br.com.geozone.repository;

import br.com.geozone.model.ConsultaHistorico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConsultaHistoricoRepository extends JpaRepository<ConsultaHistorico, Long> {

    List<ConsultaHistorico> findByUsuarioIdOrderByConsultadoEmDesc(Long usuarioId, Pageable pageable);

    long countByUsuarioId(Long usuarioId);
}
