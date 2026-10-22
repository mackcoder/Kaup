import type { VisualStatus } from "../types/domain";
interface FixtureCategory {
  key: string;
  docs: {
    emoji: string;
    name: string;
    description: string;
    date: string;
    status: VisualStatus;
  }[];
}
export const FIXTURES: FixtureCategory[] = [
  {
    key: "Carros",
    docs: [
      {
        emoji: "🚗",
        name: "Carro Gol 2016",
        description: "Contrato de compra e venda",
        date: "12 ago 2026",
        status: "ok",
      },
      {
        emoji: "📄",
        name: "Financiamento Gol 2016",
        description: "Contrato de financiamento",
        date: "12 ago 2026",
        status: "warning",
      },
      {
        emoji: "🛡️",
        name: "Seguro Gol 2016",
        description: "Apólice do seguro do veículo",
        date: "03 mai 2026",
        status: "ok",
      },
      {
        emoji: "🔧",
        name: "Manutenção Gol 2016",
        description: "Documento de revisão",
        date: "20 jan 2026",
        status: "pending",
      },
    ],
  },
  {
    key: "Casa",
    docs: [
      {
        emoji: "🏠",
        name: "Aluguel Ap. Centro",
        description: "Contrato de locação",
        date: "01 set 2026",
        status: "ok",
      },
      {
        emoji: "⚡",
        name: "Energia Elétrica",
        description: "Contrato com a distribuidora",
        date: "15 mar 2026",
        status: "ok",
      },
      {
        emoji: "🚿",
        name: "Serviço de Água",
        description: "Contrato de fornecimento",
        date: "15 mar 2026",
        status: "warning",
      },
      {
        emoji: "📶",
        name: "Internet e TV",
        description: "Contrato de telecomunicações",
        date: "10 fev 2026",
        status: "ok",
      },
      {
        emoji: "🔨",
        name: "Reforma Cozinha",
        description: "Contrato com a construtora",
        date: "22 jun 2026",
        status: "pending",
      },
    ],
  },
  {
    key: "Trabalho",
    docs: [
      {
        emoji: "📋",
        name: "Contrato de Trabalho",
        description: "CLT — Empresa XYZ Ltda.",
        date: "05 jan 2025",
        status: "ok",
      },
      {
        emoji: "💰",
        name: "Rescisão Contratual",
        description: "Termo de rescisão 2024",
        date: "30 dez 2024",
        status: "warning",
      },
      {
        emoji: "🤝",
        name: "Contrato Freelance",
        description: "Prestação de serviços",
        date: "14 ago 2026",
        status: "ok",
      },
      {
        emoji: "📑",
        name: "Acordo de Sigilo",
        description: "NDA com cliente",
        date: "14 ago 2026",
        status: "ok",
      },
      {
        emoji: "🏆",
        name: "Bônus e Metas",
        description: "Adendo salarial 2026",
        date: "01 mar 2026",
        status: "pending",
      },
      {
        emoji: "🩺",
        name: "Plano de Saúde",
        description: "Contrato do convênio médico",
        date: "05 jan 2025",
        status: "ok",
      },
    ],
  },
  {
    key: "Banco",
    docs: [
      {
        emoji: "💳",
        name: "Cartão de Crédito",
        description: "Contrato do cartão Banco X",
        date: "10 out 2025",
        status: "warning",
      },
      {
        emoji: "🏦",
        name: "Conta Corrente",
        description: "Contrato de abertura de conta",
        date: "03 abr 2023",
        status: "ok",
      },
      {
        emoji: "💸",
        name: "Empréstimo Pessoal",
        description: "Contrato de crédito pessoal",
        date: "18 jul 2026",
        status: "warning",
      },
      {
        emoji: "📈",
        name: "Investimento CDB",
        description: "Contrato de aplicação",
        date: "02 ago 2026",
        status: "ok",
      },
    ],
  },
  {
    key: "Outros",
    docs: [
      {
        emoji: "🏥",
        name: "Plano Dentário",
        description: "Contrato do convênio odonto",
        date: "12 fev 2026",
        status: "ok",
      },
      {
        emoji: "📱",
        name: "Celular Pós-Pago",
        description: "Contrato com a operadora",
        date: "07 nov 2025",
        status: "ok",
      },
      {
        emoji: "🎓",
        name: "Curso Online",
        description: "Contrato de matrícula",
        date: "30 jan 2026",
        status: "pending",
      },
      {
        emoji: "🐾",
        name: "Pet Shop Mensal",
        description: "Contrato de serviço pet",
        date: "01 abr 2026",
        status: "ok",
      },
      {
        emoji: "🚚",
        name: "Mudança Residencial",
        description: "Contrato da transportadora",
        date: "25 set 2026",
        status: "warning",
      },
      {
        emoji: "📰",
        name: "Assinatura Digital",
        description: "Contrato de assinatura",
        date: "15 mai 2026",
        status: "ok",
      },
    ],
  },
];
