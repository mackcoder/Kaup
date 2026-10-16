import type { ProfileModel } from "../../features/profile/useProfile";
export default function LoggedOut({ model }: { model: ProfileModel }) {
  const { setLoggedOut, fs, s, onBack } = model;
  return (
    <div
      style={{
        ...s.page,
        alignItems: "center",
        justifyContent: "center",
        gap: 24,
      }}
    >
      <span className="kaup-212">👋</span>
      <div className="kaup-213" style={{ fontSize: fs(22) }}>
        Até logo!
      </div>
      <div className="kaup-214" style={{ fontSize: fs(15) }}>
        Saída demonstrativa. Volte quando quiser.
      </div>
      <button
        onClick={() => {
          setLoggedOut(false);
          onBack();
        }}
        className="kaup-215"
        style={{ fontSize: fs(16) }}
      >
        🏠 Ir para o início
      </button>
    </div>
  );
}
