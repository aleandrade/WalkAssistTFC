package com.motorola.samples.walkassist;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;

/**
 * Created by andrade on 9/19/17.
 */

public class BlindActivity extends Activity {
    private static final int REQUEST_MICROPHONE = 2;
    private SpeechRecognizer speechRecognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.blind_screen);

        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_MICROPHONE);

        }

        VibrationAssist.setPreviousValue(0);

        final Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,"voice.recognition.test");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new listener());

        final Button bttnSpeech = (Button)findViewById(R.id.speech);
        bttnSpeech.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bttnSpeech.getText().equals("r")){
                    bttnSpeech.setText("l");
                    bttnSpeech.setBackgroundDrawable(getResources().getDrawable(R.drawable.speech_button_green));
                    speechRecognizer.startListening(recognizerIntent);
                } else if(bttnSpeech.getText().equals("l")) {
                    bttnSpeech.setText("r");
                    bttnSpeech.setBackgroundDrawable(getResources().getDrawable(R.drawable.speech_button));
                    speechRecognizer.stopListening();
                }
            }
        });
    }

    public void compareText(String speechRecognized){
        Button bttn = (Button)findViewById(R.id.speech);

        if(!speechRecognized.contains("desligar") && speechRecognized.contains("ligar vibração") || speechRecognized.contains("liga vibração")){
            VibrationAssist.vibrateProximity(100, getApplicationContext());
            bttn.setBackgroundDrawable(getResources().getDrawable(R.drawable.speech_button));
            speechRecognizer.stopListening();
        } else if(speechRecognized.contains("desligar vibração") || speechRecognized.contains("desliga vibração")){
            VibrationAssist.cancelVibration(getApplicationContext());
            bttn.setBackgroundDrawable(getResources().getDrawable(R.drawable.speech_button));
            speechRecognizer.stopListening();
        } else if(speechRecognized.contains("modo desenvolvedor")){
            speechRecognizer.stopListening();
            VibrationAssist.cancelVibration(getApplicationContext());
            startActivity(new Intent(this, DebugActivity.class));
        }
    }

    class listener implements RecognitionListener
    {
        public void onReadyForSpeech(Bundle params)
        {
        }
        public void onBeginningOfSpeech()
        {

        }
        public void onRmsChanged(float rmsdB)
        {
        }
        public void onBufferReceived(byte[] buffer)
        {
        }
        public void onEndOfSpeech()
        {
        }
        public void onError(int error)
        {
            String message = "";
            if(error == SpeechRecognizer.ERROR_AUDIO)                           message = "audio";
            else if(error == SpeechRecognizer.ERROR_CLIENT)                     message = "client";
            else if(error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS)   message = "insufficient permissions";
            else if(error == SpeechRecognizer.ERROR_NETWORK)                    message = "network";
            else if(error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT)            message = "network timeout";
            else if(error == SpeechRecognizer.ERROR_NO_MATCH)                   message = "no match found";
            else if(error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY)            message = "recognizer busy";
            else if(error == SpeechRecognizer.ERROR_SERVER)                     message = "server";
            else if(error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)             message = "speech timeout";
        }
        public void onResults(Bundle results)
        {
            String str = new String();
            ArrayList data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            for (int i = 0; i < data.size(); i++)
            {
                str += data.get(i);
            }
            Button bttn = (Button)findViewById(R.id.speech);
            bttn.setBackgroundDrawable(getResources().getDrawable(R.drawable.speech_button));
        }
        public void onPartialResults(Bundle partialResults) {
            ArrayList data = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            String word = (String) data.get(data.size() - 1);
            if(!word.equals("")){
                compareText(word.toLowerCase());
            }
        }
        public void onEvent(int eventType, Bundle params)
        {
        }
    }

}
