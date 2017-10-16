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
    String language;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.blind_screen);

        Intent intent = getIntent();
        final String lang = intent.getStringExtra("language");

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    if(lang == null){
                        language = Locale.getDefault().getDisplayLanguage();
                    } else {
                        language = lang;
                    }
                    if(language.equals("português")) {
                        int result = tts.setLanguage(Locale.forLanguageTag("PT-BR"));
                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            Toast.makeText(BlindActivity.this, "Language error", Toast.LENGTH_SHORT).show();
                        }
                        tts.speak("Bem vindo ao walk assist.", TextToSpeech.QUEUE_ADD, null, null);
                        tts.speak("Os comandos possíveis são. ligar. e desligar vibração. trocar idioma. e mudar para modo desenvolvedor.", TextToSpeech.QUEUE_ADD, null, null);
                        tts.speak("Toque na tela para falar", TextToSpeech.QUEUE_ADD, null, null);
                    } else {
                        int result = tts.setLanguage(Locale.forLanguageTag("en"));
                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            Toast.makeText(BlindActivity.this, "Language error", Toast.LENGTH_SHORT).show();
                        }
                        tts.speak("Welcome to Walk Assist", TextToSpeech.QUEUE_ADD, null, null);
                        tts.speak("The available commands are. vibration on. and off.  " + " change language to " +
                                "Portuguese. and developer mode.", TextToSpeech.QUEUE_ADD, null, null);
                        tts.speak("Touch the screen to speak", TextToSpeech.QUEUE_ADD, null, null);
                    }
                } else {
                    Toast.makeText(BlindActivity.this, "Initialization failed", Toast.LENGTH_SHORT).show();
                }
            }
        });

        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_MICROPHONE);

        }

        VibrationAssist.setPreviousValue(0);

        final Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "PT-BR");
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
                tts.shutdown();
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
            bttn.setBackgroundDrawable(getResources().getDrawable(R.drawable.speech_button));
            speechRecognizer.stopListening();
        } else if(speechRecognized.contains("desligar vibração") || speechRecognized.contains("desliga vibração")){
            VibrationAssist.cancelVibration(getApplicationContext());
            bttn.setBackgroundDrawable(getResources().getDrawable(R.drawable.speech_button));
            speechRecognizer.stopListening();
        } else if(speechRecognized.contains("modo desenvolvedor")){
            speechRecognizer.stopListening();
            VibrationAssist.cancelVibration(getApplicationContext());
            Intent developerIntent = new Intent(this, DebugActivity.class);
            developerIntent.putExtra("language", language);
            startActivity(developerIntent);
        } else if (speechRecognized.contains("vibration")){
            if(speechRecognized.contains("off")){
                VibrationAssist.cancelVibration(getApplicationContext());
                bttn.setBackgroundDrawable(getResources().getDrawable(R.drawable.speech_button));
                speechRecognizer.stopListening();
            } else if(speechRecognized.contains("on")){
                bttn.setBackgroundDrawable(getResources().getDrawable(R.drawable.speech_button));
                speechRecognizer.stopListening();
            }
        } else if(speechRecognized.contains("developer mode")) {
            speechRecognizer.stopListening();
            VibrationAssist.cancelVibration(getApplicationContext());
            Intent developerIntent = new Intent(this, DebugActivity.class);
            developerIntent.putExtra("language", language);
            startActivity(developerIntent);
        } else if(speechRecognized.contains("language")) {
            speechRecognizer.stopListening();
            language = "português";
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
                        Toast.makeText(BlindActivity.this, "Initialization failed", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else if(speechRecognized.contains("idioma")) {
            speechRecognizer.stopListening();
            language = "English";
            tts.shutdown();
            tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status == TextToSpeech.SUCCESS) {
                        int result = tts.setLanguage(Locale.forLanguageTag("en"));
                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            Toast.makeText(BlindActivity.this, "Language error", Toast.LENGTH_SHORT).show();
                        }
                        tts.speak("Welcome to Walk Assist", TextToSpeech.QUEUE_ADD, null, null);
                        tts.speak("The available commands are. vibration on. and off.  " + " change language to " +
                                "Portuguese. and developer mode.", TextToSpeech.QUEUE_ADD, null, null);
                        tts.speak("Touch the screen to speak", TextToSpeech.QUEUE_ADD, null, null);
                    } else {
                        Toast.makeText(BlindActivity.this, "Initialization failed", Toast.LENGTH_SHORT).show();
                    }
                }
            });
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
        public void onError(int error) {
            String message = "";
            if (error == SpeechRecognizer.ERROR_AUDIO) message = "audio";
            else if (error == SpeechRecognizer.ERROR_CLIENT) {
                message = "client";
                tts.speak("Erro. Tente novamente", TextToSpeech.QUEUE_ADD, null, null);
            } else if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                message = "insufficient permissions";
                tts.speak("Erro nas permissões", TextToSpeech.QUEUE_ADD, null, null);
            }
            else if (error == SpeechRecognizer.ERROR_NETWORK){
                message = "network";
                tts.speak("Erro de rede", TextToSpeech.QUEUE_ADD, null, null);
            }
            else if (error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT){
                message = "network timeout";
                tts.speak("Timeout de rede", TextToSpeech.QUEUE_ADD, null, null);
            }
            else if (error == SpeechRecognizer.ERROR_NO_MATCH){
                message = "No match found";
                Toast.makeText(BlindActivity.this, message, Toast.LENGTH_SHORT).show();
                return;
            }
            else if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY){
                message = "Recognizer busy";
                tts.speak("Tente novamente", TextToSpeech.QUEUE_ADD, null, null);
            }
            else if (error == SpeechRecognizer.ERROR_SERVER){
                message = "server";
                tts.speak("Erro de servidor", TextToSpeech.QUEUE_ADD, null, null);
            }
            else if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) message = "speech timeout";

            Toast.makeText(BlindActivity.this, message, Toast.LENGTH_SHORT).show();
            speechRecognizer.destroy();
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
            speechRecognizer.setRecognitionListener(new listener());
            Button bttn = (Button)findViewById(R.id.speech);
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
