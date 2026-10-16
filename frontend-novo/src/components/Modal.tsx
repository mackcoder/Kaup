import { useEffect, useRef, type ReactNode } from "react";
export default function Modal({
  children,
  onClose,
  label,
  sheetClassName,
  fullOverlay = false,
}: {
  children: ReactNode;
  onClose: () => void;
  label: string;
  sheetClassName?: string;
  fullOverlay?: boolean;
}) {
  const ref = useRef<HTMLDivElement>(null);
  const closeRef = useRef(onClose);
  closeRef.current = onClose;
  useEffect(() => {
    const previous =
      document.activeElement instanceof HTMLElement
        ? document.activeElement
        : null;
    const panel = ref.current;
    panel?.querySelector<HTMLElement>("button,input")?.focus();
    function key(e: KeyboardEvent) {
      if (e.key === "Escape") {
        e.preventDefault();
        closeRef.current();
      }
      if (e.key !== "Tab" || !panel) return;
      const nodes = [
        ...panel.querySelectorAll<HTMLElement>(
          'button:not(:disabled),input:not(:disabled),[tabindex="0"]',
        ),
      ].filter((n) => n.getClientRects().length);
      const first = nodes[0],
        last = nodes.at(-1);
      if (!first) {
        e.preventDefault();
        panel.focus();
        return;
      }
      if (e.shiftKey && document.activeElement === first) {
        e.preventDefault();
        last?.focus();
      } else if (!e.shiftKey && document.activeElement === last) {
        e.preventDefault();
        first.focus();
      }
    }
    document.addEventListener("keydown", key);
    const overflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", key);
      document.body.style.overflow = overflow;
      previous?.focus();
    };
  }, []);
  return (
    <div
      className={fullOverlay ? "modal-overlay full-overlay" : "modal-overlay"}
      onClick={onClose}
    >
      <div
        className={sheetClassName ?? "modal-sheet"}
        role="dialog"
        aria-modal="true"
        aria-label={label}
        tabIndex={-1}
        ref={ref}
        onClick={(e) => e.stopPropagation()}
      >
        {!sheetClassName && <div className="sheet-handle" />}
        {children}
      </div>
    </div>
  );
}
