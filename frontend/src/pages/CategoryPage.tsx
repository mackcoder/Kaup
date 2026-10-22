import { useState } from "react";
import type { CategoryView, DocumentView } from "../types/domain";
import { STATUS } from "../styles/status";
import UploadDialog from "../components/UploadDialog";
interface Props {
  cat: CategoryView;
  onBack: () => void;
  onOpenDoc: (doc: DocumentView) => void;
}

export default function CategoryPage({ cat, onBack, onOpenDoc }: Props) {
  const [tapped, setTapped] = useState<number | null>(null);
  const [showUpload, setShowUpload] = useState(false);

  const handleTap = (idx: number) => {
    setTapped(idx);
    setTimeout(() => {
      setTapped(null);
      const doc = cat.docs[idx];
      if (doc) onOpenDoc(doc);
    }, 150);
  };

  return (
    <div className="kaup-1">
      {/* ── HERO HEADER ── */}
      <div
        className="kaup-2"
        style={{
          background: `linear-gradient(145deg, ${cat.gradient[0]} 0%, ${cat.gradient[1]} 100%)`,
        }}
      >
        {/* Decorative circles */}
        <div className="kaup-3" />
        <div className="kaup-4" />

        {/* Back button */}
        <button onClick={onBack} className="kaup-5">
          <span className="kaup-6">←</span>
          <span className="kaup-7">Voltar</span>
        </button>

        {/* Title row */}
        <div className="kaup-8">
          <div className="kaup-9">{cat.emoji}</div>
          <div>
            <div className="kaup-10">CATEGORIA</div>
            <div className="kaup-11">{cat.label}</div>
            <div className="kaup-12">
              {cat.docs.length} documento{cat.docs.length !== 1 ? "s" : ""}
            </div>
          </div>
        </div>
      </div>

      {/* ── STATS ROW ── */}
      <div className="kaup-13">
        {(["ok", "warning", "pending"] as const).map((s, i) => {
          const count = cat.docs.filter((d) => d.status === s).length;
          const cfg = STATUS[s];
          return (
            <div
              key={s}
              className="kaup-14"
              style={{ borderRight: i < 2 ? "1.5px solid #f1f5f9" : "none" }}
            >
              <div className="kaup-15" style={{ color: cfg.color }}>
                {count}
              </div>
              <div className="kaup-16">{cfg.label}</div>
            </div>
          );
        })}
      </div>

      {/* ── ADD BUTTON ── */}
      <div className="kaup-17">
        <button
          onClick={() => setShowUpload(true)}
          className="kaup-18"
          style={{
            background: cat.colorBg,
            border: `2.5px dashed ${cat.colorLight}`,
          }}
        >
          <span className="kaup-19">➕</span>
          <span className="kaup-20" style={{ color: cat.color }}>
            Adicionar documento
          </span>
        </button>
      </div>

      {/* ── DOCUMENT LIST ── */}
      <div className="kaup-21">
        <div className="kaup-22">Documentos</div>

        {cat.docs.map((doc, idx) => {
          const st = STATUS[doc.status];
          const isTapped = tapped === idx;
          return (
            <button
              key={idx}
              onClick={() => handleTap(idx)}
              className="kaup-23"
              style={{
                border: `2px solid ${isTapped ? cat.colorLight : "#e2e8f0"}`,
                boxShadow: isTapped
                  ? `0 4px 20px ${cat.colorLight}25`
                  : "0 2px 8px rgba(0,0,0,0.05)",
                transform: isTapped ? "scale(0.98)" : "scale(1)",
              }}
            >
              {/* Emoji icon */}
              <div className="kaup-24" style={{ background: cat.colorBg }}>
                {doc.emoji}
              </div>

              {/* Text */}
              <div className="kaup-25">
                <div className="kaup-26">{doc.name}</div>
                <div className="kaup-27">{doc.description}</div>
                <div className="kaup-28">{doc.date}</div>
              </div>

              {/* Status badge */}
              <div className="kaup-29">
                <div
                  className="kaup-30"
                  style={{
                    background: st.bg,
                    border: `1.5px solid ${st.border}`,
                  }}
                >
                  {st.emoji}
                </div>
                <span className="kaup-31" style={{ color: st.color }}>
                  {st.label}
                </span>
              </div>

              {/* Chevron */}
              <div className="kaup-32">›</div>
            </button>
          );
        })}
      </div>

      {showUpload && (
        <UploadDialog
          categoryId={cat.key}
          onClose={() => setShowUpload(false)}
        />
      )}
    </div>
  );
}
