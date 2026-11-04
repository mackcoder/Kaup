package com.kaup.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.kaup.exception.ContratoInvalidoException;

import net.sourceforge.tess4j.Tesseract;

class OcrServiceTest {

    private Tesseract tesseract;
    private OcrService ocr;

    @BeforeEach
    void preparar() throws Exception {
        tesseract = mock(Tesseract.class);
        when(tesseract.doOCR(any(BufferedImage.class))).thenReturn("texto do contrato");
        ocr = new OcrService(tesseract, 3, 10_000);
    }

    private static MockMultipartFile imagem(int largura, int altura) throws Exception {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB), "png", saida);
        return new MockMultipartFile("file", "a.png", "image/png", saida.toByteArray());
    }

    private static MockMultipartFile pdfDigital(int paginas, PDRectangle tamanho) throws Exception {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            for (int i = 0; i < paginas; i++) {
                PDPage pagina = new PDPage(tamanho);
                doc.addPage(pagina);
                try (PDPageContentStream c = new PDPageContentStream(doc, pagina)) {
                    c.beginText();
                    c.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                    c.newLineAtOffset(10, 100);
                    c.showText("Clausula primeira: o locatario pagara o aluguel todo mes, ate o dia cinco, "
                            + "sob pena de multa de dez por cento sobre o valor devido.");
                    c.endText();
                }
            }
            doc.save(saida);
            return new MockMultipartFile("file", "a.pdf", "application/pdf", saida.toByteArray());
        }
    }

    @Test
    void imagemDentroDoLimiteEhLida() throws Exception {
        assertEquals("texto do contrato", ocr.extrairTexto(imagem(50, 50)));
    }

    @Test
    void imagemComPixelsDemaisEhRecusadaSemIrAoTesseract() throws Exception {
        assertThrows(ContratoInvalidoException.class, () -> ocr.extrairTexto(imagem(200, 200)));
        verifyNoInteractions(tesseract);
    }

    @Test
    void arquivoQueNaoEhImagemEhRecusado() {
        var lixo = new MockMultipartFile("file", "a.png", "image/png", "nao sou imagem".getBytes());
        assertThrows(ContratoInvalidoException.class, () -> ocr.extrairTexto(lixo));
    }

    @Test
    void pdfDigitalComPaginasDemaisEhRecusadoAntesDeExtrairTexto() throws Exception {
        var e = assertThrows(ContratoInvalidoException.class,
                () -> ocr.extrairTexto(pdfDigital(4, PDRectangle.A4)));
        assertTrue(e.getMessage().contains("4 páginas"));
    }

    @Test
    void pdfDigitalDentroDoLimiteDevolveOTextoEmbutido() throws Exception {
        String texto = ocr.extrairTexto(pdfDigital(3, PDRectangle.A4));
        assertTrue(texto.contains("Clausula primeira"));
        verifyNoInteractions(tesseract);
    }
}
