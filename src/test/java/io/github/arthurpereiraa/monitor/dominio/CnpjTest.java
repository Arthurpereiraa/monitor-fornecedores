package io.github.arthurpereiraa.monitor.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CnpjTest {

    @Test
    @DisplayName("aceita CNPJ numérico com ou sem pontuação e normaliza")
    void numerico() {
        Cnpj cnpj = Cnpj.de("11.222.333/0001-81");
        assertEquals("11222333000181", cnpj.valor());
        assertEquals("11.222.333/0001-81", cnpj.formatado());
        assertFalse(cnpj.alfanumerico());
        assertEquals(cnpj, Cnpj.de("11222333000181"));
    }

    @Test
    @DisplayName("aceita o CNPJ alfanumérico de 2026 (exemplo oficial da Receita)")
    void alfanumerico() {
        Cnpj cnpj = Cnpj.de("12.abc.345/01de-35");
        assertEquals("12ABC34501DE35", cnpj.valor());
        assertEquals("12.ABC.345/01DE-35", cnpj.formatado());
        assertTrue(cnpj.alfanumerico());
    }

    @Test
    @DisplayName("rejeita dígito verificador errado, tamanho errado, repetidos e letras no DV")
    void invalidos() {
        assertFalse(Cnpj.valido("11.222.333/0001-82"));
        assertFalse(Cnpj.valido("1122233300018"));
        assertFalse(Cnpj.valido("00.000.000/0000-00"));
        assertFalse(Cnpj.valido("12.ABC.345/01DE-3A"));
        assertFalse(Cnpj.valido(null));
        CnpjInvalidoException erro = assertThrows(CnpjInvalidoException.class, () -> Cnpj.de("123"));
        assertTrue(erro.getMessage().contains("123"));
    }
}
