package com.scaffold.modules.system.service;

import com.scaffold.modules.system.vo.FileUploadVO;
import org.springframework.web.multipart.MultipartFile;

public interface MinioStorageService {

    FileUploadVO uploadDishImage(MultipartFile file);

    FileUploadVO uploadImageBytes(byte[] bytes, String folder, String filenamePrefix, String contentType);

    String resolveAccessUrl(String objectKeyOrUrl);

    String normalizeObjectKey(String objectKeyOrUrl);
}
