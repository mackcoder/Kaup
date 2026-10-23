package com.kaup.common.error;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kaup.common.web.RequestIdFilter;

@RestController
public class ApiErrorController implements ErrorController {
    @RequestMapping("/error")
    public ResponseEntity<ApiError> error(HttpServletRequest request) {
        Object value = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        HttpStatus status = value instanceof Integer code ? HttpStatus.resolve(code) : null;
        if (status == null || !status.isError()) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String requestId = RequestIdFilter.requestId(request);
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON)
                .header(RequestIdFilter.HEADER, requestId)
                .body(ApiError.forStatus(status, requestId));
    }
}
