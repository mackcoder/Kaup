export default function TypingBubble({ color }: { color: string }) {
  return (
    <div className="kaup-187">
      {[0, 1, 2].map((i) => (
        <div
          key={i}
          className="kaup-188"
          style={{
            background: color,
            animation: `cdBounce 1.2s ease-in-out ${i * 0.2}s infinite`,
          }}
        />
      ))}
    </div>
  );
}
