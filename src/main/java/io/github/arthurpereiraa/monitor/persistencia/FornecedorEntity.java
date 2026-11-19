package io.github.arthurpereiraa.monitor.persistencia;

import io.github.arthurpereiraa.monitor.dominio.NivelRisco;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Fornecedor monitorado, com os dados da última avaliação. */
@Entity
@Table(name = "fornecedor")
public class FornecedorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 14)
    private String cnpj;

    @Column(name = "razao_social")
    private String razaoSocial;

    @Column(name = "nome_fantasia")
    private String nomeFantasia;

    private String situacao;
    private String municipio;
    private String uf;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_risco", nullable = false, length = 10)
    private NivelRisco nivelRisco;

    private String fonte;

    @Column(name = "cadastrado_em", nullable = false)
    private Instant cadastradoEm;

    @Column(name = "verificado_em", nullable = false)
    private Instant verificadoEm;

    protected FornecedorEntity() {
        // exigido pelo JPA
    }

    public FornecedorEntity(String cnpj, Instant cadastradoEm) {
        this.cnpj = cnpj;
        this.cadastradoEm = cadastradoEm;
        this.verificadoEm = cadastradoEm;
    }

    public Long getId() {
        return id;
    }

    public String getCnpj() {
        return cnpj;
    }

    public String getRazaoSocial() {
        return razaoSocial;
    }

    public void setRazaoSocial(String razaoSocial) {
        this.razaoSocial = razaoSocial;
    }

    public String getNomeFantasia() {
        return nomeFantasia;
    }

    public void setNomeFantasia(String nomeFantasia) {
        this.nomeFantasia = nomeFantasia;
    }

    public String getSituacao() {
        return situacao;
    }

    public void setSituacao(String situacao) {
        this.situacao = situacao;
    }

    public String getMunicipio() {
        return municipio;
    }

    public void setMunicipio(String municipio) {
        this.municipio = municipio;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    public NivelRisco getNivelRisco() {
        return nivelRisco;
    }

    public void setNivelRisco(NivelRisco nivelRisco) {
        this.nivelRisco = nivelRisco;
    }

    public String getFonte() {
        return fonte;
    }

    public void setFonte(String fonte) {
        this.fonte = fonte;
    }

    public Instant getCadastradoEm() {
        return cadastradoEm;
    }

    public Instant getVerificadoEm() {
        return verificadoEm;
    }

    public void setVerificadoEm(Instant verificadoEm) {
        this.verificadoEm = verificadoEm;
    }
}
