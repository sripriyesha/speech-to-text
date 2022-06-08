package org.kailaasa.speechtotext;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class UploadToGCloud {
    public static void uploadObject(
        String projectId,
        String bucketName,
        String objectName,
        String filePath
    ) throws IOException {
        // The ID of your GCP project
        // String projectId = "your-project-id";

        // The ID of your GCS bucket
        // String bucketName = "your-unique-bucket-name";

        // The ID of your GCS object
        // String objectName = "your-object-name";

        // The path to your file to upload
        // String filePath = "path/to/your/file"
        
        GoogleCredentials credentials = GoogleCredentials.fromStream(
                new FileInputStream("speech-to-text-352619-e6ac4e047752.json")
        ).createScoped(
                Arrays.asList("https://www.googleapis.com/auth/cloud-platform")
        );

        Storage storage = StorageOptions
                .newBuilder()
                .setProjectId(projectId)
                .setCredentials(credentials)
                .build()
                .getService()
        ;
        
        BlobId blobId = BlobId.of(bucketName, objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        storage.create(blobInfo, Files.readAllBytes(Paths.get(filePath)));

        System.out.println(
            "File " + filePath + " uploaded to bucket " + bucketName + " as " + objectName);
    }
  
    public static void main(String... args) throws Exception {
        try (Stream<Path> paths = Files.walk(Paths.get("./files"))) {
            paths
            .filter(Files::isRegularFile)
            .filter(p -> p.getFileName().toString().endsWith(".flac"))
            .forEach(p -> {
                try {
                    uploadObject(
                        "speech-to-text",
                        "speech-to-text-sharabheshwara-bucket1",
                        p.getFileName().toString(),
                        "./files/" + p.getFileName().toString()
                    );
                } catch (IOException ex) {
                    Logger.getLogger(UploadToGCloud.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
        } 
    }

}