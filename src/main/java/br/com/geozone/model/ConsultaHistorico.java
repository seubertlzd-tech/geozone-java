package br.com.geozone.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "consultas_historico")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsultaHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    private Double latitude;
    private Double longitude;
    private String endereco;
    private String landuse;
    private Long osmId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime consultadoEm;
}
