package com.app.replant.global.infrastructure.service.s3FileService;

import com.app.replant.global.dto.UploadedFileInfoDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
@Slf4j
public class S3FileServiceImpl implements S3FileService {
    final private S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.cloudfront-domain}")
    private String cloudfrontDomain;

    public S3FileServiceImpl(
            @Value("${aws.s3.accessKey}") String accessKey,
            @Value("${aws.s3.secretKey}") String secretKey,
            @Value("${aws.s3.region}") String region) {
        log.info("S3FileService()");

        // AWS ?�격증명 ?�정
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);

        // S3 ?�라?�언???�정
        s3Client = S3Client.builder()
                .region(Region.of(region))  // region ?�정
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .build();


    }

    // ?�일 ?�로??
    public UploadedFileInfoDto uploadImage(MultipartFile multipartFile) throws IOException {
        return uploadImageToFolder(multipartFile, null);
    }

    // 폴더에 파일 업로드
    public UploadedFileInfoDto uploadImageToFolder(MultipartFile multipartFile, String folder) throws IOException {
        log.info("uploadImageToFolder() folder: {}", folder);

        // 파일 확장자를 포함한 고유 이름 생성
        UUID uuid = UUID.randomUUID();
        String uniqueFileName = uuid.toString().replaceAll("-", "");

        String fileOriName = multipartFile.getOriginalFilename();
        String fileExtension = "";

        // null 체크 및 확장자 추출
        if (fileOriName != null && fileOriName.contains(".")) {
            fileExtension = fileOriName.substring(fileOriName.lastIndexOf("."));
        } else {
            // 확장자가 없으면 Content-Type에서 추론
            String contentType = multipartFile.getContentType();
            if (contentType != null) {
                if (contentType.contains("jpeg") || contentType.contains("jpg")) {
                    fileExtension = ".jpg";
                } else if (contentType.contains("png")) {
                    fileExtension = ".png";
                } else if (contentType.contains("gif")) {
                    fileExtension = ".gif";
                } else if (contentType.contains("webp")) {
                    fileExtension = ".webp";
                }
            }
        }

        // 폴더가 있으면 폴더 경로 추가
        String fileName;
        if (folder != null && !folder.isEmpty()) {
            fileName = folder + "/" + uniqueFileName + fileExtension;
        } else {
            fileName = uniqueFileName + fileExtension;
        }

        // PutObjectRequest로 S3에 파일 업로드 (ACL 제거 - 최신 S3 정책 호환)
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .contentType(multipartFile.getContentType())
                .build();

        // ?�일 ?�로??
        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(multipartFile.getInputStream(), multipartFile.getSize()));

        // CloudFront 도메인을 사용한 URL 생성
        String fileUrl = "https://" + cloudfrontDomain + "/" + fileName;

        // ?�로?�한 ?�일??URL �?Name 반환
        return UploadedFileInfoDto.builder()
                .fileName(fileName)
                .fileUrl(fileUrl)
                .fileSize(multipartFile.getSize())
                .contentType(multipartFile.getContentType())
                .build();
    }

    // ?�일 ??��
    public boolean deleteImage(String fileName) {
        log.info("deleteImage()");

        try {
            // S3?�서 객체 ??��
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .build();
            s3Client.deleteObject(deleteObjectRequest);
            return true;

        } catch (Exception e) {
            e.printStackTrace();

        }

        return false;

    }

    // ?�일 ?�운로드
    public byte[] downloadImage(String fileName) {
        log.info("downloadImage()");

        try {
            // S3?�서 객체�?가?�오�??�한 GetObjectRequest
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .build();

            // S3?�서 ?�일???�운로드 (ResponseInputStream?�로 반환??
            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(getObjectRequest);

            // ?�일 ?�용??ByteArray�??�어?�입?�다.
            InputStream inputStream = response;
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, length);
            }

            inputStream.close();
            byteArrayOutputStream.close();

            return byteArrayOutputStream.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();

        }

        return null;

    }
}
