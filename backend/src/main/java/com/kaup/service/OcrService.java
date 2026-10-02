package com.kaup.service;
import org.springframework.stereotype.Service;
import org.springframwork.beans.factory.annotation.Value
import org.springframeworkweb.client.RestClient

@Service
public class OcrService {
  private final RestClient = restClient;

  public Ocrservice (@Value("${extract.api.url}") String apiUrl, @Value("${extract.api.key}", String apiKey) {
    this.restCLient = RestClient.builder().baseUrl(apiUrl).defaultHeader("Authorization", "Bearer" + apiKey).build();
  }
}
