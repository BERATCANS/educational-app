package com.beratcan.app.service;

import com.beratcan.app.exception.FileStorageException;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;

@Slf4j
@Service
@Profile("prod") // Sadece Production ortamında aktif
public class CloudStorageService implements StorageService {

    @Value("${do.spaces.endpoint:}")
    private String endpoint;

    @Value("${do.spaces.key:}")
    private String accessKey;

    @Value("${do.spaces.secret:}")
    private String secretKey;

    @Value("${do.spaces.bucket:}")
    private String bucketName;

    @Value("${do.spaces.region:}")
    private String region;

    @Value("${do.spaces.cdn.endpoint:}")
    private String cdnEndpoint;

    private S3Client s3Client;

    @PostConstruct
    public void validateConfiguration() {
        if (endpoint == null || endpoint.trim().isEmpty() ||
                accessKey == null || accessKey.trim().isEmpty() ||
                secretKey == null || secretKey.trim().isEmpty() ||
                bucketName == null || bucketName.trim().isEmpty() ||
                region == null || region.trim().isEmpty()) {
            log.error("DigitalOcean Spaces configuration eksik! Lütfen .env dosyasına Spaces bilgilerini ekleyin.");
            throw new IllegalStateException(
                    "DigitalOcean Spaces configuration eksik. DO_SPACES_ENDPOINT, DO_SPACES_KEY, DO_SPACES_SECRET, DO_SPACES_BUCKET, DO_SPACES_REGION gerekli.");
        }
        log.info("DigitalOcean Spaces configuration doğrulandı: bucket={}, region={}, endpoint={}", bucketName, region,
                endpoint);
    }

    private S3Client getS3Client() {
        if (s3Client == null) {
            // DigitalOcean Spaces endpoint format: {region}.digitaloceanspaces.com
            String spacesEndpoint = "https://" + endpoint;

            s3Client = S3Client.builder()
                    .endpointOverride(java.net.URI.create(spacesEndpoint))
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)))
                    .build();
        }
        return s3Client;
    }

    @Override
    public String saveFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("Dosya boş olamaz");
        }

        try {
            // Dosya adı oluştur
            String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            String contentType = file.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // S3'e yükle
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filename)
                    .contentType(contentType)
                    .acl(ObjectCannedACL.PUBLIC_READ) // Public read access
                    .build();

            getS3Client().putObject(putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            log.info("Dosya yüklendi (cloud): {}", filename);
            return filename;
        } catch (IOException e) {
            log.error("Dosya yükleme hatası: ", e);
            throw new FileStorageException("Dosya yüklenemedi: " + e.getMessage(), e);
        }
    }

    @Override
    public Resource getFile(String filename) {
        try {
            // URL'den sadece key'i çıkar (eğer full URL ise)
            String key = extractKeyFromUrl(filename);

            // Public URL oluştur (ACL PUBLIC_READ olduğu için direkt erişilebilir)
            String publicUrl = getFileUrl(key);
            Resource resource = new UrlResource(publicUrl);

            // Resource'un var olup olmadığını kontrol et
            if (!resource.exists() || !resource.isReadable()) {
                throw new FileStorageException("Dosya bulunamadı veya okunamıyor: " + key);
            }

            return resource;
        } catch (Exception e) {
            log.error("Dosya okuma hatası: filename={}", filename, e);
            throw new FileStorageException("Dosya okunamadı: " + filename, e);
        }
    }

    @Override
    public void deleteFile(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new FileStorageException("Dosya adı boş olamaz");
        }

        try {
            // URL'den sadece key'i çıkar
            String key = extractKeyFromUrl(filename);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            getS3Client().deleteObject(deleteObjectRequest);
            log.info("Dosya silindi (cloud): {}", key);
        } catch (Exception e) {
            log.error("Dosya silme hatası: filename={}", filename, e);
            throw new FileStorageException("Dosya silinemedi: " + filename, e);
        }
    }

    @Override
    public String getFileUrl(String filename) {
        // URL'den sadece key'i çıkar
        String key = extractKeyFromUrl(filename);

        // CDN endpoint varsa onu kullan, yoksa Spaces public URL
        if (cdnEndpoint != null && !cdnEndpoint.trim().isEmpty()) {
            // CDN endpoint format: https://cdn.example.com
            String cdnUrl = cdnEndpoint.endsWith("/") ? cdnEndpoint : cdnEndpoint + "/";
            return cdnUrl + key;
        } else {
            // Spaces public URL format:
            // https://{bucket}.{region}.digitaloceanspaces.com/{key}
            return "https://" + bucketName + "." + endpoint + "/" + key;
        }
    }

    /**
     * Extract S3 key from URL or filename
     */
    private String extractKeyFromUrl(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new FileStorageException("Dosya adı boş olamaz");
        }

        // Eğer full URL ise, key'i çıkar
        if (filename.startsWith("http://") || filename.startsWith("https://")) {
            // URL format: https://bucket.endpoint/key veya https://cdn.endpoint/key
            int lastSlashIndex = filename.lastIndexOf('/');
            if (lastSlashIndex >= 0 && lastSlashIndex < filename.length() - 1) {
                return filename.substring(lastSlashIndex + 1);
            }
        }

        // Eğer /api/files/ prefix'i varsa çıkar
        if (filename.startsWith("/api/files/")) {
            return filename.substring("/api/files/".length());
        } else if (filename.startsWith("api/files/")) {
            return filename.substring("api/files/".length());
        }

        // Direkt key olarak döndür
        return filename;
    }
}
