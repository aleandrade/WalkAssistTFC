package com.motorola.samples.walkassist;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.motorola.mod.ModDevice;
import com.motorola.mod.ModManager;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by andrade on 9/19/17.
 */

public class BlindActivity extends Activity {
    private static final int REQUEST_MICROPHONE = 2;
    private static final int RC_VIBRATE = 1;
    private static final int RAW_PERMISSION_REQUEST_CODE = 100;
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech tts;
    String language;
    private boolean vibrationON = false;
    /**
     * Instance of MDK Personality Card interface
     */
    private Personality personality;

    /**
     * Commands
     */
    public static byte[] RAW_CMD_ADC_OFF = {0x00,0x00,0x00};
    public static byte[] RAW_CMD_ADC_ON = {0x00,0x00,0x01};

    /** Handler for events from mod device */
    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Personality.MSG_MOD_DEVICE:
                    /** Mod attach/detach */
                    ModDevice device = personality.getModDevice();
                    break;
                case Personality.MSG_RAW_DATA:
                    /** Mod raw data */
                    byte[] buff = (byte[]) msg.obj;
                    int length = msg.arg1;
                    onRawData(buff, length);
                    break;
                case Personality.MSG_RAW_IO_READY:
                    /** Mod RAW I/O ready to use */
                    onRawInterfaceReady();
                    break;
                case Personality.MSG_RAW_IO_EXCEPTION:
                    /** Mod RAW I/O exception */
                    break;
                case Personality.MSG_RAW_REQUEST_PERMISSION:
                    /** Request grant RAW_PROTOCOL permission */
                    onRequestRawPermission();
                default:
                    Log.i(Constants.TAG, "DebugActivity - Un-handle events: " + msg.what);
                    break;
            }
        }
    };

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
                        tts.speak("Os comandos possíveis são. ligar. e desligar auxílio. trocar idioma. e mudar para modo desenvolvedor.", TextToSpeech.QUEUE_ADD, null, null);
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

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.VIBRATE) != PackageManager
                .PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.VIBRATE},
                    RC_VIBRATE);
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
                tts.stop();
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

//        if (personality == null || personality.getModDevice() == null) {
//            Toast.makeText(BlindActivity.this, getString(R.string.sensor_not_available),
//                    Toast.LENGTH_SHORT).show();
//            return;
//        }

        if(personality != null){
            personality.getRaw().executeRaw(RAW_CMD_ADC_ON);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        /** Initial MDK Personality interface */
        initPersonality();
        personality.getRaw().executeRaw(RAW_CMD_ADC_ON);

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new listener());
    }

    @Override
    public void onPause() {
        speechRecognizer.destroy();
        if(speechRecognizer!=null){
            speechRecognizer.stopListening();
            speechRecognizer.cancel();
            speechRecognizer.destroy();
        }
        speechRecognizer = null;
        super.onPause();
    }

    public void compareText(String speechRecognized){
        Button bttn = (Button)findViewById(R.id.speech);

        if(!speechRecognized.contains("desligar") && speechRecognized.contains("ligar vibração") || speechRecognized.contains("liga vibração")){
            bttn.setBackgroundDrawable(getResources().getDrawable(R.drawable.speech_button));
            vibrationON = true;
            speechRecognizer.stopListening();
        } else if(speechRecognized.contains("desligar vibração") || speechRecognized.contains("desliga vibração")){
            VibrationAssist.cancelVibration(getApplicationContext());
            vibrationON = false;
            bttn.setBackgroundDrawable(getResources().getDrawable(R.drawable.speech_button));
            speechRecognizer.stopListening();
            tts.speak("Desligado.", TextToSpeech.QUEUE_ADD, null, null);
        } else if(!speechRecognized.contains("desligar") && speechRecognized.contains("ligar auxílio") || speechRecognized.contains("liga vibração")){
            bttn.setBackgroundDrawable(getResources().getDrawable(R.drawable.speech_button));
            vibrationON = true;
            speechRecognizer.stopListening();
            tts.speak("Ligado.", TextToSpeech.QUEUE_ADD, null, null);
        } else if(speechRecognized.contains("desligar auxílio") || speechRecognized.contains("desliga auxílio")){
            VibrationAssist.cancelVibration(getApplicationContext());
            bttn.setBackgroundDrawable(getResources().getDrawable(R.drawable.speech_button));
            vibrationON = false;
            speechRecognizer.stopListening();
            tts.speak("Desligado.", TextToSpeech.QUEUE_ADD, null, null);
        } else if(speechRecognized.contains("modo desenvolvedor")){
            speechRecognizer.stopListening();
            vibrationON = false;
            VibrationAssist.cancelVibration(getApplicationContext());
            releasePersonality();
            Intent developerIntent = new Intent(this, DebugActivity.class);
            developerIntent.putExtra("language", language);
            bttn.setBackgroundDrawable(getResources().getDrawable(R.drawable.speech_button));
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
            vibrationON = false;
            VibrationAssist.cancelVibration(getApplicationContext());
            releasePersonality();
            Intent developerIntent = new Intent(this, DebugActivity.class);
            developerIntent.putExtra("language", language);
            bttn.setBackgroundDrawable(getResources().getDrawable(R.drawable.speech_button));
            startActivity(developerIntent);
        } else if(speechRecognized.contains("language")) {
            speechRecognizer.stopListening();
            language = "português";
            tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status == TextToSpeech.SUCCESS) {
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

    /** Initial MDK Personality interface */
    private void initPersonality() {
        if (null == personality) {
            personality = new RawPersonality(this, Constants.VID_MDK, Constants.PID_TEMPERATURE);
            personality.registerListener(handler);
        }
    }

    /*
     * Beginning in Android 6.0 (API level 23), users grant permissions to apps while
     * the app is running, not when they install the app. App need check on and request
     * permission every time perform an operation.
    */
    public void onRequestRawPermission() {
        requestPermissions(new String[]{ModManager.PERMISSION_USE_RAW_PROTOCOL},
                RAW_PERMISSION_REQUEST_CODE);
    }

    /** Clean up MDK Personality interface */
    private void releasePersonality() {
        /** Save currently temperature recording status */
//        Switch switcher = (Switch) findViewById(R.id.sensor_switch);
//        if (switcher.isChecked()) {
//            Toast.makeText(this, getString(R.string.sensor_stop), Toast.LENGTH_SHORT).show();
//        }
        SharedPreferences preference = getSharedPreferences("recordingRaw", MODE_PRIVATE);
        preference.edit().putBoolean("recordingRaw", false).commit();

        /** Clean up MDK Personality interface */
        if (personality != null) {
            personality.getRaw().executeRaw(Constants.RAW_CMD_STOP);
            personality.onDestroy();
            personality = null;
        }
    }

    /** Got data from mod device RAW I/O */
    public void onRawData(byte[] buffer, int length) {
        int integer1 = 0;
        int integer2 = 0;
        int integer3 = 0;
        TextView text = (TextView) findViewById(R.id.textView);
        String sensor = "nenhum";

        if (length >= 2){
            integer1 = (buffer[0]&0xFF)+(buffer[1]&0xFF)*256;
        }
        if (length >= 4){
            integer2 = (buffer[2]&0xFF)+(buffer[3]&0xFF)*256;
            text.setText(Integer.toString(integer2));
        }
        if (length == 6){
            integer3 = (buffer[4]&0xFF)+(buffer[5]&0xFF)*256;
        }

        if(vibrationON){
            VibrationAssist.vibrateProximity(integer1, integer2, integer3, tts, getApplicationContext());
        }
    }

    /** RAW I/O of attached mod device is ready to use */
    public void onRawInterfaceReady() {
        /**
         *  Personality has the RAW interface, query the information data via RAW command, the data
         *  will send back from MDK with flag TEMP_RAW_COMMAND_INFO and TEMP_RAW_COMMAND_CHALLENGE.
         */
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                personality.getRaw().executeRaw(Constants.RAW_CMD_INFO);
            }
        }, 500);
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
                speechRecognizer.stopListening();
                Button bttn = (Button)findViewById(R.id.speech);
                bttn.setBackgroundDrawable(getResources().getDrawable(R.drawable.speech_button));
                tts.speak("Comando não encontrado", TextToSpeech.QUEUE_ADD, null, null);
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
            speechRecognizer.cancel();
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
            speechRecognizer.stopListening();
            speechRecognizer.cancel();
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
