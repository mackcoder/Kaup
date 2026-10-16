# LABORATÓRIO DE ENGENHARIA DE SOFTWARE

# 📚 *Contratos Nível AI*

> **Disciplina:** Laboratório de Engenharia de Software  
> **Docente:** Gustavo Moreira Calixto
> **Metodologia:** Scrum / Agile

---

## 👥 Integrantes do Grupo

Nome | RA
-----|---
**André Doerner Duarte** | 10427938
**Matheus Leonardo Cardoso Kroeff** | 10426434
**Naoto Ushizaki** | 10437445

---

## 1. 🎯 Introdução
No Brasil, pessoas em situação de vulnerabilidade econômica podem enfrentar dificuldades para compreender contratos devido à presença de termos jurídicos e financeiros complexos. Essa dificuldade pode ser ainda maior entre pessoas com baixo nível de alfabetização funcional, podendo levar à assinatura de contratos sem a compreensão de seus custos, prazos, juros, multas e demais condições.

Nosso projeto, desenvolvido utilizando o **framework ágil Scrum**, busca facilitar a compreensão das condições e acordos presentes nesses contratos por meio de uma aplicação capaz de analisar seus conteúdos e apresentá-los de forma simplificada. O **desenvolvimento será organizado em Sprints**, com a participação dos **três papéis do *Scrum***: **Product Owner**, responsável pela priorização dos requisitos; **Scrum Master**, responsável pelo acompanhamento e aplicação da metodologia; e **Equipe de Desenvolvimento**, responsável pela implementação e entrega das funcionalidades.

---

## 2. 💡 O Projeto

### 2.1 Motivação
A dificuldade de compreender contratos pode representar uma barreira para que indivíduos tenham maior controle sobre suas próprias decisões financeiras. Diante desse problema, vimos uma oportunidade de desenvolver uma ferramenta que simplifique essas informações e, ao mesmo tempo, aplicar os conhecimentos de **Engenharia de Software adquiridos no semestre anterior**, utilizando o **Scrum** para organizar os requisitos, responsabilidades e entregas de forma incremental. Em situações de empréstimos, financiamentos, compras parceladas ou contratos de serviços, a falta de clareza sobre as condições estabelecidas pode fazer com que informações importantes passem despercebidas. Dessa forma, a aplicação busca contribuir para a autonomia financeira e reduzir decisões tomadas sem o devido entendimento, enquanto permite à equipe colocar em prática conceitos de desenvolvimento ágil ao longo da construção da solução.

### 2.2 Objetivo
O principal objetivo do projeto é aplicar na prática os conhecimentos de Engenharia de Software, utilizando o Scrum como framework de desenvolvimento ágil, permitindo que a equipe vivencie suas práticas, papéis e processos ao longo da construção do sistema. Como produto desse processo, o grupo busca desenvolver uma aplicação capaz de promover maior autonomia e segurança na tomada de decisões financeiras, tornando contratos mais acessíveis para pessoas com dificuldades de interpretação.

Para isso, a aplicação utilizará Inteligência Artificial para analisar contratos enviados por imagem ou PDF, traduzindo seus termos para uma linguagem simples e destacando informações importantes, como valores, prazos, juros, multas e possíveis riscos. A aplicação também poderá considerar informações fornecidas pelo usuário, como sua renda e situação financeira, para apresentar uma análise contextualizada do compromisso que está sendo assumido.

---

## 3. 🛠️ Metodologia
O desenvolvimento do projeto será realizado utilizando o **Scrum** como **framework *ágil***, **organizando o trabalho em *Sprints*** e priorizando as funcionalidades por meio do *Product Backlog*. A equipe acompanhará a evolução do projeto por meio dos eventos e responsabilidades definidos pelo Scrum, buscando realizar entregas incrementais e melhorar o processo ao longo do desenvolvimento.

- **Scrum:** Organização do projeto em Sprints, com Product Backlog, Sprint Backlog, acompanhamento e avaliação das entregas.
- **Product Owner:** Responsável por definir e priorizar os requisitos, organizar o Product Backlog e validar se as entregas atendem aos objetivos do projeto.
- **Scrum Master:** Responsável por acompanhar as Sprints, auxiliar na aplicação do Scrum, identificar impedimentos e contribuir para a organização e melhoria do processo.
- **Equipe de Desenvolvimento:** Responsável pela implementação, testes e integração das funcionalidades do sistema.

---

## 4. 🚀 Conclusão e Expectativas
Com o desenvolvimento do projeto, esperamos **colocar em prática os conhecimentos de *Engenharia de Software***, principalmente compreender o **funcionamento e o andamento do *Scrum*** durante o desenvolvimento de um produto. Através das *Sprints*, buscamos acompanhar na prática a definição de requisitos, priorização do backlog, distribuição das atividades, acompanhamento do desenvolvimento e avaliação dos resultados.

- **Aprendizado:** Compreender na prática como os papéis, eventos e artefatos do Scrum se relacionam durante o desenvolvimento.
- **Experiência:** Aprender a organizar e adaptar o trabalho da equipe conforme o andamento de cada Sprint.
- **Aplicação:** Utilizar os conceitos aprendidos para desenvolver uma solução funcional enquanto adquirimos experiência com o processo de desenvolvimento ágil.

---

## 5. 🗂️ Estrutura e versões do frontend

O repositório Kaup mantém o protótipo original e a aplicação refatorada em pastas independentes:

| Caminho | Responsabilidade |
|---|---|
| `frontend/src/` | Frontend original React + TypeScript + Vite, usado como referência visual e funcional |
| `frontend-novo/` | Frontend refatorado, com as cinco telas preservadas, rotas reais, componentes reutilizáveis, testes e adapters mock/API |
| `backend/` | Backend Java 17 + Spring Boot, com dependências JPA/PostgreSQL e Tess4J |
| `docs/` | Requisitos, concepção, documentação acadêmica e apresentações |
| `Kaup.pdf` | Apresentação do projeto |

A aplicação refatorada mantém Home, categorias, listagem de documentos, chat por documento e perfil. Corrige a seleção de arquivos e os contadores, inclui prévia de imagem e confirmação de envio, e separa a interface dos dados e do acesso HTTP. O frontend original continua disponível na sua própria pasta.

Documentação da refatoração: [execução e arquitetura](frontend-novo/README.md), [relatório de migração](frontend-novo/MIGRATION_NOTES.md), [especificação](frontend-novo/SPECS.md) e [contrato proposto da API](frontend-novo/API_CONTRACT_RASCUNHO.md).

## 6. ▶️ Como executar

Use **Node.js 22.12+** e npm. Cada bloco abaixo parte da raiz do repositório e executa uma versão independente.

### Frontend refatorado

```bash
cd frontend-novo
npm install
npm run dev
```

Abra **http://localhost:5174**. O modo padrão é `mock` e funciona integralmente sem backend. Os documentos e valores demonstrativos são fictícios. Uploads, mensagens e preferências permanecem somente na memória da sessão e são descartados ao recarregar a página. Arquivos enviados em mock não passam por OCR nem por análise real; áudio, ajuda, exclusão de conta e logout continuam demonstrativos.

### Frontend original

```bash
cd frontend/src
npm install
npm run dev
```

Abra **http://localhost:8443**. Consulte também o [guia do frontend original](frontend/como_rodar.md) para instruções de execução e publicação. As duas versões podem rodar ao mesmo tempo, nas respectivas portas.

## 7. 🧪 Qualidade e integração

Dentro de `frontend-novo/`:

```bash
npm run lint
npm run test
npm run build
npm run preview
```

O build verifica TypeScript antes de gerar `dist/`. A refatoração inclui 24 testes de domínio, serviços e fluxos React, além de scripts de navegador e registros de comparação visual em 390×844, 430×932 e 1280×800. Os comandos e limites dessas verificações estão no [README da aplicação refatorada](frontend-novo/README.md#verificação-e-comparações).

Para preparar a conexão HTTP com o Spring Boot, copie `frontend-novo/.env.example` para `frontend-novo/.env`, defina `VITE_DATA_SOURCE=api` e reinicie o servidor de desenvolvimento. Com `VITE_API_BASE_URL` vazia, o proxy Vite encaminha `/api` para `http://localhost:8080`; uma origem explícita pode ser configurada nessa variável. Em produção, configure o encaminhamento de `/api` ou a origem do backend.

**Estado atual do backend na `main`:** somente `GET /api/analise/health` está implementado. Upload, documentos, análise OCR/IA, histórico/chat, perfil e autenticação ainda dependem da implementação e aprovação dos contratos no Spring Boot. Falhas do modo `api` são exibidas e não retornam dados mock automaticamente. A integração HTTP interceptada nos testes não comprova a disponibilidade desses endpoints no backend real.

PostgreSQL, OCR e Gemini devem ser acessados exclusivamente pelo backend. Não coloque chaves ou segredos em variáveis `VITE_*`, pois elas são expostas no navegador. O [rascunho da API](frontend-novo/API_CONTRACT_RASCUNHO.md) reúne as pendências de autenticação, autorização, armazenamento, formatos/limites de arquivos, CORS e privacidade.

---

## ✉️ Contatos

Fique à vontade para entrar em contato com os membros da equipe para dúvidas, sugestões ou colaborações no projeto:

* **André Doerner Duarte**
  * 📧 E-mail: andredoernerd@gmail.com
  * 💼 [LinkedIn](https://www.linkedin.com/in/andredoernerduarte/)
  * 🐙 [GitHub](https://github.com/DedeDuarte)

* **Matheus Leonardo Cardoso Kroeff**
  * 📧 E-mail: 10426434@mackenzista.com.br
  * 💼 [LinkedIn](https://www.linkedin.com/in/matheus-leonardo-cardoso-kroeff-23838535b/)
  * 🐙 [GitHub](https://github.com/Matheus-Kroeff)

* **Naoto Ushizaki**
  * 📧 E-mail: 10437445@mackenzista.com.br
  * 💼 [LinkedIn](https://www.linkedin.com/in/naoto-ushizaki-30b94a172/)
  * 🐙 [GitHub](https://github.com/mackcoder)

## 8. 📚 Referências

* INSTITUTO PAULO MONTENEGRO. **INAF Brasil 2024: alfabetismo do analógico ao digital.** São Paulo, 2024. Disponível em: https://alfabetismofuncional.org.br/2024_Relatorio_Inaf_DIGITAL.pdf. Acesso em: 19 ago. 2026.

* SOUZA, Rakel Brandão de et al. **Produção de conhecimento sobre o analfabetismo funcional e as desigualdades sociais no Brasil.** Goiânia, 2025. Disponível em: https://revistas.ufg.br/revistaufg/article/view/75874. Acesso em: 19 ago. 2026.

* CORRÊA, Barbara Priscila de Souza; COLETTA, Carolina. **O acesso ao crédito em bancos digitais e fintechs aumenta a probabilidade de endividamento pessoal no Brasil?** 2024. Disponível em: https://ojs.unifor.br/rca/article/view/14924. Acesso em: 19 ago. 2026.

