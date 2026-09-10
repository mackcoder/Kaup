# 📝Concepção
![Quais artefatos?](image.png)
---
## 💼 Visão e Caso de Negócio
<div align="justify">
O projeto visa auxiliar analfabetos funcionais na interpretação de contratos com o intuito de evitar com que caiam em golpes. 
</div>

---
| Seção | Conteúdo Principal |
| :--- | :--- |
| **Problema ou Oportunidade** | Prevenir que usuários sofram prejuízos financeiros ou contratuais causados pela dificuldade de compreensão de cláusulas e termos jurídicos. |
| **Análise de Alternativas** | Acesso direto via web (QR Code): descartado por riscos de infraestrutura, especificamente o consumo excessivo de recursos e a alta latência na transmissão de dados/mídia e no tempo de resposta do agente de IA |
| **Custos e Recursos** | O projeto contará com uma equipe dedicada de 3 pessoas para o desenvolvimento. O Figma será adotado como ferramenta principal para o design da interface do usuário (UI) e experiência do usuário (UX).|
| **Benefícios Esperados** | Redução do índice de fraudes e prejuízos contratuais, proporcionando maior previsibilidade, autonomia e segurança jurídica para o usuário. |
| **Riscos** | Tratamento indevido de dados sensíveis/pessoais (LGPD); Qualidade ruim dos documentos; Baixa confiança do usuário na ferramenta. |
| **Mitigação** | Anonimizar dados automaticamente antes de enviar à API e exclusão imediata; implementar um validador de imagem no app via OCR; interface intuitiva com alertas visuais por níveis de risco com linguagem acessível.|

## ⫶☰ Glossário
### Termos técnicos e de IA
- IA: Inteligência Artificial
- Latência: Tempo de resposta entre envio da requisição e resposta da IA.
- OCR (Optical Character Recognition): Tecnologia de reconhecimento óptico de caracteres usada para converter fotos ou arquivos PDF digitalizados de contratos em texto editável e legível por máquina

### Termos do domínio Jurídico/Contratual
-   LGPD(Lei Geral de Proteção de Dados): Legislação brasileira que regula o tratamento e privacidade de dados pessoais físicss e digitais.    

    ## 📜 Lista de riscos + plano de Gestão dos riscos
    ### RISCOS
    - Leitura/Análise imprecisa do agente ao escanear fotos de contratos com baixa qualidade
    - Falta de detalhes pode levar a categorização incorreta dos tipos de contratos
    - O agente pode acabar perguntando repetidamente por detalhes sobre o usuário, nos quais já poderia estar armazenados em seu banco de dados.    

    ### GESTÃO DOS RISCOS
    - O agente irá procurar ou pedir detalhes adicionais sobre o contrato para poder categorizar comm precisão o tipo do contrato (residencial, comércio, trabalho). 
    - Implementação de uma estrutura de dados para armazenar informações de usuários.
    - 
    
    ## 🛠️ Protótipos e provas de conceitos
    

    ## 💡 Plano de iteração
    1. Upload de `Contrato`
    2. Validação da captura ()
    3. Extração de texto (OCR)
    4. Exibição do conteúdo extraído, validando se a captura funciona de forma confiável antes de integrar a camada de IA. // Colocar em bullet points
    
    ## 🛠️ Plano de desenvolvimento de software
    -
    -
    -
    -

    ## 🛠️ Pasta de desenvolvimento

