package com.kaup.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
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
    private final long maxPixels;

    public OcrService(Tesseract tesseract, @Value("${ocr.max-paginas}") int maxPaginas,
                      @Value("${ocr.max-pixels}") long maxPixels) {
        this.tesseract = tesseract;
        this.maxPaginas = maxPaginas;
        this.maxPixels = maxPixels;
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
            // O limite vale antes de qualquer extração: um PDF digital enorme também custa caro.
            int paginas = documento.getNumberOfPages();
            if (paginas > maxPaginas) {
                throw new ContratoInvalidoException(
                        "O PDF tem " + paginas + " páginas. O máximo permitido é " + maxPaginas + ".");
            }

            String digital = new PDFTextStripper().getText(documento).trim();
            if (digital.length() >= MIN_CARACTERES_PDF_DIGITAL) {
                return digital;
            }

            // Página gigante (ex.: 200 m de lado) viraria uma imagem de bilhões de pixels.
            for (int i = 0; i < paginas; i++) {
                PDRectangle caixa = documento.getPage(i).getMediaBox();
                double pixels = (double) caixa.getWidth() / 72 * DPI * caixa.getHeight() / 72 * DPI;
                if (pixels > maxPixels) {
                    throw new ContratoInvalidoException("As páginas do PDF são grandes demais para ler.");
                }
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
        BufferedImage imagem = lerImagem(bytes);
        return reconhecer(imagem).trim();
    }

    // Lê só o cabeçalho para saber o tamanho: decodificar antes de medir deixaria uma imagem
    // pequena em bytes e enorme em pixels estourar a memória.
    private BufferedImage lerImagem(byte[] bytes) throws IOException {
        try (ImageInputStream entrada = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> leitores = entrada == null ? null : ImageIO.getImageReaders(entrada);
            if (leitores == null || !leitores.hasNext()) {
                throw new ContratoInvalidoException("Não foi possível ler a imagem enviada.");
            }
            ImageReader leitor = leitores.next();
            try {
                leitor.setInput(entrada);
                if ((long) leitor.getWidth(0) * leitor.getHeight(0) > maxPixels) {
                    throw new ContratoInvalidoException("A imagem é grande demais. Envie uma foto de até "
                            + maxPixels / 1_000_000 + " megapixels.");
                }
                return leitor.read(0);
            } catch (IOException e) {
                throw new ContratoInvalidoException("Não foi possível ler a imagem enviada.");
            } finally {
                leitor.dispose();
            }
        }
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
