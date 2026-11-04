# Kaup — instruções para o Claude

## Natureza do projeto

O Kaup é um **projeto universitário**, feito para ser testado por colegas e professores. Não é um produto para o público. Isso define o que vale a pena construir:

- **Sem verificação de e-mail no cadastro.** Basta e-mail e senha. Verificar e-mail seria uma barreira para quem só quer testar.
- **Sem e-mail de verdade na recuperação de senha.** O código simulado (`KAUP_CODIGO_FAKE`, hoje `676869`) é uma decisão **definitiva**, não uma pendência. Não propor SMTP, Brevo, Resend, SES nem código aleatório com validade.
- **Segurança proporcional.** O objetivo é só: (1) não sofrer DDoS nem teste de estresse, (2) ninguém sem autorização entrar no banco, (3) não estourar a cota da chave do Gemini. Detalhes em `docs/planejamento/SEGURANCA.md`. Evitar soluções pesadas (WAF, SIEM, OAuth, MFA, auditoria completa).
- **Prioridade é a demonstração funcionar.** Preferir a solução simples a que cobre 100% dos casos.

Ao ler `docs/planejamento/PLANO_CONTINUACAO.md`, trate como **descartados** os itens que dependem de e-mail real: verificação de e-mail, recuperação por e-mail, escolha de provedor de e-mail (D7), `S4` na parte do código aleatório. A parte de `S4` que continua valendo é só esconder a dica do código fora do ambiente de desenvolvimento, se o time quiser.

## Estrutura

- `backend/` — Spring Boot 3 (Java 17), PostgreSQL, Tess4J (OCR), Gemini.
- `frontend/` — React 19 + Vite + TypeScript. `VITE_DATA_SOURCE=api|mock`.
- `docs/` — documentação de concepção; `docs/planejamento/` — planos de trabalho.
- `scripts/` e `RODAR.md` — como subir tudo.

## Como testar

```bash
cd frontend && npm test
docker run --rm -v "$PWD/backend":/app -w /app maven:3.9-eclipse-temurin-17 mvn -B test   # sem Maven instalado
```

## Convenções

- Idioma: português do Brasil em código de domínio, mensagens e documentos.
- Commits em português, no estilo `tipo(escopo): descrição` (`feat`, `fix`, `chore`, `docs`).
- Planos e documentos de planejamento ficam em `docs/planejamento/`.
- Nunca versionar `.env` nem chaves.
