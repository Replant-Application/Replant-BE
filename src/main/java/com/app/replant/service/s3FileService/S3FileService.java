package com.app.replant.service.s3FileService;

import com.app.replant.controller.dto.UploadedFileInfoDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface S3FileService {

    public UploadedFileInfoDto uploadImage(MultipartFile multipartFile) throws IOException ;

    // 폴더에 파일 업로드
    public UploadedFileInfoDto uploadImageToFolder(MultipartFile multipartFile, String folder) throws IOException;

    // 파일 삭제
    public boolean deleteImage(String fileName) ;
    // 파일 다운로드
    public byte[] downloadImage(String fileName);
}
