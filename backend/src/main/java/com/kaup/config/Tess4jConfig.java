package com.kaup.config;

import net.sourceforge.tess4j.Tesseract;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Tess4jConfig {

    @Bean
    public Tesseract tesseract(@Value("${ocr.tessdata-path}") String tessdataPath, @Value("${ocr.language}") String language) {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessdataPath); // Caminho padrão no Linux/Docker Alpine
        tesseract.setLanguage(language); // Configura idioma Português
        return tesseract;
    }
}
