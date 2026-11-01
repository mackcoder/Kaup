package com.kaup.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

class LimitePorIpFilterTest {

    private final ObjectMapper json = new ObjectMapper();

    private MockHttpServletResponse chamar(LimitePorIpFilter filtro, String remoto, String encaminhado,
                                           String metodo, String caminho) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(metodo, caminho);
        request.setRemoteAddr(remoto);
        if (encaminhado != null) {
            request.addHeader("X-Forwarded-For", encaminhado);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filtro.doFilter(request, response, chain);
        if (chain.getRequest() == null) {
            response.setCommitted(true); // marca "barrado", para o teste distinguir de "passou"
        }
        return response;
    }

    @Test
    void passaAteOLimiteEDepoisResponde429EmPortuguesComRetryAfter() throws Exception {
        LimitePorIpFilter filtro = new LimitePorIpFilter(new LimitePorIp(3, 0, 0), json, 1);
        for (int i = 0; i < 3; i++) {
            assertEquals(200, chamar(filtro, "9.9.9.9", null, "GET", "/api/documentos").getStatus());
        }
        MockHttpServletResponse barrada = chamar(filtro, "9.9.9.9", null, "GET", "/api/documentos");
        assertEquals(429, barrada.getStatus());
        assertNotNull(barrada.getHeader("Retry-After"));
        assertTrue(barrada.getContentAsString().contains("Muitas requisições"));
    }

    @Test
    void atrasDeProxyUsaOEnderecoQueOProxyAcrescentou() throws Exception {
        LimitePorIpFilter filtro = new LimitePorIpFilter(new LimitePorIp(1, 0, 0), json, 1);
        // O proxy (10.0.0.1) é sempre o mesmo; o que muda é o cliente que ele anexou ao fim da lista.
        assertEquals(200, chamar(filtro, "10.0.0.1", "203.0.113.7", "GET", "/api/x").getStatus());
        assertEquals(200, chamar(filtro, "10.0.0.1", "203.0.113.8", "GET", "/api/x").getStatus());
        assertEquals(429, chamar(filtro, "10.0.0.1", "203.0.113.7", "GET", "/api/x").getStatus());
    }

    @Test
    void enderecoFalsoNoInicioDaListaNaoEscapaDoLimite() throws Exception {
        LimitePorIpFilter filtro = new LimitePorIpFilter(new LimitePorIp(1, 0, 0), json, 1);
        assertEquals(200, chamar(filtro, "10.0.0.1", "1.1.1.1, 203.0.113.7", "GET", "/api/x").getStatus());
        // Mesmo cliente de verdade, outro "endereço" inventado à esquerda: continua o mesmo contador.
        assertEquals(429, chamar(filtro, "10.0.0.1", "2.2.2.2, 203.0.113.7", "GET", "/api/x").getStatus());
    }

    @Test
    void comSaltosZeroIgnoraOCabecalho() throws Exception {
        LimitePorIpFilter filtro = new LimitePorIpFilter(new LimitePorIp(1, 0, 0), json, 0);
        assertEquals(200, chamar(filtro, "9.9.9.9", "1.1.1.1", "GET", "/api/x").getStatus());
        assertEquals(429, chamar(filtro, "9.9.9.9", "2.2.2.2", "GET", "/api/x").getStatus());
    }

    @Test
    void cadastroEstouradoTemMensagemPropria() throws Exception {
        LimitePorIpFilter filtro = new LimitePorIpFilter(new LimitePorIp(0, 0, 1), json, 1);
        chamar(filtro, "9.9.9.9", null, "POST", "/api/auth/cadastro");
        MockHttpServletResponse barrada = chamar(filtro, "9.9.9.9", null, "POST", "/api/auth/cadastro");
        assertEquals(429, barrada.getStatus());
        assertTrue(barrada.getContentAsString().contains("contas criadas"));
    }
}
