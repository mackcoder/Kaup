package com.kaup.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.kaup.entity.Papel;
import com.kaup.entity.Usuario;
import com.kaup.exception.DadosInvalidosException;
import com.kaup.exception.EmailJaCadastradoException;
import com.kaup.security.LimiteDeTentativas;
import com.kaup.security.SecurityConfig;
import com.kaup.security.UsuarioLogado;
import com.kaup.service.ContaService;

/** Usa a cadeia de segurança de verdade (SecurityConfig): sessão, CSRF e respostas 401/403. */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, LimiteDeTentativas.class})
class AuthControllerTest {

    private static final UUID ID = UUID.fromString("3f2a9c1e-5b7d-4c8a-9e0f-1a2b3c4d5e6f");
    private static final String SENHA = "senha1234";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContaService contaService;

    @MockBean
    private UserDetailsService usuarios;

    // O limitador é de verdade e vive no contexto de teste, compartilhado entre os testes.
    @Autowired
    private LimiteDeTentativas limite;

    private Usuario ana;

    @BeforeEach
    void preparar() {
        for (String email : new String[] {"ana@exemplo.com", "outra@exemplo.com", "nao-existe@exemplo.com"}) {
            limite.acertou("entrar:" + email);
        }
        ana = new Usuario();
        ReflectionTestUtils.setField(ana, "id", ID);
        ana.setNome("Ana Souza");
        ana.setEmail("ana@exemplo.com");
        ana.setPapel(Papel.USER);

        String hash = new BCryptPasswordEncoder().encode(SENHA);
        when(usuarios.loadUserByUsername(anyString())).thenAnswer(i -> {
            if (!"ana@exemplo.com".equalsIgnoreCase(i.getArgument(0))) {
                throw new UsernameNotFoundException("Conta não encontrada.");
            }
            return new UsuarioLogado(ID, "ana@exemplo.com", Papel.USER, hash);
        });
        when(contaService.buscar(ID)).thenReturn(Optional.of(ana));
    }

    // Envia o token como o frontend faz: o mesmo valor no cookie XSRF-TOKEN e no cabeçalho X-XSRF-TOKEN.
    private static RequestPostProcessor comCsrf() {
        return request -> {
            request.setCookies(new Cookie("XSRF-TOKEN", "token-de-teste"));
            request.addHeader("X-XSRF-TOKEN", "token-de-teste");
            return request;
        };
    }

    private String json(String email, String senha) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + senha + "\"}";
    }

    @Test
    void semLoginRetorna401EJaEntregaOCookieCsrf() throws Exception {
        mockMvc.perform(get("/api/auth/eu"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Entre na sua conta para continuar."))
                .andExpect(cookie().exists("XSRF-TOKEN")); // sem ele a página não conseguiria fazer o login
    }

    @Test
    void loginCorretoAbreSessaoQueValeNasProximasRequisicoes() throws Exception {
        MockHttpSession sessao = (MockHttpSession) mockMvc.perform(post("/api/auth/entrar").with(comCsrf())
                        .contentType(MediaType.APPLICATION_JSON).content(json("ANA@exemplo.com", SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana Souza"))
                .andExpect(jsonPath("$.email").value("ana@exemplo.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andReturn().getRequest().getSession();

        mockMvc.perform(get("/api/auth/eu").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID.toString()));
    }

    @Test
    void senhaErradaEEmailInexistenteDaoAMesmaMensagem() throws Exception {
        for (String corpo : new String[] {json("ana@exemplo.com", "errada123"), json("nao-existe@exemplo.com", SENHA)}) {
            mockMvc.perform(post("/api/auth/entrar").with(comCsrf()).contentType(MediaType.APPLICATION_JSON).content(corpo))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("E-mail ou senha incorretos."));
        }
    }

    @Test
    void loginSemCamposRetorna401() throws Exception {
        mockMvc.perform(post("/api/auth/entrar").with(comCsrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou senha incorretos."));
    }

    @Test
    void loginSemTokenCsrfEhRecusado() throws Exception {
        mockMvc.perform(post("/api/auth/entrar").contentType(MediaType.APPLICATION_JSON)
                        .content(json("ana@exemplo.com", SENHA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void cadastroCriaAContaEJaDeixaLogado() throws Exception {
        when(contaService.cadastrar(any())).thenReturn(ana);

        MockHttpSession sessao = (MockHttpSession) mockMvc.perform(post("/api/auth/cadastro").with(comCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ana Souza\",\"email\":\"ana@exemplo.com\",\"password\":\"" + SENHA
                                + "\",\"confirmPassword\":\"" + SENHA + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ana@exemplo.com"))
                .andReturn().getRequest().getSession();

        mockMvc.perform(get("/api/auth/eu").session(sessao)).andExpect(status().isOk());
    }

    @Test
    void cadastroInvalidoRetorna400EEmailRepetidoRetorna409() throws Exception {
        when(contaService.cadastrar(any()))
                .thenThrow(new DadosInvalidosException("As senhas não conferem."))
                .thenThrow(new EmailJaCadastradoException());
        String corpo = "{\"name\":\"Ana\",\"email\":\"ana@exemplo.com\",\"password\":\"a\",\"confirmPassword\":\"b\"}";

        mockMvc.perform(post("/api/auth/cadastro").with(comCsrf()).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("As senhas não conferem."));
        mockMvc.perform(post("/api/auth/cadastro").with(comCsrf()).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Já existe uma conta com esse e-mail."));
    }

    @Test
    void sairEncerraASessao() throws Exception {
        MockHttpSession sessao = (MockHttpSession) mockMvc.perform(post("/api/auth/entrar").with(comCsrf())
                        .contentType(MediaType.APPLICATION_JSON).content(json("ana@exemplo.com", SENHA)))
                .andReturn().getRequest().getSession();

        mockMvc.perform(post("/api/auth/sair").with(comCsrf()).session(sessao)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/eu").session(sessao)).andExpect(status().isUnauthorized());
    }

    @Test
    void cincoSenhasErradasBloqueiamOLoginPorUmInstanteInclusiveComASenhaCerta() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/entrar").with(comCsrf()).contentType(MediaType.APPLICATION_JSON)
                            .content(json("ana@exemplo.com", "errada123")))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/entrar").with(comCsrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(json("ANA@exemplo.com", SENHA)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.detail").value("Muitas tentativas. Aguarde alguns segundos e tente de novo."));

        // outro e-mail não é afetado
        mockMvc.perform(post("/api/auth/entrar").with(comCsrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(json("outra@exemplo.com", "qualquer123")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginCertoZeraAContagemDeErros() throws Exception {
        for (int rodada = 0; rodada < 3; rodada++) {
            for (int i = 0; i < 4; i++) {
                mockMvc.perform(post("/api/auth/entrar").with(comCsrf()).contentType(MediaType.APPLICATION_JSON)
                                .content(json("ana@exemplo.com", "errada123")))
                        .andExpect(status().isUnauthorized());
            }
            mockMvc.perform(post("/api/auth/entrar").with(comCsrf()).contentType(MediaType.APPLICATION_JSON)
                            .content(json("ana@exemplo.com", SENHA)))
                    .andExpect(status().isOk());
        }
    }
}
