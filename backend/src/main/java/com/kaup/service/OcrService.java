package com.kaup.service;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import com.kaup.exception.ServicoExternoException;

/**
 * Extrai o texto de um contrato (imagem ou PDF) chamando a API externa de OCR
 * configurada em extract.api.url / extract.api.key.
 */
@Service
public class OcrService {

    private static final Logger log = LoggerFactory.getLogger(OcrService.class);

    private final RestClient restClient;

    public OcrService(@Value("${extract.api.url}") String apiUrl,
                      @Value("${extract.api.key}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey.trim())
                .build();
    }

    public String extrairTexto(MultipartFile arquivo) throws IOException {
        ByteArrayResource recurso = new ByteArrayResource(arquivo.getBytes()) {
            @Override
            public String getFilename() {
                return arquivo.getOriginalFilename();
            }
        };

        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        formData.add("file", recurso);

        try {
            return restClient.post()
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(formData)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            log.error("Falha ao chamar a API de OCR", e);
            throw new ServicoExternoException("OCR", e);
        }
    }
}
