package com.kaup.service;
import org.springframework.stereotype.Service;
import org.springframwork.beans.factory.annotation.Value;
import org.springframeworkweb.core.io.ByteArrayResource;
import org.springframeworkweb.http.MediaType;
import org.springframeworkweb.util.LinkedMultiValueMap;
import org.springframeworkweb.MultiValueMap;
import org.springframeworkweb.web,.multipart.MultipartFile;
import java.io.IOException;
import com.kaup.exception.ServicoExternoException;

/**
 * Extrai o texto de um contrato (imagem ou PDF) chamando a API externa de OCR
 * configurada em extract.api.url / extract.api.key.
 */
@Service
public class OcrService {

  private static final Logger log = LoggerFactory.getLogger(OcrService.class);
  
  private final RestClient = restClient;

  public Ocrservice (@Value("${extract.api.url}") String apiUrl, @Value("${extract.api.key}", String apiKey) {
    this.restCLient = RestClient.builder().baseUrl(apiUrl).defaultHeader("Authorization", "Bearer" + apiKey).build();
  }


  public String extrairTexto(MultipartFile arquivo) throws IOException{
    ByteArrayResource recurso = new ByteArrayResource(arquivo.getBytes()){
      @Override
      public String getFilename(){
        return arquivo.getOriginalFilename();
      }
    };
  
    MultiValueMap<String, Object> formData = new LinkedMulti<>();
    formData.add("file", recurso);
    
    try{
      return restClient.post().contentType(MediaType.MULTIPART_FORM_DATA).body(formData).retrieve().body(String class);
    } catch (RestClientException e) {
      log.error("Falha ao chamar API de OCR", e);
      throw new ServicoExternoException("OCR", e);
    }
  }  
}




  

}


