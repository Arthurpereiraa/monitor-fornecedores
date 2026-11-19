package io.github.arthurpereiraa.monitor.persistencia;

import io.github.arthurpereiraa.monitor.dominio.Alerta;
import io.github.arthurpereiraa.monitor.dominio.NivelRisco;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Uma avaliação de risco de um fornecedor. O conjunto delas forma o histórico. */
@Entity
@Table(name = "avaliacao")
public class AvaliacaoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fornecedor_id", nullable = false)
    private FornecedorEntity fornecedor;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_risco", nullable = false, length = 10)
    private NivelRisco nivelRisco;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_anterior", length = 10)
    private NivelRisco nivelAnterior;

    private String fonte;

    @Column(name = "avaliado_em", nullable = false)
    private Instant avaliadoEm;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "avaliacao_alerta", joinColumns = @JoinColumn(name = "avaliacao_id"))
    @OrderColumn(name = "ordem")
    private List<AlertaEmbeddable> alertas = new ArrayList<>();

    protected AvaliacaoEntity() {
        // exigido pelo JPA
    }

    public AvaliacaoEntity(FornecedorEntity fornecedor, NivelRisco nivelRisco, NivelRisco nivelAnterior, String fonte,
            Instant avaliadoEm, List<Alerta> alertas) {
        this.fornecedor = fornecedor;
        this.nivelRisco = nivelRisco;
        this.nivelAnterior = nivelAnterior;
        this.fonte = fonte;
        this.avaliadoEm = avaliadoEm;
        alertas.forEach(a -> this.alertas.add(AlertaEmbeddable.de(a)));
    }

    public Long getId() {
        return id;
    }

    public FornecedorEntity getFornecedor() {
        return fornecedor;
    }

    public NivelRisco getNivelRisco() {
        return nivelRisco;
    }

    public NivelRisco getNivelAnterior() {
        return nivelAnterior;
    }

    public String getFonte() {
        return fonte;
    }

    public Instant getAvaliadoEm() {
        return avaliadoEm;
    }

    public List<Alerta> getAlertas() {
        return alertas.stream().map(AlertaEmbeddable::paraDominio).toList();
    }
}
