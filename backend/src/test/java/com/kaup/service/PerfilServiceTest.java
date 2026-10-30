package com.kaup.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.kaup.dto.PerfilRequest;
import com.kaup.dto.PerfilResponse;
import com.kaup.entity.Usuario;
import com.kaup.exception.DadosInvalidosException;
import com.kaup.exception.EmailJaCadastradoException;
import com.kaup.repository.UsuarioRepository;

class PerfilServiceTest {

    private final UUID id = UUID.randomUUID();
    private UsuarioRepository repository;
    private PerfilService service;
    private Usuario ana;

    @BeforeEach
    void preparar() {
        repository = mock(UsuarioRepository.class);
        service = new PerfilService(repository);
        ana = new Usuario();
        ReflectionTestUtils.setField(ana, "id", id);
        ana.setNome("Ana Souza");
        ana.setEmail("ana@exemplo.com");
        when(repository.findById(id)).thenReturn(Optional.of(ana));
        when(repository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
    }

    private PerfilRequest soCom(String nome, String email, String fonte, String nivel, String renda,
                                Boolean audio, Boolean compartilhar) {
        return new PerfilRequest(nome, email, fonte, nivel, renda, audio, compartilhar);
    }

    @Test
    void contaNovaTemOsPadroesDoFrontend() {
        PerfilResponse perfil = service.obter(id);

        assertEquals("Ana Souza", perfil.name());
        assertEquals("large", perfil.fontSize());
        assertEquals("simple", perfil.explainLevel());
        assertEquals("prefer_not", perfil.income());
        assertFalse(perfil.audio());
        assertTrue(perfil.shareIncomeWithAi()); // ligado por padrão; a pessoa pode bloquear
    }

    @Test
    void patchParcialAlteraSoOQueVeio() {
        PerfilResponse perfil = service.atualizar(id, soCom(null, null, "small", null, "3to5", null, null));

        assertEquals("small", perfil.fontSize());
        assertEquals("3to5", perfil.income());
        assertEquals("Ana Souza", perfil.name());
        assertEquals("simple", perfil.explainLevel()); // intocado
    }

    @Test
    void bloquearOEnvioDaRendaFicaGravado() {
        service.atualizar(id, soCom(null, null, null, null, null, null, false));

        assertFalse(ana.isCompartilharRenda());
        assertFalse(service.obter(id).shareIncomeWithAi());
    }

    @Test
    void perfilCompletoComoOFrontendEnvia() {
        PerfilResponse perfil = service.atualizar(id,
                soCom("  Ana Maria ", "ANA@exemplo.com", "medium", "complete", "7to15", true, false));

        assertEquals("Ana Maria", perfil.name());
        assertEquals("ana@exemplo.com", perfil.email());
        assertEquals("complete", perfil.explainLevel());
        assertTrue(perfil.audio());
        assertFalse(perfil.shareIncomeWithAi());
    }

    @Test
    void valoresForaDasListasSaoRecusados() {
        for (PerfilRequest ruim : new PerfilRequest[] {
                soCom(null, null, "gigante", null, null, null, null),
                soCom(null, null, null, "expert", null, null, null),
                soCom(null, null, null, null, "rico", null, null)}) {
            assertThrows(DadosInvalidosException.class, () -> service.atualizar(id, ruim));
        }
    }

    @Test
    void pedidoInvalidoNaoAlteraNada() {
        // O tamanho de letra é válido, mas o nível não: nem o tamanho pode ter sido gravado.
        assertThrows(DadosInvalidosException.class,
                () -> service.atualizar(id, soCom(null, null, "small", "expert", null, null, null)));

        assertEquals("large", ana.getFontSize());
        verify(repository, never()).save(any());
    }

    @Test
    void nomeEEmailInvalidosSaoRecusados() {
        assertThrows(DadosInvalidosException.class, () -> service.atualizar(id, soCom("   ", null, null, null, null, null, null)));
        assertThrows(DadosInvalidosException.class, () -> service.atualizar(id, soCom(null, "sem-arroba", null, null, null, null, null)));
    }

    @Test
    void emailDeOutraContaEhRecusadoMasOProprioEmailNao() {
        Usuario outra = new Usuario();
        ReflectionTestUtils.setField(outra, "id", UUID.randomUUID());
        when(repository.findByEmail("bia@exemplo.com")).thenReturn(Optional.of(outra));
        when(repository.findByEmail("ana@exemplo.com")).thenReturn(Optional.of(ana));

        assertThrows(EmailJaCadastradoException.class,
                () -> service.atualizar(id, soCom(null, "bia@exemplo.com", null, null, null, null, null)));
        assertEquals("ana@exemplo.com",
                service.atualizar(id, soCom(null, "ana@exemplo.com", null, null, null, null, null)).email());
    }
}
