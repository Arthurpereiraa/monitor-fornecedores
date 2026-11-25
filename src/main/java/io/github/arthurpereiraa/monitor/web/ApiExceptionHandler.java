package io.github.arthurpereiraa.monitor.web;

import io.github.arthurpereiraa.monitor.dominio.CnpjInvalidoException;
import io.github.arthurpereiraa.monitor.integracao.EmpresaNaoEncontradaException;
import io.github.arthurpereiraa.monitor.integracao.FonteIndisponivelException;
import io.github.arthurpereiraa.monitor.lote.ReavaliacaoEmAndamentoException;
import io.github.arthurpereiraa.monitor.servico.FornecedorJaCadastradoException;
import io.github.arthurpereiraa.monitor.servico.FornecedorNaoEncontradoException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Erros no formato Problem Details (RFC 9457), com mensagens em português. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(CnpjInvalidoException.class)
    ProblemDetail cnpjInvalido(CnpjInvalidoException e) {
        return problema(HttpStatus.BAD_REQUEST, "CNPJ inválido", e.getMessage());
    }

    @ExceptionHandler({FornecedorNaoEncontradoException.class, EmpresaNaoEncontradaException.class})
    ProblemDetail naoEncontrado(RuntimeException e) {
        return problema(HttpStatus.NOT_FOUND, "Não encontrado", e.getMessage());
    }

    @ExceptionHandler(FornecedorJaCadastradoException.class)
    ProblemDetail jaCadastrado(FornecedorJaCadastradoException e) {
        return problema(HttpStatus.CONFLICT, "Fornecedor já cadastrado", e.getMessage());
    }

    @ExceptionHandler(ReavaliacaoEmAndamentoException.class)
    ProblemDetail reavaliacaoEmAndamento(ReavaliacaoEmAndamentoException e) {
        return problema(HttpStatus.CONFLICT, "Reavaliação em andamento", e.getMessage());
    }

    @ExceptionHandler(FonteIndisponivelException.class)
    ProblemDetail fonteIndisponivel(FonteIndisponivelException e) {
        return problema(HttpStatus.SERVICE_UNAVAILABLE, "Fontes de dados indisponíveis", e.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail naoAutenticado(AuthenticationException e) {
        return problema(HttpStatus.UNAUTHORIZED, "Não autenticado", "Usuário ou senha inválidos.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail requisicaoInvalida(MethodArgumentNotValidException e) {
        String detalhe = e.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return problema(HttpStatus.BAD_REQUEST, "Requisição inválida", detalhe);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail parametroInvalido(MethodArgumentTypeMismatchException e) {
        return problema(HttpStatus.BAD_REQUEST, "Parâmetro inválido",
                "Valor \"" + e.getValue() + "\" inválido para o parâmetro \"" + e.getName() + "\".");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail corpoInvalido(HttpMessageNotReadableException e) {
        return problema(HttpStatus.BAD_REQUEST, "Requisição inválida", "Corpo da requisição ausente ou com JSON inválido.");
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        return problema;
    }
}
