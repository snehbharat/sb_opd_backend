package com.sbpl.OPD.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.utils.IoUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

/**
 * Utility class for performing operations on AWS S3 bucket like
 * uploading, checking existence, deleting, and downloading files.
 *
 * @author Rahul Kumar
 */
@Component
public class S3BucketStorageUtility {

  private static final Logger logger = LoggerFactory.getLogger(S3BucketStorageUtility.class);

  @Value("${aws.s3.bucket-name}")
  private String bucketName;

  @Value("${aws.region}")
  private String region;

  private final S3Client s3Client;

  public S3BucketStorageUtility(S3Client s3Client) {
    this.s3Client = s3Client;
  }

  /**
   * Uploads a file to AWS S3 bucket.
   *
   * @param fileName    Name of the file (key) in the bucket.
   * @param content     Byte array of file content.
   * @param contentType MIME type of the file.
   * @return Public URL of the uploaded file.
   */
  public String uploadFile(String fileName, byte[] content, String contentType) {
    try {
      PutObjectRequest putObjectRequest = PutObjectRequest.builder()
          .bucket(bucketName)
          .key(fileName)
          .contentType(contentType)
          .build();

      s3Client.putObject(putObjectRequest, RequestBody.fromBytes(content));

      String fileUrl = String.format("""
          https://%s.s3.%s.amazonaws.com/%s""", bucketName, region, fileName);
      logger.info("File uploaded to S3: {}", fileUrl);
      return fileUrl;
    } catch (Exception e) {
      logger.error("Failed to upload file to S3: {}", fileName, e);
      return null;
    }
  }

  /**
   * Uploads a file to AWS S3 bucket with a unique filename.
   *
   * @param originalFileName Original name of the file
   * @param content          Byte array of file content
   * @param contentType      MIME type of the file
   * @return Public URL of the uploaded file with unique name
   */
  public String uploadFileWithUniqueName(String originalFileName, byte[] content, String contentType ,String uhid,Long appointmentId) {
    String extension = "";
    if (originalFileName.contains(".")) {
      extension = originalFileName.substring(originalFileName.lastIndexOf("."));
    }
      String date = LocalDate.now()
              .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

      String uniqueFileName = "MEDREC_"
              + uhid + "_"
              + appointmentId + "_"
              + date + "_"
              + UUID.randomUUID()
              + extension;
//      String uniqueFileName = UUID.randomUUID().toString() + extension;
    return uploadFile(uniqueFileName, content, contentType);
  }

    public String uploadFileWithNonMedRec(String originalFileName, byte[] content, String contentType ,String uhid,Long appointmentId) {
        String extension = "";
        if (originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String date = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        String uniqueFileName = "NON_MEDREC_"
                + uhid + "_"
                + appointmentId + "_"
                + date + "_"
                + UUID.randomUUID()
                + extension;
        return uploadFile(uniqueFileName, content, contentType);
    }

    public String uploadInvoicePdf(String originalFileName, byte[] content, String contentType ,String billNo,Long appointmentId) {
        String extension = "";
        if (originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String date = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        String uniqueFileName = "NON_MEDREC_"
                + billNo + "_"
                + date + "_"
                + UUID.randomUUID()
                + extension;
        return uploadFile(uniqueFileName, content, contentType);
    }

    public String uploadClinicLogo(String originalFileName,
                                   byte[] content,
                                   String contentType) {

        String extension = "";

        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        String date = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        String uniqueFileName = "CLINIC_LOGO_"
                + date + "_"
                + UUID.randomUUID()
                + extension;

        return uploadFile(uniqueFileName, content, contentType);
    }

  /**
   * Checks if a file exists in the S3 bucket.
   *
   * @param fileName Name of the file to check.
   * @return True if the file exists, false otherwise.
   */
  public boolean doesFileExist(String fileName) {
    try {
      HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
          .bucket(bucketName)
          .key(fileName)
          .build();
      s3Client.headObject(headObjectRequest);
      logger.info("File exists in S3: {}", fileName);
      return true;
    } catch (S3Exception e) {
      logger.warn("File does not exist in S3: {}", fileName);
      return false;
    }
  }

  /**
   * Deletes a file from the S3 bucket.
   *
   * @param fileName Name of the file to delete.
   */
  public void deleteFile(String fileName) {
    try {
      DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
          .bucket(bucketName)
          .key(fileName)
          .build();
      s3Client.deleteObject(deleteObjectRequest);
      logger.info("File deleted from S3: {}", fileName);
    } catch (S3Exception e) {
      logger.error("Failed to delete file from S3: {}", fileName, e);
    }
  }

  /**
   * Downloads a file from S3 and returns it as a Base64-encoded string.
   *
   * @param fileName Name of the file to download.
   * @return Base64 string of the file content or null if failed.
   */
     public String getFileAsBase64(String fileName) {
    GetObjectRequest getObjectRequest = GetObjectRequest.builder()
        .bucket(bucketName)
        .key(fileName)
        .build();

    try (ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest)) {
      byte[] bytes = IoUtils.toByteArray(s3Object);
      String base64 = Base64.getEncoder().encodeToString(bytes);
      logger.info("File retrieved from S3 as Base64: {}", fileName);
      return base64;
    } catch (IOException e) {
      logger.error("Failed to download file from S3: {}", fileName, e);
      return null;
    }
  }

}