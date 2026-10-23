package com.kaup.common.error;

import org.springframework.http.HttpStatusCode;

public record ApiError(String code, String message, String requestId) {
    public static ApiError forStatus(HttpStatusCode status, String requestId) {
        return switch (status.value()) {
            case 400 -> new ApiError("VALIDATION_ERROR", "Verifique os dados enviados.", requestId);
            case 401 -> new ApiError("UNAUTHORIZED", "Entre na sua conta para continuar.", requestId);
            case 403 -> new ApiError("FORBIDDEN", "Acesso não permitido.", requestId);
            case 404 -> new ApiError("NOT_FOUND", "Recurso não encontrado.", requestId);
            case 405 -> new ApiError("METHOD_NOT_ALLOWED", "Método HTTP não permitido.", requestId);
            case 406 -> new ApiError("NOT_ACCEPTABLE", "Formato de resposta não disponível.", requestId);
            case 409 -> new ApiError("CONFLICT", "A operação conflita com o estado atual.", requestId);
            case 413 -> new ApiError("FILE_TOO_LARGE", "O arquivo excede o tamanho permitido.", requestId);
            case 415 -> new ApiError("UNSUPPORTED_MEDIA_TYPE", "Formato de envio não suportado.", requestId);
            case 422 -> new ApiError("UNPROCESSABLE_CONTENT", "Não foi possível processar os dados enviados.", requestId);
            case 429 -> new ApiError("RATE_LIMITED", "Muitas solicitações. Tente novamente mais tarde.", requestId);
            case 503 -> new ApiError("SERVICE_UNAVAILABLE", "Serviço temporariamente indisponível.", requestId);
            default -> new ApiError("INTERNAL_ERROR", "Não foi possível concluir a operação.", requestId);
        };
    }
}
