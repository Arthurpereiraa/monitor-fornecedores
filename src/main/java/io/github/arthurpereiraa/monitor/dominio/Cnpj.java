package io.github.arthurpereiraa.monitor.dominio;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * CNPJ validado. Aceita o formato numérico e o alfanumérico (IN RFB nº 2.229/2024, em vigor desde julho de 2026):
 * as 12 primeiras posições podem ter letras e cada caractere vale (código ASCII − 48) no cálculo dos dígitos.
 */
public record Cnpj(String valor) {

    private static final Pattern FORMATO = Pattern.compile("^[0-9A-Z]{12}\\d{2}$");
    private static final int[] PESOS_1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    public Cnpj {
        Objects.requireNonNull(valor, "CNPJ não pode ser nulo");
        if (!valido(valor)) {
            throw new CnpjInvalidoException(valor);
        }
        valor = normalizar(valor);
    }

    /** Cria a partir de qualquer texto (com ou sem pontuação). Lança {@link CnpjInvalidoException} se for inválido. */
    public static Cnpj de(String texto) {
        return new Cnpj(texto == null ? "" : texto);
    }

    public static String normalizar(String texto) {
        return texto.toUpperCase(Locale.ROOT).replaceAll("[^0-9A-Z]", "");
    }

    public static boolean valido(String texto) {
        if (texto == null) {
            return false;
        }
        String cnpj = normalizar(texto);
        if (!FORMATO.matcher(cnpj).matches() || cnpj.chars().distinct().count() == 1) {
            return false;
        }
        int d1 = digito(cnpj, PESOS_1);
        int d2 = digito(cnpj.substring(0, 12) + d1, PESOS_2);
        return cnpj.endsWith("" + d1 + d2);
    }

    private static int digito(String base, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (base.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    public boolean alfanumerico() {
        return valor.chars().anyMatch(Character::isLetter);
    }

    public String formatado() {
        return valor.substring(0, 2) + "." + valor.substring(2, 5) + "." + valor.substring(5, 8)
                + "/" + valor.substring(8, 12) + "-" + valor.substring(12);
    }

    @Override
    public String toString() {
        return formatado();
    }
}
