# Atividade (GOMS)

---

## 1. GOMS
* **Goals (Metas):** 
* **Operators (Operadores - OP):** 
* **Methods (Métodos):** 
* **Selection Rules (Regras de Seleção):** 

## 2. Estrutura

### **GOAL 0: Analisar um contrato físico no aplicativo**

---
### **GOAL 1: Iniciar captura do documento**
* `OP`: Olhar para a tela inicial do aplicativo
* `OP`: Localizar visualmente o botão "Nova Análise"
* `OP`: Tocar no botão "Nova Análise"
* `OP`: Decidir a forma de entrada do documento *(Regra de Seleção)*

#### **Regra de Seleção para Entrada de Documento:**
* **Se** o usuário possui o contrato em arquivo digital (PDF/DOCX):
  * **Use METHOD:** Upload de Arquivo
* **Se** o usuário possui apenas as folhas impressas do documento:
  * **Use METHOD:** Captura por Câmera

#### **METHOD: Captura por Câmera**
1. `OP`: Tocar na opção "Tirar Foto"
2. `OP`: Posicionar a folha de papel sobre uma superfície iluminada
3. `OP`: Alinhar as bordas da folha dentro do retângulo guia na tela
4. `OP`: Tocar no botão de captura
5. `OP`: Olhar para a tela de pré-visualização para checar a foto
6. `OP`: Tocar no botão "Continuar" para confirmar a legibilidade e enviar

---

### **GOAL 2: Obter interpretação do contrato**
* `OP`: Aguardar processamento do OCR (Optical Character Recognition) e análise da IA 
* `OP`: Visualizar a análise estruturada (Valores, Prazos e Riscos)
* `OP`: Escolher o formato de consumo das informações *(Regra de Seleção)* 

#### **Regra de Seleção para Consumo de Informação:**
* **Se** o usuário prefere resumo em áudio ou tem baixa instrução para leitura:
  * **Use METHOD:** Ouvir Resumo em Áudio
* **Se** o usuário prefere ler o resumo:
  * **Use METHOD:** Leitura Direta na Interface

#### **METHOD: Ouvir Resumo em Áudio**
1. `OP`: Localizar visualmente o botão "Ouvir Resumo"
2. `OP`: Tocar no botão "Ouvir Resumo"
3. `OP`: Escutar a síntese dos juros, prazos e penalidades narrada pelo assistente

---

### **GOAL 3: Tirar dúvida específica sobre valores ou parcelas**
* `OP`: Tocar no ícone de microfone no campo de entrada
* `OP`: Falar a dúvida em voz alta: *"Qual é o valor final somando todos os juros?"*
* `OP`: Tocar novamente no microfone para finalizar e enviar o áudio
* `OP`: Aguardar a geração da resposta pela IA
* `OP`: Ler o texto sintetizado ou escutar o áudio com a resposta explicativa
