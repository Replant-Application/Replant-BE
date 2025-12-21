package com.app.replant.service.s3FileService;

import com.app.replant.controller.dto.UploadedFileInfoDto;
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

    public S3FileServiceImpl(
            @Value("${aws.s3.accessKey}") String accessKey,
            @Value("${aws.s3.secretKey}") String secretKey,
            @Value("${aws.s3.region}") String region) {
        log.info("S3FileService()");

        // AWS 자격증명 설정
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);

        // S3 클라이언트 설정
        s3Client = S3Client.builder()
                .region(Region.of(region))  // region 설정
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .build();


    }

    // 파일 업로드
    public UploadedFileInfoDto uploadImage(MultipartFile multipartFile) throws IOException {
        log.info("uploadImage()");

        // 파일 확장자 포함한 고유 이름 생성
        UUID uuid = UUID.randomUUID();
        String uniqueFileName = uuid.toString().replaceAll("-", "");

        String fileOriName = multipartFile.getOriginalFilename();
        String fileExtension = fileOriName.substring(fileOriName.lastIndexOf(".")); // 확장자 추출

        String fileName = uniqueFileName + fileExtension;

            // PutObjectRequest로 S3에 파일 업로드
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .acl(ObjectCannedACL.PUBLIC_READ)  // 공개 읽기 권한
                    .contentType(multipartFile.getContentType())
                    .build();

            // 파일 업로드
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(multipartFile.getInputStream(), multipartFile.getSize()));

            // 업로드한 파일의 URL
            String fileUrl = s3Client.utilities().getUrl(GetUrlRequest.builder().bucket(bucket).key(fileName).build()).toString();

            // 업로드한 파일의 URL 과 Name 반환
            return UploadedFileInfoDto.builder()
                    .fileName(fileName)
                    .fileUrl(fileUrl)
                    .fileSize(multipartFile.getSize())
                    .contentType(multipartFile.getContentType())
                    .build();

    }

    // 파일 삭제
    public boolean deleteImage(String fileName) {
        log.info("deleteImage()");

        try {
            // S3에서 객체 삭제
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

    // 파일 다운로드
    public byte[] downloadImage(String fileName) {
        log.info("downloadImage()");

        try {
            // S3에서 객체를 가져오기 위한 GetObjectRequest
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .build();

            // S3에서 파일을 다운로드 (ResponseInputStream으로 반환됨)
            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(getObjectRequest);

            // 파일 내용을 ByteArray로 읽어들입니다.
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
