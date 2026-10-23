# Decisões iniciais — 16/10/2026

Branch dede_backend, base 32172a3. Escolha arquitetural não significa funcionalidade implementada nem autorização de deploy público.

| ADR | Decisão e motivo | Implementação nesta fase |
|---|---|---|
| 001 Stack | Java 17, Maven 3.9.11, Boot 3.5.16 (linha 3.x solicitada e compatibilidade verificada); dependências pelo BOM | Web/Validation/JPA/Security, PostgreSQL e Flyway |
| 002 Limites | Monólito por funcionalidade; controllers finos, casos de uso concretos, sem interfaces triviais. Futuros auth/documents/analysis/chat/profile e compartilhados mínimos security/storage/ocr/ai/common | Bootstrap, auth, health, security, erros/correlação |
| 003 Banco | Postgres 17.9; Flyway único autor de DDL, Hibernate validate, open-in-view=false. V1 users sem seed, UUID pelo servidor, timestamps UTC e email normalizado único. Demais tabelas por fase | V1/V2 e UserAccount JPA; contas e sessões implementadas |
| 004 HTTP | Contrato TypeScript atual: /api, health texto, multipart file, arrays diretos, POST chat unitário assistant, enums existentes; sem alias legado obrigatório | Health e auth reais; documentos/análise/chat/perfil ainda ausentes |
| 005 Assíncrono | Jobs PostgreSQL com claim/lease e worker limitado; retomar tarefas abandonadas; sem @Async isolado, broker ou transação longa OCR/IA. Upload 202 e consulta de status | Planejado fases 3–4 |
| 006 OCR | PDFBox para PDF textual e Tesseract local português para imagens/scans sob TextExtractor. Preferir processo limitado/timeout; verificar versões na fase 4. ExtractMyText não adotado para evitar outro compartilhamento externo | Nenhum OCR nesta fase |
| 007 Gemini | Cliente HTTP server-side sob ContractAiClient, modelo via env sem default presumido, timeout/cota, JSON validado e evidências; sem tools executáveis ou fallback fictício. Falha após OCR mantém texto privado e failed; retry explícito/limitado a definir | Não implementado, chave desnecessária |
| 008 Auth | Spring Security + Spring Session JDBC na fase 2. Cookie HttpOnly/SameSite=Lax/Secure em prod, mesma origem/proxy, BCrypt com custo medido; CSRF também login/logout/upload e renovado após login. Sem JWT no localStorage | Cadastro/login/logout/me/CSRF e sessão JDBC reais; sem usuário padrão, Basic, form ou bypass |
| 009 Arquivos | FileStorage privado local em volume dev, chave opaca gerada pelo servidor; sem filename como path, bytes no banco ou URL pública. Bucket privado futuro | Planejado, sem adapter/volume vazio |
| 010 Privacidade | Fixtures sintéticas, logs sem payloads, renda prefer_not. Consentimento explícito para IA e retenção antes de dados reais. Não prometer anonimização/exclusão de backups ainda | Contas/sessões locais, erros controlados e nenhum envio externo |
| 011 Operação | Docker multi-stage Java 17, runtime não-root, Compose isolado no loopback, Postgres legado preservado. Dev/prod explícitos; prod exige JDBC/credenciais externos e cookie Secure | Artefatos desta fase; evidências no plano |
| 012 Health/erros | Liveness textual Backend do Kaup está rodando.; sem alegar OCR/IA/DB. Startup exige Flyway/DB, health não consulta dependências após startup. Sem Actuator público. Erros code/message/requestId e header X-Request-Id, incluindo Security e /error | Implementado nesta fase, validação no plano |
| 013 Layout | Mover especificações da pasta duplicada para backend-novo; AGENTS passa a abranger código e caminhos relativos corretos | Documentos reposicionados; diretórios protegidos preservados |

## Impactos e pendências

analysisFromDTO ignora facts; corrigir somente fase 8 com teste. ApiService não envia turnId nem lê corpo JSON de erro; negociar idempotência/CSRF/login. Contexto só mostra documento principal e attachContext segue sem contrato. Nenhuma edição frontend nesta fase.

Retenção, consentimento, eventual anonimização, backups e exclusão concorrente exigem acordo antes de documentos pessoais; visão/jornadas não são garantias cumpridas. Modelo Gemini e versões OCR serão verificados nas fases próprias, sem herdar gemini-3.8-flash do legado.

Versões: [requisitos oficiais Boot](https://docs.spring.io/spring-boot/3.5/system-requirements.html) e [BOM 3.5.16](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/3.5.16/spring-boot-dependencies-3.5.16.pom). Nenhum provider externo testado ou implementado nesta entrega.

## Complemento ADR-012 — revisão de erros e logs

Rejeições de StrictHttpFirewall respondem diretamente com ApiError e X-Request-Id, evitando que
sendError encaminhe o mesmo método bloqueado novamente ao firewall durante /error. O fallback
servlet também escreve explicitamente o header de correlação, inclusive quando o filtro inicial
não executou. Não houve relaxamento de métodos/caminhos permitidos ou autenticação/CSRF.

HTTP malformado rejeitado pelo parser Tomcat antes dos filtros mantém 400 genérico, sem garantir
JSON/requestId. Http11Processor usa nível ERROR para suprimir diagnósticos INFO/DEBUG que incluíam
URL/headers e stacktrace. A perda desses diagnósticos de entrada é intencional para evitar conteúdo
sensível nos logs; falhas ERROR do componente continuam habilitadas. Teste com marcador sintético
comprova a rejeição e a ausência do marcador em resposta e logs.

## ADR-014 — Organização dos documentos iniciais

Por solicitação do usuário, os 13 Markdown iniciais ficam em `backend-novo/docs/`,
incluindo o conteúdo completo de AGENTS.md e README.md. O AGENTS.md da raiz passa a ser apenas
um encaminhamento para preservar a descoberta automática das instruções em todo o backend.
Links entre documentos permanecem na mesma pasta; caminhos relativos às outras áreas do repositório
e os prompts foram atualizados. Essa organização substitui a localização definida no ADR-013.
Código, migrations, configuração e arquivos de ambiente continuam na raiz da implementação.

## ADR-015 — Autenticação real por sessão (fase 2, aceita)

Spring Security autentica JSON com DaoAuthenticationProvider/BCrypt e usuário cujo principal é o UUID da conta, sem email/senha no principal persistido. O controller delega o login a serviço que invoca explicitamente a estratégia de sessão (troca de ID + limpeza do CSRF) e salva o SecurityContext no repositório de sessão. Spring Session JDBC armazena sessão/contexto/CSRF no PostgreSQL; Flyway cria o esquema, autoinicialização JDBC desabilitada.

Cadastro recebe name/email/password, responde 201 com id/name/email e exige login separado. Email é normalizado com strip/lowercase Locale.ROOT; unicidade é garantida por INSERT ON CONFLICT, inclusive sob concorrência, evitando log de violação SQL com email. BCrypt padrão custo 12, configurável entre 10 e 14; senha nova 12–72 caracteres e no máximo 72 bytes UTF-8, sem trim/truncamento. Login inválido ou conta desabilitada usa o mesmo 401 genérico.

Token CSRF mascarado retornado por GET /api/auth/csrf, enviado em X-CSRF-TOKEN; somente cookie de sessão HttpOnly, SameSite=Lax, path /, sem Domain, Secure fora de dev. Cliente deve buscar token novamente após login/logout. Sem CORS permissivo, form login, Basic ou tokens no localStorage. Sessão inativa expira em 30 minutos; cookie não tem persistência de remember-me.

Limite local por IP direto (não confiar em X-Forwarded-For): registro/login compartilham janela de 20 tentativas/60s, mapa limitado a 4096 origens; excesso responde 429. Não é proteção distribuída nem substitui revisão de implantação/proxy. Identidade atual verifica conta habilitada; guard de owner responde 404 para outro proprietário. Recursos A/B existirão somente nos testes, sem antecipar documentos da fase 3.

Referências de implementação: [persistência e estratégia de sessão](https://docs.spring.io/spring-security/reference/6.5/servlet/authentication/session-management.html), [renovação de CSRF](https://docs.spring.io/spring-security/reference/6.5/servlet/exploits/csrf.html), [Spring Session JDBC](https://docs.spring.io/spring-session/reference/configuration/jdbc.html).

Validação do ADR-015: 42 testes no clean verify (incluindo PostgreSQL real); smoke Compose com upgrade
V1→V2, sessão/CSRF preservados no reinício, logout/replay, 429 por IP e flags de cookie no profile prod.
O envelope JSON de auth não expõe motivos internos de falha, senhas ou hashes. Reautenticação bem-sucedida
pode trocar a conta e sempre renova o ID; falha não troca a identidade de uma sessão já autenticada.
O frontend continua sem alterações; consumo de JSON/cookie/CSRF será integrado na fase 8.
