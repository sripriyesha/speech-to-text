package org.kailaasa.speechtotext;

import com.google.api.gax.paging.Page;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UploadToGCloud {
    public static void uploadObject(
        Storage storage,
        String bucketName,
        String objectName,
        String filePath
    ) throws IOException {        
        BlobId blobId = BlobId.of(bucketName, objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        storage.create(blobInfo, Files.readAllBytes(Paths.get(filePath)));

        System.out.println(
            "File " + filePath + " uploaded to bucket " + bucketName + " as " + objectName);
    }
  
    public static void main(String... args) throws Exception {
        GoogleCredentials credentials = GoogleCredentials.fromStream(
                new FileInputStream("speech-to-text-352619-e6ac4e047752.json")
        ).createScoped(
                Arrays.asList("https://www.googleapis.com/auth/cloud-platform")
        );

        Storage storage = StorageOptions
                .newBuilder()
                .setProjectId("speech-to-text")
                .setCredentials(credentials)
                .build()
                .getService()
        ;
        
        File dir = new File("./files");
        File[] directoryListing = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".flac");
            }
        });
        
        for (File file : directoryListing) {
            if (Files.exists(Paths.get("./subtitles/" + file.getName() + ".srt"))) {
                System.out.println(file.getName() + " subtitles already retrieved");
                continue;
            }
            
            boolean isAlreadyUploaded = false;
            
            Page<Blob> blobs = storage.list("speech-to-text-sharabheshwara-bucket1");
            
            for (Blob blob : blobs.iterateAll()) {
                boolean fileAlreadyExists = file.getName().equals(blob.getName());
                
                if (fileAlreadyExists) {
                    System.out.println(file.getName() + " already uploaded");
                    isAlreadyUploaded = true;
                    break;
                }
            }
            
            if (!isAlreadyUploaded) {
                uploadObject(
                    storage,
                    "speech-to-text-sharabheshwara-bucket1",
                    file.getName(),
                    "./files/" + file.getName()
                );
            }
        }
    }
}