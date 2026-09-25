package com.kaup.config;

import net.sourceforge.tess4j.Tesseract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Tess4jConfig {

    @Bean
    public Tesseract tesseract() {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath("/usr/share/tessdata"); // Caminho padrão no Linux/Docker Alpine
        tesseract.setLanguage("por"); // Configura idioma Português
        return tesseract;
    }
}