export const ACCEPTED_FILES = ".pdf,.png,.jpg,.jpeg";
export const MAX_FILE_SIZE = 10 * 1024 * 1024;
const formats: Record<string, string[]> = {
  "application/pdf": ["pdf"],
  "image/png": ["png"],
  "image/jpeg": ["jpg", "jpeg"],
};
export function validateFile(file: File): string | null {
  if (!file.size) return "O arquivo está vazio. Escolha outro arquivo.";
  if (file.size > MAX_FILE_SIZE) return "O arquivo deve ter até 10 MiB.";
  const ext = file.name.split(".").pop()?.toLowerCase() ?? "";
  if (!formats[file.type]?.includes(ext))
    return "Escolha um PDF, PNG ou JPEG válido. DOCX e TXT ainda não são suportados.";
  return null;
}
