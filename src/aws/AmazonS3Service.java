package aws;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface AmazonS3Service {
    String generateUrl(String s3Path);

    String generateUrlToPublicResource(String s3Path);

    void uploadToS3(String contentType, long size, String s3Key, InputStream inputStream);

    void uploadToS3(String contentType, long size, String s3Key, InputStream inputStream, boolean isPublic);

    void downloadFromS3(String s3Path, OutputStream outputStream) throws IOException;

    void downloadFromS3(String s3Path, File localFile) throws IOException;

    void deleteFromS3(String s3Key, boolean isPublic);
}