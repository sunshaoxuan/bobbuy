package com.bobbuy.service;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetBucketPolicyArgs;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ImageStorageService {
    private static final Logger log = LoggerFactory.getLogger(ImageStorageService.class);

    @Value("${bobbuy.minio.endpoint}")
    private String endpoint;

    @Value("${bobbuy.minio.accessKey}")
    private String accessKey;

    @Value("${bobbuy.minio.secretKey}")
    private String secretKey;

    @Value("${bobbuy.minio.bucket}")
    private String bucket;

    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        try {
            minioClient = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();

            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Created MinIO bucket: {}", bucket);
                
                // Set public read policy for the bucket to allow Direct URL access
                String policy = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:GetBucketLocation\",\"s3:ListBucket\"],\"Resource\":[\"arn:aws:s3:::" + bucket + "\"]},{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:GetObject\"],\"Resource\":[\"arn:aws:s3:::" + bucket + "/*\"]}]}";
                minioClient.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucket).config(policy).build());
            }
        } catch (Exception e) {
            log.error("Failed to initialize MinIO client or bucket", e);
        }
    }

    public String saveBase64(String base64Content) {
        if (base64Content == null || base64Content.isEmpty()) {
            return null;
        }

        String pureBase64 = base64Content.contains(",") ? 
                            base64Content.substring(base64Content.indexOf(",") + 1) : 
                            base64Content;

        try {
            byte[] bytes = Base64.getDecoder().decode(pureBase64);
            String filename = UUID.randomUUID().toString() + ".jpg";
            
            try (InputStream is = new ByteArrayInputStream(bytes)) {
                minioClient.putObject(
                    PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(filename)
                        .stream(is, bytes.length, -1)
                        .contentType("image/jpeg")
                        .build()
                );
            }

            log.info("Saved original photo to MinIO: {}/{}", bucket, filename);
            
            // Return the direct MinIO URL (since we set the bucket to public read)
            // In production, you might want to use a proxy URL or cloudfront
            return endpoint + "/" + bucket + "/" + filename;
        } catch (Exception e) {
            log.error("Failed to save base64 image to MinIO", e);
            return null;
        }
    }
}
