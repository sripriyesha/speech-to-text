package org.kailaasa.speechtotext;

// Imports the Google Cloud client library
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.api.gax.paging.Page;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.speech.v1.LongRunningRecognizeMetadata;
import com.google.cloud.speech.v1.LongRunningRecognizeResponse;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.cloud.speech.v1.SpeechSettings;
import com.google.cloud.speech.v1.WordInfo;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SpeechToTranscripts {

    public static void asyncRecognizeWords(String gcsUri) throws Exception {
        CredentialsProvider credentialsProvider = FixedCredentialsProvider.create(
            ServiceAccountCredentials.fromStream(
                    new FileInputStream("speech-to-text-352619-e6ac4e047752.json")
            )
        );
        
      SpeechSettings settings = SpeechSettings.newBuilder().setCredentialsProvider(credentialsProvider).build();

      try (SpeechClient speech = SpeechClient.create(settings)) {

          // Configure remote file request for FLAC
        RecognitionConfig config =
            RecognitionConfig.newBuilder()
                .setEncoding(AudioEncoding.FLAC)
                .setLanguageCode("en-US")
                //.setSampleRateHertz(16000)
                .setEnableWordTimeOffsets(true)
                .build();
        RecognitionAudio audio = RecognitionAudio.newBuilder().setUri(gcsUri).build();

        // Use non-blocking call for getting file transcription
        OperationFuture<LongRunningRecognizeResponse, LongRunningRecognizeMetadata> response =
          speech.longRunningRecognizeAsync(config, audio);

        while (!response.isDone()) {
          System.out.println("Waiting for response...");
          Thread.sleep(10000);
        }

        List<SpeechRecognitionResult> results = response.get().getResultsList();

        int sequenceNumber = 1;

        for (SpeechRecognitionResult result : results) {
            // There can be several alternative transcripts for a given chunk of speech. Just use the
            // first (most likely) one here.
            SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
            System.out.printf("Transcription: %s\n", alternative.getTranscript());

            int i = 0;

            StringBuilder str = new StringBuilder();
            String prefix = "";

            String[] resultSplit = gcsUri.split("/");

            
            try(FileWriter fw = new FileWriter("./transcripts/" + resultSplit[3] + ".txt", true);
                BufferedWriter bw = new BufferedWriter(fw);
                PrintWriter out = new PrintWriter(bw)
            ){
                for (WordInfo wordInfo : alternative.getWordsList()) {
                    if (i == 0) {
                        out.println(sequenceNumber);

                        str.append(prefix);
                        prefix = " ";
                        str.append(wordInfo.getWord());

                        i++;

                        continue;
                    } 

                    if (i%10 == 0) {                    
                        out.println(str.toString());
                        out.println();
                        str.setLength(0);

                        sequenceNumber++;
                    }

                    str.append(prefix);
                    str.append(wordInfo.getWord());

                    i++;
                }

                if (!str.isEmpty()) {
                    out.println(str.toString());
                    out.println();
                    sequenceNumber++;
                }
            }
        }
        
        
      }
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
        Page<Blob> blobs = storage.list("speech-to-text-sharabheshwara-bucket1");
        
        for (Blob blob : blobs.iterateAll()) {
            String blobName = blob.getName();
            if (Files.exists(Paths.get("./transcripts/" + blobName + ".txt"))) {
                continue;
            }
            
            asyncRecognizeWords(
                "gs://speech-to-text-sharabheshwara-bucket1/" + blobName
            );
            
            storage.delete("speech-to-text-sharabheshwara-bucket1", blobName);

            System.out.println(
                "Object " +
                blob.getName() +
                " was deleted from " +
                "speech-to-text-sharabheshwara-bucket1"
            );
            
        }
    }
}
        