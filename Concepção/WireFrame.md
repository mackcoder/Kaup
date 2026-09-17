# Wire Frames
### [Link para Figma funcnional](https://www.figma.com/make/Ul3LXBxImcey4sMvcjQN68/Sem-t%C3%ADtulo?t=PMgsx6C8KpeQ0aX6-20&fullscreen=1)

## 1. Tela Inicial

<p align="center">
    <img src="WireFrame_prints/1.0%20-%20Tela%20Inicial.png" width="300">
    <img src="WireFrame_prints/1.1%20-%20Tela%20Inicial%20-%20Meus%20Documentos.png" width="300">
</p>

Na tela inicial, temos acesso às principais funcionalidades do aplicativo. A navegação foi pensada para ser simples e fácil de entender, utilizando imagens claras, cores consistentes e blocos clicáveis grandes para ajudar o usuário a identificar cada opção.

### Principais elementos

- __Enviar Documento__:__ Permite que o usuário envie um documento e inicie uma nova conversa com a IA.
- __Meus Documentos:__ Mostra conversas anteriores organizadas por contexto, facilitando o acesso a documentos e chats relacionados ao mesmo assunto.
- __Menu de Perfil:__ Permite acessar as configurações da conta e algumas preferências do aplicativo.

### Fluxo

Ao selecionar __Enviar Documento__, o usuário é direcionado para a tela de envio e análise do documento.

Ao selecionar um _contexto_, o usuário é direcionado para uma página que reúne os documentos e conversas relacionados àquele assunto.

---

## 2. Documentos por Contexto

<p align="center">
    <img src="WireFrame_prints/2.0%20-%20Documentos%20Por%20Contexto.png" width="300">
</p>

Nesta tela, o usuário consegue visualizar todos os documentos relacionados a um mesmo contexto. No exemplo, a categoria selecionada é __Carros__, reunindo contratos, financiamentos, seguros e outros documentos relacionados.

A organização busca facilitar a visualização rápida do estado de cada documento, utilizando cores, ícones e textos curtos para indicar sua situação.

### Principais elementos

- __Categoria:__ Mostra o contexto atual e a quantidade de documentos relacionados a ele.
- __Resumo de Status:__ Apresenta rapidamente quantos documentos estão aprovados, precisam de atenção ou ainda estão aguardando análise.
- __Adicionar Documento:__ Permite iniciar um novo Chat sobre outro documento dentro, ou não, do contexto atual.
- __Lista de Documentos:__ Exibe os documentos relacionados ao contexto, mostrando nome, tipo, data e status.
- __Status Visual:__ Utiliza cores e ícones diferentes para facilitar a identificação da situação de cada documento.

### Fluxo

Ao selecionar __Adicionar Documento__, o usuário pode enviar um novo arquivo, iniciando um novo Chat.

Ao selecionar um dos documentos da lista, o usuário é direcionado para a página de análise e conversa referente àquele documento.

O botão __Voltar__ retorna para a tela inicial.

---

## 3. Todos os Documentos

<p align="center">
    <img src="WireFrame_prints/3.0%20-%20Todos%20os%20Documentos.png" width="300">
    <img src="WireFrame_prints/3.1%20-%20Todos%20os%20Documentos%20%2B%20Filtro.png" width="300">
</p>

Nesta tela, o usuário consegue visualizar todos os documentos cadastrados no aplicativo, independentemente do contexto ao qual pertencem. Os documentos ficam separados por categorias, facilitando a localização e a organização das informações.

Também é possível utilizar a busca e os filtros de status para encontrar documentos com mais rapidez.

### Principais elementos

- __Barra de Busca:__ Permite pesquisar documentos pelo nome.
- __Filtros de Status:__ Possibilitam visualizar apenas documentos aprovados, que precisam de atenção ou que ainda estão aguardando análise.
- __Categorias:__ Separam os documentos de acordo com seu contexto, como _Carros_, _Casa_, _Trabalho_ e _Banco_.
- __Contador de Documentos:__ Mostra a quantidade de documentos em cada categoria e em cada status.
- __Lista de Documentos:__ Apresenta informações como nome, categoria, data e status de cada documento.
- __Status Visual:__ Utiliza ícones e cores para facilitar a identificação da situação de cada documento.

### Fluxo

Ao digitar na __Barra de Busca__, a lista é filtrada de acordo com o documento procurado.

Ao selecionar um dos __Filtros de Status__, são exibidos apenas os documentos que se encontram naquela situação.

Ao selecionar um documento, o usuário é direcionado para sua página de análise e conversa.

O botão __Voltar__ retorna para a tela anterior.

---

## 4. Início de Novo Chat

<p align="center">
    <img src="WireFrame_prints/4.0%20-%20Criando%20Novo%20Chat.png" width="300">
</p>

Sempre que o usuário clicar em _enviar documento_ ou _adicionar documento_, o Mesmo será redirecionado para essa página, onde pode escolher como deseja enviar um documento para o aplicativo. A ideia é deixar esse processo o mais simples possível, oferecendo apenas duas opções bem claras: tirar uma foto ou selecionar um arquivo já existente no dispositivo.

### Principais elementos

- __Tirar uma Foto:__ Abre a câmera do dispositivo para que o usuário possa fotografar o documento.
- __Escolher Arquivo:__ Permite selecionar um PDF, imagem ou outro documento armazenado no dispositivo.
- __Janela de Seleção:__ Aparece sobre a tela atual, sem tirar o usuário completamente do contexto em que estava.

### Fluxo

Ao selecionar __Tirar uma Foto__, a câmera do dispositivo é aberta para realizar o envio do documento.

Ao selecionar __Escolher Arquivo__, o usuário pode procurar e selecionar um arquivo armazenado no dispositivo.

Após escolher o documento, o usuário segue para a etapa de leitura e análise do conteúdo.

---

## 5. Análise e Conversa com a IA

<p align="center">
    <img src="WireFrame_prints/5.0%20-%20Chat.png" width="300">
    <img src="WireFrame_prints/5.1%20-%20Chat.png" width="300">
</p>

<p align="center">
    <img src="WireFrame_prints/5.2%20-%20Chat.png" width="300">
    <img src="WireFrame_prints/5.3%20-%20Chat%20%2B%20Contexto.png" width="300">
</p>

Nesta tela, o usuário consegue visualizar a análise feita pela IA e continuar a conversa para entender melhor o documento. A ideia é apresentar primeiro as informações mais importantes de forma simples e, quando necessário, fazer perguntas extras para conseguir uma análise mais completa.

### Principais elementos

- __Resumo da Análise:__ Mostra os principais dados encontrados no documento, como valores, prazos e possíveis pontos de atenção.
- __Assistente IA:__ Explica o conteúdo do documento em uma linguagem mais simples e faz perguntas quando precisa de mais informações.
- __Perguntas Guiadas:__ Algumas respostas podem ser feitas por opções prontas, evitando que o usuário precise digitar tudo.
- __Alertas:__ Destacam pontos do documento que merecem atenção.
- __Campo de Conversa:__ Permite que o usuário faça perguntas sobre o documento após a análise inicial.
- __Informações do Documento:__ Mantém visível o nome, tipo e contexto do documento analisado.

### Fluxo

Após o envio do documento, a IA realiza uma análise inicial e apresenta os principais dados encontrados.

Caso seja necessária alguma informação complementar, o assistente faz uma pergunta e apresenta opções de resposta para facilitar a interação.

Depois da resposta, a análise pode ser atualizada com novas informações e possíveis alertas.

Após essa etapa, o usuário pode continuar conversando livremente com a IA pelo campo de mensagem.

Ao clicar em _Contexto_, o usuário pode visualizar todos os documentos relacionados àquela sessão e adicionar novos arquivos que possam ajudar na análise, caso necessário.

---

## 6. Perfil e Configurações

<p align="center">
    <img src="WireFrame_prints/6.0%20-%20Configurações%20-%20Perfil.png" width="300">
    <img src="WireFrame_prints/6.1%20-%20Configurações%20-%20Meus%20Documentos.png" width="300">
    <img src="WireFrame_prints/6.2%20-%20Configurações%20-%20Explicações%20%2B%20Letras.png" width="300">
</p>

<p align="center">
    <img src="WireFrame_prints/6.3%20-%20Configurações%20-%20Renda%20(personalização).png" width="300">
    <img src="WireFrame_prints/6.4%20-%20Configurações%20-%20Privacidade.png" width="300">
    <img src="WireFrame_prints/6.5%20-%20Configurações%20-%20Ajuda.png" width="300">
</p>

Nesta tela, o usuário pode visualizar suas informações pessoais, acompanhar seus documentos e ajustar algumas preferências do aplicativo.

As configurações foram organizadas em blocos grandes e separados por assunto, buscando facilitar a localização das opções e deixar a navegação mais simples.

### Principais elementos

- __Dados do Perfil:__ Exibe nome, e-mail e foto do usuário.
- __Resumo dos Documentos:__ Mostra quantos documentos estão aprovados, precisam de atenção ou ainda aguardam análise.
- __Preferência de Explicação:__ Permite escolher o nível de detalhe usado nas respostas da IA.
- __Tamanho das Letras:__ Permite alterar o tamanho dos textos exibidos no aplicativo.
- __Faixa de Renda:__ Pode ser informada para ajudar em algumas análises financeiras, sendo uma informação opcional.
- __Privacidade:__ Reúne opções relacionadas aos documentos, análises e dados do usuário.
- __Ajuda:__ Oferece diferentes formas de aprender a utilizar o aplicativo ou pedir auxílio.
- __Sair da Conta:__ Encerra a sessão atual do usuário.

### Fluxo

- __Dados do Perfil:__ Permite visualizar e alterar as informações da conta.
- __Meus Documentos:__ Permite acessar documentos separados por status ou visualizar todos de uma vez.
- __Preferência de Explicação:__ Define se as respostas da IA serão mais simples, normais ou completas.
- __Tamanho das Letras:__ Ajusta o tamanho dos textos exibidos no aplicativo.
- __Informações Pessoais:__ Permite salvar algumas informações do usuário para que a IA possa reutilizá-las em outras conversas, sem precisar perguntar tudo novamente
  - No protótipo, implementamos apenas a faixa de renda como exemplo.
- __Privacidade:__ Reúne opções para gerenciar documentos, análises e dados da conta.
- __Ajuda:__ Oferece instruções visuais, em áudio ou suporte de uma pessoa.
- __Sair da Conta:__ Encerra a sessão atual do usuário.
