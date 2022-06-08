## Run with Netbeans

make sure to add google credentials to Netbeans:

1. Right click on the project
2. click on Properties
3. click on Actions
4. click on Run project
5. add the environment variable as follows:
   Env.GOOGLE_APPLICATION_CREDENTIALS=/Users/florent/Projects/github/speech-to-text/speech-to-text-352619-e6ac4e047752.json

## Convert mp4 to FLAC for Google Cloud

ffmpeg -i video.mp4 -ac 1 audio.flac

-ac 1 needed because the Speech to Text service require only one audio channel
