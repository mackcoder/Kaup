package com.kaup.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.kaup.exception.ContratoInvalidoException;
import com.kaup.exception.ServicoExternoException;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

/**
 * Extrai o texto de um contrato (imagem ou PDF) com OCR local (Tesseract).
 * O arquivo nunca sai do servidor.
 */
@Service
public class OcrService {

    private static final Logger log = LoggerFactory.getLogger(OcrService.class);

    /** PDF com pelo menos este tanto de texto embutido é tratado como digital (sem OCR). */
    private static final int MIN_CARACTERES_PDF_DIGITAL = 100;
    private static final int DPI = 300;

    private final Tesseract tesseract;
    private final int maxPaginas;

    public OcrService(Tesseract tesseract, @Value("${ocr.max-paginas}") int maxPaginas) {
        this.tesseract = tesseract;
        this.maxPaginas = maxPaginas;
    }

    public String extrairTexto(MultipartFile arquivo) throws IOException {
        byte[] bytes = arquivo.getBytes();
        if ("application/pdf".equals(arquivo.getContentType())) {
            return extrairDePdf(bytes);
        }
        return extrairDeImagem(bytes);
    }

    private String extrairDePdf(byte[] bytes) {
        try (PDDocument documento = Loader.loadPDF(bytes)) {
            String digital = new PDFTextStripper().getText(documento).trim();
            if (digital.length() >= MIN_CARACTERES_PDF_DIGITAL) {
                return digital;
            }

            int paginas = documento.getNumberOfPages();
            if (paginas > maxPaginas) {
                throw new ContratoInvalidoException(
                        "O PDF tem " + paginas + " páginas. O máximo permitido é " + maxPaginas + ".");
            }

            PDFRenderer renderizador = new PDFRenderer(documento);
            StringBuilder texto = new StringBuilder();
            for (int i = 0; i < paginas; i++) {
                BufferedImage pagina = renderizador.renderImageWithDPI(i, DPI, ImageType.GRAY);
                texto.append(reconhecer(pagina)).append("\n\n");
            }
            return texto.toString().trim();
        } catch (InvalidPasswordException e) {
            throw new ContratoInvalidoException("O PDF está protegido por senha.");
        } catch (IOException e) {
            log.warn("Não foi possível ler o PDF enviado", e);
            throw new ContratoInvalidoException("Não foi possível ler o PDF. O arquivo pode estar corrompido.");
        }
    }

    private String extrairDeImagem(byte[] bytes) throws IOException {
        BufferedImage imagem = ImageIO.read(new ByteArrayInputStream(bytes));
        if (imagem == null) {
            throw new ContratoInvalidoException("Não foi possível ler a imagem enviada.");
        }
        return reconhecer(imagem).trim();
    }

    private String reconhecer(BufferedImage imagem) {
        try {
            // A instância do Tesseract não é thread-safe: uma página por vez.
            synchronized (tesseract) {
                return tesseract.doOCR(imagem);
            }
        } catch (TesseractException e) {
            log.error("Falha no OCR local (Tesseract)", e);
            throw new ServicoExternoException("OCR", e);
        }
    }
}
