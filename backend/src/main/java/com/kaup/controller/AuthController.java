package com.kaup.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kaup.dto.CadastroRequest;
import com.kaup.dto.LoginRequest;
import com.kaup.dto.UsuarioResponse;
import com.kaup.entity.Usuario;
import com.kaup.security.LimiteDeTentativas;
import com.kaup.security.UsuarioLogado;
import com.kaup.service.ContaService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Cadastro, entrada, saída e consulta de quem está logado. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final ContaService contaService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextos;
    private final LimiteDeTentativas limite;

    public AuthController(ContaService contaService, AuthenticationManager authenticationManager,
                          SecurityContextRepository contextos, LimiteDeTentativas limite) {
        this.contaService = contaService;
        this.authenticationManager = authenticationManager;
        this.contextos = contextos;
        this.limite = limite;
    }

    /** Cria a conta e já deixa a pessoa logada. */
    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse cadastrar(@RequestBody CadastroRequest pedido,
                                     HttpServletRequest request, HttpServletResponse response) {
        Usuario usuario = contaService.cadastrar(pedido);
        abrirSessao(usuario.getEmail(), pedido.password(), request, response);
        return UsuarioResponse.de(usuario);
    }

    @PostMapping("/entrar")
    public UsuarioResponse entrar(@RequestBody LoginRequest pedido,
                                  HttpServletRequest request, HttpServletResponse response) {
        if (pedido.email() == null || pedido.password() == null) {
            throw new BadCredentialsException("Credenciais ausentes.");
        }
        // Bloqueado, nem a senha certa entra: senão dava para testar senhas durante o bloqueio.
        String chave = "entrar:" + ContaService.normalizarEmail(pedido.email());
        limite.verificar(chave);
        Authentication autenticacao;
        try {
            autenticacao = abrirSessao(pedido.email(), pedido.password(), request, response);
        } catch (AuthenticationException e) {
            limite.falhou(chave);
            throw e;
        }
        limite.acertou(chave);
        UsuarioLogado logado = (UsuarioLogado) autenticacao.getPrincipal();
        return UsuarioResponse.de(contaService.buscar(logado.id())
                .orElseThrow(() -> new BadCredentialsException("Conta não encontrada.")));
    }

    @PostMapping("/sair")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sair(Authentication autenticacao, HttpServletRequest request, HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(request, response, autenticacao);
    }

    /** Quem está logado. Sem sessão válida, o Spring Security já responde 401 antes daqui. */
    @GetMapping("/eu")
    public UsuarioResponse eu(@AuthenticationPrincipal UsuarioLogado logado) {
        return UsuarioResponse.de(contaService.buscar(logado.id())
                .orElseThrow(() -> new InsufficientAuthenticationException("A conta não existe mais.")));
    }

    private Authentication abrirSessao(String email, String senha,
                                       HttpServletRequest request, HttpServletResponse response) {
        Authentication autenticacao = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, senha));
        if (request.getSession(false) != null) {
            request.changeSessionId(); // uma sessão anônima não pode virar a sessão da conta
        }
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacao);
        SecurityContextHolder.setContext(contexto);
        contextos.saveContext(contexto, request, response);
        return autenticacao;
    }
}
