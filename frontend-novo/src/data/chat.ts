import type { ContextFile, DocumentView } from "../types/domain";
export const INCOME_OPTIONS = [
  "Menos de R$ 3.000",
  "Entre R$ 3.000 e R$ 5.000",
  "Entre R$ 5.000 e R$ 7.000",
  "Entre R$ 7.000 e R$ 15.000",
  "Entre R$ 15.000 e R$ 20.000",
  "Mais de R$ 20.000",
  "Outra opção",
];

export const INCOME_RESPONSES: Record<string, string> = {
  "Menos de R$ 3.000":
    "⚠️ A parcela comprometeria mais de 40% da sua renda. Especialistas recomendam no máximo 30%. Isso pode ser arriscado. Avalie bem antes de assinar.",
  "Entre R$ 3.000 e R$ 5.000":
    "⚠️ A parcela pode comprometer entre 25% e 42% da sua renda. Isso pode ser elevado. Verifique se você tem outras despesas fixas antes de fechar o contrato.",
  "Entre R$ 5.000 e R$ 7.000":
    "✅ A parcela comprometeria entre 18% e 25% da sua renda. Está dentro do limite recomendado, mas verifique outras dívidas antes de assinar.",
  "Entre R$ 7.000 e R$ 15.000":
    "✅ A parcela comprometeria entre 8% e 18% da sua renda. Parece adequado. Compare com outras opções de financiamento antes de decidir.",
  "Entre R$ 15.000 e R$ 20.000":
    "✅ A parcela comprometeria menos de 8% da sua renda. O compromisso financeiro é relativamente baixo em relação ao que você ganha.",
  "Mais de R$ 20.000":
    "✅ A parcela comprometeria menos de 7% da sua renda. O impacto no seu orçamento seria pequeno. Ainda assim, leia todas as cláusulas.",
  "Outra opção":
    "Tudo bem! Independente da renda, o mais importante é verificar se as parcelas cabem no seu orçamento junto com todas as despesas fixas. Quer que eu explique alguma cláusula?",
};

const FREE_TEXT_RESPONSES: Array<{ keywords: string[]; response: string }> = [
  {
    keywords: ["multa", "cancelar", "rescisão", "sair", "desistir"],
    response:
      "A multa por cancelamento antecipado geralmente é de 10% a 20% do valor restante. Verifique a cláusula de rescisão para saber o valor exato antes de assinar.",
  },
  {
    keywords: ["juros", "taxa", "cet", "percentual", "custo"],
    response:
      "Os juros são o custo extra pelo dinheiro emprestado. Fique atento ao CET (Custo Efetivo Total), que inclui juros, taxas e seguros. Quanto menor, melhor para você.",
  },
  {
    keywords: ["prazo", "meses", "anos", "duração", "tempo", "parcelas"],
    response:
      "O prazo é de 48 meses (4 anos). Quanto mais longo o prazo, menor a parcela, mas você paga mais juros no total. Prazos curtos custam menos no geral.",
  },
  {
    keywords: ["assinar", "assinatura", "documento", "papel"],
    response:
      "Antes de assinar, certifique-se de ler todas as páginas. Peça para explicar qualquer trecho que não entender. Nunca assine em branco e guarde sempre uma cópia.",
  },
];

const DEFAULT_FREE_RESPONSE =
  'Posso explicar cláusulas, valores, prazos, multas e pontos de atenção deste contrato. Basta perguntar de forma simples, como "O que acontece se eu atrasar?" ou "Qual é a multa se eu cancelar?"';

export function getFreeTextResponse(text: string): string {
  const lower = text.toLowerCase();
  for (const { keywords, response } of FREE_TEXT_RESPONSES) {
    if (keywords.some((k) => lower.includes(k))) return response;
  }
  return DEFAULT_FREE_RESPONSE;
}

export function getContextFiles(doc: DocumentView): ContextFile[] {
  const base: ContextFile = {
    emoji: doc.emoji,
    name: doc.name,
    type: "Principal",
    isMain: true,
  };
  const extras: Record<string, ContextFile[]> = {
    Carros: [
      {
        emoji: "🚗",
        name: "Contrato de compra e venda",
        type: "Documento relacionado",
      },
      {
        emoji: "📑",
        name: "Comprovante de renda.pdf",
        type: "Documento de apoio",
      },
    ],
    Casa: [
      {
        emoji: "🏠",
        name: "Vistoria do imóvel.pdf",
        type: "Documento relacionado",
      },
      {
        emoji: "📑",
        name: "Comprovante de endereço.pdf",
        type: "Documento de apoio",
      },
    ],
    Trabalho: [
      {
        emoji: "💼",
        name: "Holerite — último mês",
        type: "Documento de apoio",
      },
      {
        emoji: "📑",
        name: "Carteira de trabalho.pdf",
        type: "Documento relacionado",
      },
    ],
    Banco: [
      {
        emoji: "📑",
        name: "Extrato bancário — 3 meses",
        type: "Documento de apoio",
      },
      {
        emoji: "🏦",
        name: "Proposta comercial.pdf",
        type: "Documento relacionado",
      },
    ],
    Outros: [
      {
        emoji: "📄",
        name: "Documento adicional.pdf",
        type: "Documento relacionado",
      },
    ],
  };
  return [base, ...(extras[doc.catKey] ?? extras["Outros"])];
}
