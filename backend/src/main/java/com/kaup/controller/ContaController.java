package com.kaup.controller;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kaup.dto.ExportacaoResponse;
import com.kaup.security.UsuarioLogado;
import com.kaup.service.ContaService;
import com.kaup.service.ExportacaoService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** A própria conta de quem está logado. */
@RestController
@RequestMapping("/api/conta")
public class ContaController {

    private final ContaService contaService;
    private final ExportacaoService exportacao;

    public ContaController(ContaService contaService, ExportacaoService exportacao) {
        this.contaService = contaService;
        this.exportacao = exportacao;
    }

    /** Baixa tudo o que o sistema guarda da conta (JSON). */
    @GetMapping("/exportar")
    public ResponseEntity<ExportacaoResponse> exportar(@AuthenticationPrincipal UsuarioLogado usuario) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("kaup-meus-dados.json").build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(exportacao.exportar(usuario.id()));
    }

    /** Apaga a conta, os documentos e as conversas, e encerra a sessão. */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@AuthenticationPrincipal UsuarioLogado usuario, Authentication autenticacao,
                        HttpServletRequest request, HttpServletResponse response) {
        contaService.excluir(usuario);
        new SecurityContextLogoutHandler().logout(request, response, autenticacao);
    }
}
