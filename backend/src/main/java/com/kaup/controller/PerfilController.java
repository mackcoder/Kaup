package com.kaup.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kaup.dto.PerfilRequest;
import com.kaup.dto.PerfilResponse;
import com.kaup.security.UsuarioLogado;
import com.kaup.service.PerfilService;

/** Perfil de quem está logado (ver frontend/src/services/api/endpoints.ts). */
@RestController
@RequestMapping("/api/perfil")
public class PerfilController {

    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    @GetMapping
    public PerfilResponse obter(@AuthenticationPrincipal UsuarioLogado usuario) {
        return perfilService.obter(usuario.id());
    }

    @PatchMapping
    public PerfilResponse atualizar(@RequestBody PerfilRequest pedido, @AuthenticationPrincipal UsuarioLogado usuario) {
        return perfilService.atualizar(usuario.id(), pedido);
    }
}
