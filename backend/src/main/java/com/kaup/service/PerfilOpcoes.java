package com.kaup.service;

import java.util.Set;

/** Valores aceitos nas preferências; são os mesmos que o frontend valida (types/domain.ts). */
public final class PerfilOpcoes {

    public static final Set<String> TAMANHOS_DE_LETRA = Set.of("small", "medium", "large");
    public static final Set<String> NIVEIS_DE_EXPLICACAO = Set.of("simple", "normal", "complete");
    public static final Set<String> FAIXAS_DE_RENDA =
            Set.of("less3", "3to5", "5to7", "7to15", "more15", "prefer_not");

    private PerfilOpcoes() { }
}
