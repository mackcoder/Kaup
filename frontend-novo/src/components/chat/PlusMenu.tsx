export default function PlusMenu({
  onClose,
  onSelect,
  catColorBg,
}: {
  onClose: () => void;
  onSelect: (kind: "photo" | "file" | "context") => void;
  catColorBg: string;
}) {
  const items = [
    {
      emoji: "📷",
      label: "Tirar foto",
      sub: "Aponte a câmera para o documento",
    },
    { emoji: "📁", label: "Enviar arquivo", sub: "PDF, imagem ou documento" },
    {
      emoji: "📎",
      label: "Adicionar ao contexto",
      sub: "Usar como referência na análise",
    },
  ];
  return (
    <>
      <div onClick={onClose} className="kaup-174" />
      <div className="kaup-175">
        {items.map((item, i) => (
          <button
            key={i}
            onClick={() => {
              onClose();
              onSelect(i === 0 ? "photo" : i === 1 ? "file" : "context");
            }}
            className="kaup-176"
            style={{
              borderBottom: i < items.length - 1 ? "1px solid #f1f5f9" : "none",
            }}
          >
            <div className="kaup-177" style={{ background: catColorBg }}>
              {item.emoji}
            </div>
            <div>
              <div className="kaup-178">{item.label}</div>
              <div className="kaup-144">{item.sub}</div>
            </div>
          </button>
        ))}
      </div>
    </>
  );
}
