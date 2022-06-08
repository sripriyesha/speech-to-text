package org.kailaasa.speechtotext;

// Imports the Google Cloud client library
import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.speech.v1.LongRunningRecognizeMetadata;
import com.google.cloud.speech.v1.LongRunningRecognizeResponse;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.cloud.speech.v1.WordInfo;
import com.google.protobuf.ByteString;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SpeechToText {

    public static void asyncRecognizeWords(String gcsUri) throws Exception {
      // Instantiates a client with GOOGLE_APPLICATION_CREDENTIALS
      try (SpeechClient speech = SpeechClient.create()) {

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

            String strStartHour = "";
            String strStartMinute = "";
            String strStartSeconds = "";
            String strStartNanos = "";
            long startNanos = 0;

            String strPreviousEndHour = "";
            String strPreviousEndMinute = "";
            String strPreviousEndSeconds = "";
            String strPreviousEndNanos = "";
            long previousEndNanos = 0;

            StringBuilder str = new StringBuilder();
            String prefix = "";

            String[] resultSplit = gcsUri.split("/");


            try(FileWriter fw = new FileWriter("./subtitles/" + resultSplit[3] + ".srt", true);
                BufferedWriter bw = new BufferedWriter(fw);
                PrintWriter out = new PrintWriter(bw)
            ){
                for (WordInfo wordInfo : alternative.getWordsList()) {
                    if (i == 0) {
                        out.println(sequenceNumber);

                        long startSeconds = wordInfo.getStartTime().getSeconds();
                        strStartHour = String.format("%02d", startSeconds / 3600);
                        strStartMinute = String.format("%02d", (startSeconds %3600) / 60);
                        strStartSeconds = String.format("%02d", startSeconds % 60);
                        startNanos = TimeUnit.NANOSECONDS.toMillis(wordInfo.getStartTime().getNanos());

                        if (startNanos == 0) {
                            strStartNanos = "000";
                        } else {
                            strStartNanos = String.valueOf(startNanos);
                        }

                        str.append(prefix);
                        prefix = " ";
                        str.append(wordInfo.getWord());

                        long previousEndSeconds = wordInfo.getEndTime().getSeconds();
                        strPreviousEndHour = String.format("%02d", previousEndSeconds / 3600);
                        strPreviousEndMinute = String.format("%02d", (previousEndSeconds %3600) / 60);
                        strPreviousEndSeconds = String.format("%02d", previousEndSeconds % 60);
                        previousEndNanos = TimeUnit.NANOSECONDS.toMillis(wordInfo.getEndTime().getNanos());

                        if (previousEndNanos == 0) {
                            strPreviousEndNanos = "000";
                        } else {
                            strPreviousEndNanos = String.valueOf(previousEndNanos);
                        }

                        i++;

                        continue;
                    } 

                    if (i%10 == 0) {                    
                        out.printf(
                            "%s:%s:%s,%s --> %s:%s:%s,%s\n",
                            strStartHour,
                            strStartMinute,
                            strStartSeconds,
                            strStartNanos,
                            strPreviousEndHour,
                            strPreviousEndMinute,
                            strPreviousEndSeconds,
                            previousEndNanos
                        ); 
                        out.println(str.toString());
                        out.println();
                        str.setLength(0);

                        sequenceNumber++;

                        long startSeconds = wordInfo.getStartTime().getSeconds();
                        strStartHour = String.format("%02d", startSeconds / 3600);
                        strStartMinute = String.format("%02d", (startSeconds %3600) / 60);
                        strStartSeconds = String.format("%02d", startSeconds % 60);
                        startNanos = TimeUnit.NANOSECONDS.toMillis(wordInfo.getStartTime().getNanos());

                        if (startNanos == 0) {
                            strStartNanos = "000";
                        } else {
                            strStartNanos = String.valueOf(startNanos);
                        }
                    }

                    str.append(prefix);
                    str.append(wordInfo.getWord());

                    long previousEndSeconds = wordInfo.getEndTime().getSeconds();
                    strPreviousEndHour = String.format("%02d", previousEndSeconds / 3600);
                    strPreviousEndMinute = String.format("%02d", (previousEndSeconds %3600) / 60);
                    strPreviousEndSeconds = String.format("%02d", previousEndSeconds % 60);
                    previousEndNanos = TimeUnit.NANOSECONDS.toMillis(wordInfo.getEndTime().getNanos());

                    if (previousEndNanos == 0) {
                        strPreviousEndNanos = "000";
                    } else {
                        strPreviousEndNanos = String.valueOf(previousEndNanos);
                    }

                    i++;
                }

                if (!str.isEmpty()) {
                    out.printf(
                        "%s:%s:%s,%s --> %s:%s:%s,%s\n",
                        strStartHour,
                        strStartMinute,
                        strStartSeconds,
                        strStartNanos,
                        strPreviousEndHour,
                        strPreviousEndMinute,
                        strPreviousEndSeconds,
                        strPreviousEndNanos
                    ); 
                    out.println(str.toString());
                    out.println();
                    sequenceNumber++;
                }
            }
        }
      }
    }

    public static void main(String... args) throws Exception {
        asyncRecognizeWords("gs://speech-to-text-sharabheshwara-bucket1/Why does an Avatar or Incarnation happen_.flac");
    }
}
        