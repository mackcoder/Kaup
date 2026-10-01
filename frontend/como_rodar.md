# Como rodar e compartilhar o frontend

O frontend do Kaup é uma aplicação React com Vite. Execute os comandos abaixo a partir da **raiz do projeto** (`Kaup`), salvo quando indicado.

## 1. Confira a versão do Node.js

```bash
node -v
```

Este projeto usa Vite 8 e precisa de **Node.js 20.19+ ou 22.12+**. Recomendamos Node 22. Se o comando mostrar `v12` ou `v18`, instale uma versão compatível com o [nvm](https://github.com/nvm-sh/nvm#installing-and-updating):

```bash
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.8/install.sh | bash
source ~/.nvm/nvm.sh
nvm install 22
nvm use 22
node -v
```

Se o nvm já estiver instalado, execute `nvm install 22` uma vez e `nvm use 22` ao abrir um novo terminal. Se o comando `nvm` não for reconhecido, execute `source ~/.nvm/nvm.sh`.

## 2. Rode no seu computador

```bash
cd frontend/src
npm install
npm run dev
```

Abra **http://localhost:8443** no navegador. Para parar o servidor, pressione `Ctrl+C`.

Se aparecer `EBADENGINE` ou `SyntaxError: Unexpected token '.'`, confira `node -v`: o terminal ainda está usando uma versão antiga do Node. Ative o Node 22 e execute `npm install` novamente.

## 3. Publique para outras pessoas acessarem

Com o Node 22 ativo e dentro de `frontend/src`, gere a versão de publicação:

```bash
npm run build
```

O Vite criará a pasta `frontend/src/dist`. Acesse o [Netlify Drop](https://app.netlify.com/drop), arraste **a pasta `dist`** para a página e compartilhe o endereço `netlify.app` gerado. Para publicar alterações depois, rode `npm run build` novamente e envie a nova pasta `dist`. Consulte também a [documentação do Netlify](https://docs.netlify.com/start/quickstarts/netlify-drop-quickstart/).

Essa publicação disponibiliza a interface. Funcionalidades que dependam do backend ou do banco de dados exigem que esses serviços também estejam hospedados e conectados ao frontend.
