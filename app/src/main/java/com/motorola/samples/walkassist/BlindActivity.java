package com.motorola.samples.walkassist;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by andrade on 9/19/17.
 */

public class BlindActivity extends Activity {
    private static final int REQUEST_MICROPHONE = 2;
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech tts;

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

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.forLanguageTag("PT-BR"));
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(BlindActivity.this, "Language error", Toast.LENGTH_SHORT).show();
                    }
                    tts.speak("Bem vindo ao walk assist.", TextToSpeech.QUEUE_ADD, null, null);
                    tts.speak("Os comandos possíveis são. ligar. e desligar vibração. trocar idioma. e mudar para modo desenvolvedor.", TextToSpeech.QUEUE_ADD, null, null);
                    tts.speak("Toque na tela para falar", TextToSpeech.QUEUE_ADD, null, null);
                } else {
                    Toast.makeText(BlindActivity.this, "Initialization failed", Toast.LENGTH_SHORT).show();                }
            }
        });

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
        Button bttn = (Button)findViewById(R.id.speech);
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

            Toast.makeText(BlindActivity.this, message, Toast.LENGTH_SHORT).show();
            tts.speak("Erro", TextToSpeech.QUEUE_ADD, null, null);
            speechRecognizer.destroy();
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
            speechRecognizer.setRecognitionListener(new listener());
            bttn.setBackgroundDrawable(getResources().getDrawable(R.drawable.speech_button));
        }
        public void onResults(Bundle results)
        {
            String str = new String();
            ArrayList data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            for (int i = 0; i < data.size(); i++)
            {
                str += data.get(i);
            }
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
