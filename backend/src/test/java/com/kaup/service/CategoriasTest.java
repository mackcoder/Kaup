package com.kaup.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

class CategoriasTest {

    @Test
    void asChavesSaoAsQueOFrontendConhece() {
        // Se mudar aqui, mudar também frontend/src/styles/categories.ts.
        assertEquals(List.of("Carros", "Casa", "Trabalho", "Banco", "Outros"),
                List.copyOf(Categorias.DESCRICOES.keySet()));
    }

    @Test
    void normalizarIgnoraMaiusculasEEspacos() {
        assertEquals("Casa", Categorias.normalizar("casa"));
        assertEquals("Trabalho", Categorias.normalizar("  TRABALHO "));
    }

    @Test
    void normalizarDevolveNuloParaOQueNaoEhCategoria() {
        assertNull(Categorias.normalizar("Viagens"));
        assertNull(Categorias.normalizar(""));
        assertNull(Categorias.normalizar(null));
    }
}
