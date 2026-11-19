package io.github.arthurpereiraa.monitor.dominio;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Regras de risco determinísticas. São as mesmas dos projetos due-diligence-agentes (Python) e radar-cnpj (TypeScript):
 * o mesmo CNPJ gera o mesmo nível de risco nos três.
 */
public final class RegrasRisco {

    private static final BigDecimal CAPITAL_MINIMO_DEMAIS = new BigDecimal("10000");

    static final List<String> RECOMENDACOES_PADRAO = List.of(
            "Solicitar as certidões negativas de débitos (CND federal, FGTS e trabalhista).",
            "Consultar protestos e processos judiciais em nome da empresa.");

    private static final Map<String, String> RECOMENDACOES_POR_ALERTA = Map.of(
            "situacao_irregular", "Não formalizar a contratação até a empresa regularizar a situação cadastral na Receita Federal.",
            "empresa_muito_nova", "Exigir garantias contratuais ou pagamento vinculado à entrega, por se tratar de empresa recém-aberta.",
            "empresa_nova", "Pedir referências de clientes atuais, já que a empresa tem pouco histórico.",
            "situacao_mudou", "Entender o motivo da mudança recente na situação cadastral antes de contratar.",
            "mei", "Confirmar se o valor do contrato é compatível com o limite anual de faturamento do MEI.",
            "capital_baixo", "Avaliar se o capital social é compatível com o porte e o valor do contrato pretendido.",
            "endereco_inconsistente", "Confirmar o endereço de funcionamento (comprovante ou visita), pois o CEP não corresponde ao município.",
            "sem_contato", "Obter e validar um e-mail e um telefone de contato oficiais da empresa.",
            "sem_socios", "Solicitar o contrato social atualizado para identificar os sócios e administradores.");

    private RegrasRisco() {}

    public static AnaliseRisco analisar(DadosEmpresa empresa, EnderecoCep cep, LocalDate hoje) {
        List<Alerta> alertas = new ArrayList<>();

        String situacao = maiusculo(empresa.situacaoCadastral());
        if (!situacao.isEmpty() && !situacao.equals("ATIVA")) {
            alertas.add(new Alerta(Alerta.Nivel.ALTO, "situacao_irregular", "Situação cadastral " + situacao + " na Receita Federal."));
        }

        Integer idade = null;
        LocalDate inicio = empresa.dataInicioAtividade();
        if (inicio != null) {
            idade = (int) ChronoUnit.MONTHS.between(inicio, hoje);
            if (idade < 6) {
                alertas.add(new Alerta(Alerta.Nivel.ALTO, "empresa_muito_nova", "Empresa aberta há " + idadePorExtenso(idade) + "."));
            } else if (idade < 24) {
                alertas.add(new Alerta(Alerta.Nivel.MEDIO, "empresa_nova",
                        "Empresa aberta há " + idadePorExtenso(idade) + " (menos de 2 anos)."));
            }
        }

        LocalDate mudanca = empresa.dataSituacaoCadastral();
        if (mudanca != null && inicio != null && mudanca.isAfter(inicio)) {
            long dias = ChronoUnit.DAYS.between(mudanca, hoje);
            if (dias >= 0 && dias <= 90) {
                alertas.add(new Alerta(Alerta.Nivel.MEDIO, "situacao_mudou", "A situação cadastral mudou há " + dias + " dias."));
            }
        }

        if (Boolean.TRUE.equals(empresa.optanteMei())) {
            alertas.add(new Alerta(Alerta.Nivel.MEDIO, "mei",
                    "Optante pelo MEI: há limite anual de faturamento; contratos de valor alto podem ser incompatíveis."));
        }

        if (empresa.capitalSocial() != null && empresa.capitalSocial().compareTo(CAPITAL_MINIMO_DEMAIS) < 0
                && normalizar(empresa.porte()).equals("DEMAIS")) {
            alertas.add(new Alerta(Alerta.Nivel.MEDIO, "capital_baixo", "Capital social abaixo de R$ 10.000 para uma empresa de porte DEMAIS."));
        }

        if (cep != null) {
            String cidadeReceita = normalizar(empresa.municipio());
            String ufReceita = normalizar(empresa.uf());
            String cidadeCep = normalizar(cep.cidade());
            String ufCep = normalizar(cep.uf());
            boolean ufDiferente = !ufReceita.isEmpty() && !ufCep.isEmpty() && !ufReceita.equals(ufCep);
            if (!cidadeReceita.isEmpty() && !cidadeCep.isEmpty() && (!cidadeReceita.equals(cidadeCep) || ufDiferente)) {
                alertas.add(new Alerta(Alerta.Nivel.MEDIO, "endereco_inconsistente",
                        "O CEP cadastrado pertence a " + cep.cidade() + "/" + cep.uf() + ", mas o município informado na Receita é "
                                + empresa.municipio() + "/" + empresa.uf() + "."));
            }
        }

        if (vazio(empresa.email()) && vazio(empresa.telefone())) {
            alertas.add(new Alerta(Alerta.Nivel.INFO, "sem_contato", "Nenhum e-mail ou telefone cadastrado na Receita."));
        }

        boolean individual = normalizar(empresa.naturezaJuridica()).contains("INDIVIDUAL") || Boolean.TRUE.equals(empresa.optanteMei());
        if (empresa.socios().isEmpty() && !individual) {
            alertas.add(new Alerta(Alerta.Nivel.INFO, "sem_socios", "Nenhum sócio listado no quadro societário."));
        }

        alertas.sort(Comparator.comparing(Alerta::nivel));
        return new AnaliseRisco(nivelGeral(alertas), alertas, idade, recomendacoes(alertas));
    }

    public static NivelRisco nivelGeral(List<Alerta> alertas) {
        Set<Alerta.Nivel> niveis = new LinkedHashSet<>();
        alertas.forEach(a -> niveis.add(a.nivel()));
        if (niveis.contains(Alerta.Nivel.ALTO)) {
            return NivelRisco.ALTO;
        }
        return niveis.contains(Alerta.Nivel.MEDIO) ? NivelRisco.MEDIO : NivelRisco.BAIXO;
    }

    /** Recomendações concretas derivadas dos alertas, mais as padrão; no máximo 4. */
    public static List<String> recomendacoes(List<Alerta> alertas) {
        Set<String> recs = new LinkedHashSet<>();
        for (Alerta alerta : alertas) {
            String rec = RECOMENDACOES_POR_ALERTA.get(alerta.codigo());
            if (rec != null) {
                recs.add(rec);
            }
        }
        recs.addAll(RECOMENDACOES_PADRAO);
        return recs.stream().limit(4).toList();
    }

    public static String idadePorExtenso(int meses) {
        int anos = Math.max(meses, 0) / 12;
        int resto = Math.max(meses, 0) % 12;
        List<String> partes = new ArrayList<>();
        if (anos > 0) {
            partes.add(anos + (anos == 1 ? " ano" : " anos"));
        }
        if (resto > 0 || anos == 0) {
            partes.add(resto + (resto == 1 ? " mês" : " meses"));
        }
        return String.join(" e ", partes);
    }

    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toUpperCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    private static String maiusculo(String texto) {
        return texto == null ? "" : texto.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }
}
