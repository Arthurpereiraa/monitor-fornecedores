package io.github.arthurpereiraa.monitor.integracao;

import io.github.arthurpereiraa.monitor.dominio.DadosEmpresa;

public record ResultadoConsulta(DadosEmpresa dados, FonteDados fonte) {

    public enum FonteDados {
        BRASILAPI("BrasilAPI"),
        MINHA_RECEITA("Minha Receita");

        private final String nome;

        FonteDados(String nome) {
            this.nome = nome;
        }

        public String nome() {
            return nome;
        }
    }
}
