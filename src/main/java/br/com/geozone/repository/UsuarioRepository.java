package br.com.geozone.repository;

import br.com.geozone.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Usuario> findByAtivoTrue();

    List<Usuario> findByPlano(Usuario.Plano plano);

    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.ativo = true")
    long countAtivos();

    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.plano = 'PRO'")
    long countPro();
}
