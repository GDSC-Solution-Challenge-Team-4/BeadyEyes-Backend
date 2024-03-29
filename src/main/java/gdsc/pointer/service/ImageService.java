package gdsc.pointer.service;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.*;
import gdsc.pointer.dto.request.image.ImageUrlDto;
import gdsc.pointer.dto.request.image.PointerAIDto;
import gdsc.pointer.dto.request.image.PointerDto;
import gdsc.pointer.dto.response.image.PolyResponseDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ImageService {

    @Value("${spring.cloud.gcp.storage.bucket}")
    private String bucketName;

    @Value("${AI_SERVER_URL}")
    private String aiServerUrl;

    private final Storage storage;


    public String uploadImage(MultipartFile file) throws IOException {

        String uuid = UUID.randomUUID().toString(); // Google Cloud Storage에 저장될 파일 이름
        String ext = file.getContentType(); // 파일의 형식 ex) JPG

        // Cloud에 이미지 업로드
        BlobInfo blobInfo = storage.create(
                BlobInfo.newBuilder(bucketName, uuid)
                        .setContentType(ext)
                        .build(),
                file.getInputStream()
        );

        return "https://storage.googleapis.com/" + bucketName + "/" + uuid;
    }

    public String toText(MultipartFile file) throws IOException {

        // GCS 사진 업로드 후, 공개 이미지 url 반환
        String image_url = uploadImage(file);

        // RestTemplate 사용하여 AI Server로 통신
        // 사진 url 전송 -> 텍스트 반환
        // POST 요청
        ResponseEntity<String> responseEntity = postWithImageUrl(image_url);
        log.info("responseEntity.getBody() = {}", responseEntity.getBody());

        String postResult = responseEntity.getBody();

        // 문자열 파싱
        String parsedResult = textParse(postResult);

        return parsedResult;
    }

    private ResponseEntity<String> postWithImageUrl(String url) {
        URI uri = UriComponentsBuilder
                .fromUriString(aiServerUrl)
                .path("/image/toText")
                .encode()
                .build()
                .toUri();

        ImageUrlDto imageUrlDto = new ImageUrlDto();
        imageUrlDto.setImageUrl(url);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(
                uri, imageUrlDto, String.class
        );

        return responseEntity;
    }

    private String textParse(String text) {
        // Replace "\\" with an empty string
        String parsedText = text.replace("\\", "");

        // Replace "\n" with an empty string
        parsedText = parsedText.replace("\n", "");

        return parsedText;
    }

    public PolyResponseDto getImageTextBoundingPoly(MultipartFile file) throws IOException {

        // GCS 사진 업로드 후, 공개 이미지 url 반환
        String image_url = uploadImage(file);

        ResponseEntity<PolyResponseDto> responseDto = postImageBoundingPoly(image_url);
        return responseDto.getBody();
    }

    public String getWordWithPointer(PointerDto dto) throws IOException {

        // GCS 사진 업로드 후, 공개 이미지 url 반환
        String image_url = uploadImage(dto.getImage());

        //String image_url = dto.getImageUrl();
        ResponseEntity<String> response = postPointer(image_url);
        String parsedText = textParse(response.getBody());
        return parsedText;
    }

    private ResponseEntity<String>  postPointer(String url) {

        URI uri = UriComponentsBuilder
                .fromUriString(aiServerUrl)
                .path("/image/pointer")
                .encode()
                .build()
                .toUri();

        PointerAIDto pointerAIDto = new PointerAIDto();
        pointerAIDto.setImageUrl(url);


        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> responseEntity = restTemplate.postForEntity(
                uri, pointerAIDto, String.class
        );
        System.out.println(responseEntity);

        return responseEntity;

    }

    private ResponseEntity<PolyResponseDto> postImageBoundingPoly(String url) {
        URI uri = UriComponentsBuilder
                .fromUriString(aiServerUrl)
                .path("/image/boundingPoly")
                .encode()
                .build()
                .toUri();

        ImageUrlDto imageUrlDto = new ImageUrlDto();
        imageUrlDto.setImageUrl(url);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<PolyResponseDto> responseEntity = restTemplate.postForEntity(
                uri, imageUrlDto, PolyResponseDto.class
        );
        System.out.println(responseEntity);

        return responseEntity;
    }



}
