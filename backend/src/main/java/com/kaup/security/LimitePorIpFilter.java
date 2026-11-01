package com.kaup.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Primeiro filtro da cadeia: recusa com 429 (e {@code Retry-After}) o IP que passou do
 * {@link LimitePorIp}, antes de qualquer outro trabalho.
 *
 * <p>Atrás de proxy, o IP de verdade vem em {@code X-Forwarded-For}. Cada proxy confiável acrescenta
 * o endereço de quem falou com ele no <b>fim</b> da lista, então com N proxies o cliente é o N-ésimo
 * contando do fim; o que vem antes foi escrito pelo próprio cliente e não vale. Sem o cabeçalho, ou
 * com {@code saltosDeProxy=0}, usa o endereço da conexão.
 */
public class LimitePorIpFilter extends OncePerRequestFilter {

    private final LimitePorIp limite;
    private final ObjectMapper json;
    private final int saltosDeProxy;

    public LimitePorIpFilter(LimitePorIp limite, ObjectMapper json, int saltosDeProxy) {
        this.limite = limite;
        this.json = json;
        this.saltosDeProxy = saltosDeProxy;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        LimitePorIp.Bloqueio bloqueio = limite.verificar(
                enderecoDe(request), request.getMethod(), request.getRequestURI());
        if (bloqueio == null) {
            chain.doFilter(request, response);
            return;
        }
        String detalhe = bloqueio.cadastro()
                ? "Muitas contas criadas por este endereço. Tente de novo mais tarde."
                : "Muitas requisições em pouco tempo. Aguarde um instante e tente de novo.";
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(bloqueio.segundos()));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        json.writeValue(response.getOutputStream(),
                ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, detalhe));
    }

    String enderecoDe(HttpServletRequest request) {
        String encaminhado = request.getHeader("X-Forwarded-For");
        if (saltosDeProxy > 0 && encaminhado != null && !encaminhado.isBlank()) {
            String[] enderecos = encaminhado.split(",");
            int posicao = Math.max(0, enderecos.length - saltosDeProxy);
            String endereco = enderecos[posicao].trim();
            if (!endereco.isEmpty()) {
                return endereco;
            }
        }
        return request.getRemoteAddr();
    }
}
