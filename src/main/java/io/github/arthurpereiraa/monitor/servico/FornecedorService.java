package io.github.arthurpereiraa.monitor.servico;

import io.github.arthurpereiraa.monitor.dominio.AnaliseRisco;
import io.github.arthurpereiraa.monitor.dominio.Cnpj;
import io.github.arthurpereiraa.monitor.dominio.DadosEmpresa;
import io.github.arthurpereiraa.monitor.dominio.EnderecoCep;
import io.github.arthurpereiraa.monitor.dominio.NivelRisco;
import io.github.arthurpereiraa.monitor.dominio.RegrasRisco;
import io.github.arthurpereiraa.monitor.integracao.ConsultaEmpresaGateway;
import io.github.arthurpereiraa.monitor.integracao.ResultadoConsulta;
import io.github.arthurpereiraa.monitor.persistencia.AvaliacaoEntity;
import io.github.arthurpereiraa.monitor.persistencia.AvaliacaoRepository;
import io.github.arthurpereiraa.monitor.persistencia.FornecedorEntity;
import io.github.arthurpereiraa.monitor.persistencia.FornecedorRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Casos de uso do monitoramento: cadastrar, consultar, reavaliar e remover fornecedores. */
@Service
public class FornecedorService {

    private static final Logger log = LoggerFactory.getLogger(FornecedorService.class);
    static final int TAMANHO_MAXIMO_PAGINA = 100;

    private final FornecedorRepository fornecedores;
    private final AvaliacaoRepository avaliacoes;
    private final ConsultaEmpresaGateway gateway;
    private final Clock clock;

    public FornecedorService(FornecedorRepository fornecedores, AvaliacaoRepository avaliacoes, ConsultaEmpresaGateway gateway,
            Clock clock) {
        this.fornecedores = fornecedores;
        this.avaliacoes = avaliacoes;
        this.gateway = gateway;
        this.clock = clock;
    }

    @Transactional
    public FornecedorView cadastrar(String cnpjTexto) {
        Cnpj cnpj = Cnpj.de(cnpjTexto);
        if (fornecedores.existsByCnpj(cnpj.valor())) {
            throw new FornecedorJaCadastradoException(cnpj);
        }
        Avaliado avaliado = avaliar(cnpj);
        FornecedorEntity fornecedor = new FornecedorEntity(cnpj.valor(), Instant.now(clock));
        atualizar(fornecedor, avaliado);
        fornecedor = fornecedores.save(fornecedor);
        AvaliacaoEntity avaliacao = avaliacoes.save(new AvaliacaoEntity(fornecedor, avaliado.analise().nivel(), null,
                avaliado.resultado().fonte().nome(), fornecedor.getVerificadoEm(), avaliado.analise().alertas()));
        log.info("Fornecedor {} cadastrado com risco {}", cnpj, avaliado.analise().nivel());
        return FornecedorView.de(fornecedor, AvaliacaoView.de(avaliacao));
    }

    @Transactional(readOnly = true)
    public PaginaView<FornecedorView> listar(NivelRisco nivel, int pagina, int tamanho) {
        PageRequest page = PageRequest.of(Math.max(pagina, 0), Math.clamp(tamanho, 1, TAMANHO_MAXIMO_PAGINA),
                Sort.by("razaoSocial").ascending());
        var resultado = nivel == null ? fornecedores.findAll(page) : fornecedores.findByNivelRisco(nivel, page);
        return PaginaView.de(resultado.map(f -> FornecedorView.de(f, null)));
    }

    @Transactional(readOnly = true)
    public FornecedorView buscar(String cnpjTexto) {
        FornecedorEntity fornecedor = carregar(Cnpj.de(cnpjTexto));
        AvaliacaoView ultima = avaliacoes.findFirstByFornecedorIdOrderByAvaliadoEmDescIdDesc(fornecedor.getId())
                .map(AvaliacaoView::de)
                .orElse(null);
        return FornecedorView.de(fornecedor, ultima);
    }

    @Transactional(readOnly = true)
    public List<AvaliacaoView> historico(String cnpjTexto) {
        FornecedorEntity fornecedor = carregar(Cnpj.de(cnpjTexto));
        return avaliacoes.findByFornecedorIdOrderByAvaliadoEmDescIdDesc(fornecedor.getId()).stream().map(AvaliacaoView::de).toList();
    }

    /** Consulta os dados de novo, recalcula o risco e registra a avaliação no histórico. */
    @Transactional
    public AvaliacaoView reavaliar(String cnpjTexto) {
        Cnpj cnpj = Cnpj.de(cnpjTexto);
        FornecedorEntity fornecedor = carregar(cnpj);
        NivelRisco anterior = fornecedor.getNivelRisco();
        Avaliado avaliado = avaliar(cnpj);
        atualizar(fornecedor, avaliado);
        AvaliacaoEntity avaliacao = avaliacoes.save(new AvaliacaoEntity(fornecedor, avaliado.analise().nivel(), anterior,
                avaliado.resultado().fonte().nome(), fornecedor.getVerificadoEm(), avaliado.analise().alertas()));
        AvaliacaoView view = AvaliacaoView.de(avaliacao);
        if (view.mudou()) {
            log.warn("Risco de {} mudou de {} para {}", cnpj, anterior, view.nivel());
        }
        return view;
    }

    @Transactional
    public void remover(String cnpjTexto) {
        FornecedorEntity fornecedor = carregar(Cnpj.de(cnpjTexto));
        avaliacoes.deleteByFornecedorId(fornecedor.getId());
        fornecedores.delete(fornecedor);
    }

    private FornecedorEntity carregar(Cnpj cnpj) {
        return fornecedores.findByCnpj(cnpj.valor()).orElseThrow(() -> new FornecedorNaoEncontradoException(cnpj));
    }

    private Avaliado avaliar(Cnpj cnpj) {
        ResultadoConsulta resultado = gateway.buscarEmpresa(cnpj);
        DadosEmpresa dados = resultado.dados();
        EnderecoCep cep = dados.cep() == null ? null : gateway.buscarCep(dados.cep()).orElse(null);
        AnaliseRisco analise = RegrasRisco.analisar(dados, cep, LocalDate.now(clock));
        return new Avaliado(resultado, analise);
    }

    private void atualizar(FornecedorEntity f, Avaliado avaliado) {
        DadosEmpresa d = avaliado.resultado().dados();
        f.setRazaoSocial(d.razaoSocial());
        f.setNomeFantasia(d.nomeFantasia());
        f.setSituacao(d.situacaoCadastral());
        f.setMunicipio(d.municipio());
        f.setUf(d.uf());
        f.setNivelRisco(avaliado.analise().nivel());
        f.setFonte(avaliado.resultado().fonte().nome());
        f.setVerificadoEm(Instant.now(clock));
    }

    private record Avaliado(ResultadoConsulta resultado, AnaliseRisco analise) {}
}
