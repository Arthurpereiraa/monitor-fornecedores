package io.github.arthurpereiraa.monitor.dominio;

/** Cidade e UF a que um CEP pertence, usados para conferir o endereço cadastrado na Receita. */
public record EnderecoCep(String cidade, String uf) {}
