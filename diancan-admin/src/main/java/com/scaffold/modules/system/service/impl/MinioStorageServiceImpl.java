package com.scaffold.modules.system.service.impl;

import com.scaffold.common.exception.BusinessException;
import com.scaffold.common.result.ResultCode;
import com.scaffold.modules.system.service.MinioStorageService;
import com.scaffold.modules.system.vo.FileUploadVO;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MinioStorageServiceImpl implements MinioStorageService {

    @Value("${storage.minio.enabled:false}")
    private boolean enabled;

    @Value("${storage.minio.endpoint:}")
    private String endpoint;

    @Value("${storage.minio.access-key:}")
    private String accessKey;

    @Value("${storage.minio.secret-key:}")
    private String secretKey;

    @Value("${storage.minio.bucket:dish-images}")
    private String bucket;

    @Value("${storage.minio.public-url:}")
    private String publicUrl;

    @Value("${storage.minio.auto-create-bucket:true}")
    private boolean autoCreateBucket;

    @Value("${storage.minio.max-file-size-mb:5}")
    private int maxFileSizeMb;

    @Value("${storage.minio.private-bucket:true}")
    private boolean privateBucket;

    @Value("${storage.minio.presign-expire-seconds:3600}")
    private int presignExpireSeconds;

    @Override
    public FileUploadVO uploadDishImage(MultipartFile file) {
        if (!enabled) {
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED, "MinIO 未启用，请先配置 storage.minio.enabled=true");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请选择图片文件");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "仅支持图片文件上传");
        }

        long maxBytes = (long) maxFileSizeMb * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "图片大小不能超过 " + maxFileSizeMb + "MB");
        }

        if (isBlank(endpoint) || isBlank(accessKey) || isBlank(secretKey) || isBlank(bucket)) {
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED, "MinIO 配置不完整");
        }

        String objectName = buildObjectName(file.getOriginalFilename());
        try {
            MinioClient client = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();

            ensureBucket(client, bucket);

            try (InputStream inputStream = file.getInputStream()) {
                client.putObject(PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .stream(inputStream, file.getSize(), -1)
                        .contentType(contentType)
                        .build());
            }

            FileUploadVO vo = new FileUploadVO();
            vo.setObjectName(objectName);
            vo.setUrl(resolveAccessUrl(objectName));
            return vo;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("MinIO 上传失败: fileName={}, err={}", file.getOriginalFilename(), e.getMessage(), e);
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED, "图片上传失败");
        }
    }

    @Override
    public FileUploadVO uploadImageBytes(byte[] bytes, String folder, String filenamePrefix, String contentType) {
        if (!enabled) {
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED, "MinIO 未启用，请先配置 storage.minio.enabled=true");
        }
        if (bytes == null || bytes.length == 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "上传内容不能为空");
        }
        if (isBlank(contentType)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "文件类型不能为空");
        }
        if (isBlank(endpoint) || isBlank(accessKey) || isBlank(secretKey) || isBlank(bucket)) {
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED, "MinIO 配置不完整");
        }

        String safeFolder = isBlank(folder) ? "misc" : folder.trim().replaceAll("^/+|/+$", "");
        String safePrefix = isBlank(filenamePrefix) ? "file" : filenamePrefix.trim().replaceAll("[^a-zA-Z0-9_-]", "_");
        String ext = inferExtension(contentType);
        String objectName = safeFolder + "/" + safePrefix + "-" + UUID.randomUUID().toString().replace("-", "") + ext;

        try {
            MinioClient client = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();

            ensureBucket(client, bucket);

            try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
                client.putObject(PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .stream(inputStream, bytes.length, -1)
                        .contentType(contentType)
                        .build());
            }

            FileUploadVO vo = new FileUploadVO();
            vo.setObjectName(objectName);
            vo.setUrl(resolveAccessUrl(objectName));
            return vo;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("MinIO 字节上传失败: objectName={}, err={}", objectName, e.getMessage(), e);
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED, "文件上传失败");
        }
    }

    private void ensureBucket(MinioClient client, String targetBucket) throws Exception {
        boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(targetBucket).build());
        if (exists) return;
        if (!autoCreateBucket) {
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED, "存储桶不存在: " + targetBucket);
        }
        client.makeBucket(MakeBucketArgs.builder().bucket(targetBucket).build());
    }

    private String buildObjectName(String originalFilename) {
        String ext = "";
        if (originalFilename != null) {
            int index = originalFilename.lastIndexOf('.');
            if (index >= 0 && index < originalFilename.length() - 1) {
                ext = "." + originalFilename.substring(index + 1).toLowerCase(Locale.ROOT);
            }
        }
        return "dish/" + UUID.randomUUID().toString().replace("-", "") + ext;
    }

    private String buildFileUrl(String objectName) {
        String base = isBlank(publicUrl) ? endpoint : publicUrl;
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/" + bucket + "/" + objectName;
    }

    @Override
    public String resolveAccessUrl(String objectKeyOrUrl) {
        if (isBlank(objectKeyOrUrl)) {
            return objectKeyOrUrl;
        }
        String objectKey = normalizeObjectKey(objectKeyOrUrl);
        // 兼容历史外部 URL（无法提取对象键）
        if (isHttpUrl(objectKey) && !isObjectKeyUnderBucket(objectKey)) {
            return objectKey;
        }

        String resolvedKey = objectKey;
        if (isHttpUrl(objectKey)) {
            resolvedKey = extractObjectKeyFromUrl(objectKey);
        }
        if (isBlank(resolvedKey)) {
            return objectKeyOrUrl;
        }
        if (!enabled) {
            return buildFileUrl(resolvedKey);
        }
        if (!privateBucket) {
            return buildFileUrl(resolvedKey);
        }
        try {
            MinioClient client = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();
            int expire = Math.max(60, presignExpireSeconds);
            return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucket)
                    .object(resolvedKey)
                    .method(Method.GET)
                    .expiry(expire, TimeUnit.SECONDS)
                    .build());
        } catch (Exception e) {
            log.warn("生成预签名URL失败，降级为直链: key={}, err={}", resolvedKey, e.getMessage());
            return buildFileUrl(resolvedKey);
        }
    }

    @Override
    public String normalizeObjectKey(String objectKeyOrUrl) {
        if (isBlank(objectKeyOrUrl)) {
            return objectKeyOrUrl;
        }
        if (!isHttpUrl(objectKeyOrUrl)) {
            // 兼容历史数据：对象键可能带前导斜杠，如 "/dish/xxx.jpg"
            return objectKeyOrUrl.replaceFirst("^/+", "");
        }
        String objectKey = extractObjectKeyFromUrl(objectKeyOrUrl);
        return isBlank(objectKey) ? objectKeyOrUrl : objectKey;
    }

    private String extractObjectKeyFromUrl(String fullUrl) {
        try {
            URI uri = URI.create(fullUrl);
            String path = uri.getPath();
            if (isBlank(path)) return null;
            String bucketPrefix = "/" + bucket + "/";
            int idx = path.indexOf(bucketPrefix);
            if (idx < 0) return null;
            return path.substring(idx + bucketPrefix.length());
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isHttpUrl(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private boolean isObjectKeyUnderBucket(String value) {
        if (!isHttpUrl(value)) return true;
        return extractObjectKeyFromUrl(value) != null;
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    private String inferExtension(String contentType) {
        String type = contentType.toLowerCase(Locale.ROOT);
        if ("image/png".equals(type)) return ".png";
        if ("image/jpeg".equals(type) || "image/jpg".equals(type)) return ".jpg";
        if ("image/webp".equals(type)) return ".webp";
        return ".bin";
    }
}
