package aws;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.Calendar;
import java.util.TimeZone;

//@Service
public class SimpleAmazonS3Service implements AmazonS3Service {

    //@Value("${aws.s3.secure.bucketName}")
    public String secureBucketName = "dg-img";

    //@Value("${aws.s3.public.bucketName}")
    public String publicBucketName = "dg-img";

    //@Autowired
    ///com.amazonaws.services.s3.AmazonS3Client


    public String secret = "bN5ujDqr4uaNHI38rApTaSRdRkVodYSYaYecgLTy";
    public String access = "AKIAI7XFPWUN2NTT4GDA";

    public AmazonS3 amazonS3 = new AmazonS3Client(new BasicAWSCredentials(access, secret));
    //private static final Logger LOGGER = LoggerFactory.getLogger(AssetsController.class);

    @Override
    public void uploadToS3(String contentType, long size, String s3Key, InputStream inputStream) {
        uploadToS3(contentType, size, s3Key, inputStream, true);
    }

    @Override
    public void uploadToS3(String contentType, long size, String s3Key, InputStream inputStream, boolean isPublic) {
        String bucketName = isPublic ? publicBucketName : secureBucketName;
        uploadToS3(bucketName, contentType, size, s3Key, inputStream);
    }

    public void uploadToS3(String bucketName, String contentType, long size, String s3Key, InputStream inputStream) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();

            metadata.setContentType(contentType);
            metadata.setContentLength(size);

            amazonS3.putObject(bucketName, s3Key, inputStream, metadata);

            //LOGGER.info("Successfully Uploaded " + bucketName + "/" + s3Key + " to s3");

        } catch (AmazonClientException e) {
            //LOGGER.error("Error uploading file to s3", e);
            System.out.println("Error uploading file to s3" + e.getMessage());
            throw new RuntimeException("Error uploading file to s3", e);
        }
    }

    @Override
    public String generateUrlToPublicResource(String s3Path) {
        return "https://s3.amazonaws.com/" + publicBucketName + "/" + s3Path;
    }

    @Override
    public String generateUrl(String s3Path) {
        return generatePresignedUrl(secureBucketName, s3Path);
    }

    public String generatePresignedUrl(String bucketName, String s3Path) {
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        now.add(Calendar.MINUTE, 30);
        //LOGGER.info("creating singed url for " + s3Path);
        return amazonS3.generatePresignedUrl(bucketName, s3Path, now.getTime()).toString();
    }

    @Override
    public void downloadFromS3(String s3Path, OutputStream outputStream) throws IOException {
        S3Object object = amazonS3.getObject(secureBucketName, s3Path);
        //LOGGER.info("Started download of file {}.", s3Path);
        S3ObjectInputStream s3InputStream = null;
        try {
            s3InputStream = object.getObjectContent();
            IOUtils.copy(s3InputStream, outputStream);
        } finally {
            //LOGGER.info("Finished download of file {}.", s3Path);
            if (s3InputStream != null) {
                s3InputStream.close();
            }
        }
    }

    @Override
    public void downloadFromS3(String s3Path, File localFile) throws IOException {
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(localFile);
            downloadFromS3(s3Path, outputStream);
        } finally {
            if (outputStream != null) {
                outputStream.close();
            }
        }
    }

    public void deleteFromS3(String s3Key, boolean isPublic) {
        String bucketName = isPublic ? publicBucketName : secureBucketName;
        amazonS3.deleteObject(bucketName, s3Key);
    }

}