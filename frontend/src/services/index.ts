import { ApiService } from "./api/ApiService";
import { MockService } from "./mock/MockService";
import type { KaupService } from "./contracts/KaupService";
export function createService(
  mode = import.meta.env.VITE_DATA_SOURCE ?? "mock",
): KaupService {
  if (mode === "mock") return new MockService();
  if (mode === "api") {
    const timeout = Number(import.meta.env.VITE_API_TIMEOUT_MS ?? 20000);
    return new ApiService(
      import.meta.env.VITE_API_BASE_URL ?? "",
      Number.isFinite(timeout) && timeout > 0 ? timeout : 20000,
    );
  }
  throw new Error("VITE_DATA_SOURCE deve ser mock ou api.");
}
export function errorMessage(error: unknown) {
  return error instanceof Error
    ? error.message
    : "Ocorreu um erro. Tente novamente.";
}
