/**
 * Copyright (c) 2016 Motorola Mobility, LLC.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.motorola.samples.walkassist;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.motorola.mod.ModDevice;
import com.motorola.mod.ModManager;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;

/**
 * A class to represent main activity.
 */
public class DebugActivity extends Activity{
    public static final String MOD_UID = "mod_uid";

    private static final int RAW_PERMISSION_REQUEST_CODE = 100;
    private static final int RC_VIBRATE = 010;

    /**
     * Instance of MDK Personality Card interface
     */
    private Personality personality;

    /**
     * Line chart to draw temperature values
     */
    private static int count;
    private static float maxTop = 80f;
    private static float minTop = 70f;
    private LineChartView chart;
    private Viewport viewPort;
    private static final int RESULT_SPEECH = 1;
    private static final int REQUEST_MICROPHONE = 2;

    private boolean vibrate = false;

    TextView display;
    Button bttnVibration;
    Button bttnVoice;
    private SpeechRecognizer speechRecognizer;

    public int TOGGLE_ON = 0;
    public int TOGGLE_OFF = 1;
    public int TOGGLE_ANY = 2;

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
                    onModDevice(device);
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
                    onIOException();
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
        setContentView(R.layout.activity_main);

        VibrationAssist.setPreviousValue(0);
        display = (TextView) findViewById(R.id.display);

        final Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,"voice.recognition.test");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new listener());

        bttnVoice = (Button)findViewById(R.id.bttnVoice);
        bttnVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Drawable background = bttnVoice.getBackground();
                if(((ColorDrawable)background).getColor() == Color.parseColor("#00ff00")){
                    speechRecognizer.stopListening();
                    bttnVoice.setBackgroundColor(Color.parseColor("#000000"));
                } else {
                    speechRecognizer.startListening(recognizerIntent);
                }
            }
        });

        final Button bttnADC = (Button)findViewById(R.id.bttnADC);
        bttnADC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] CollectData = {0x05};
                personality.getRaw().executeRaw(CollectData);
            }
        });

        bttnVibration = (Button)findViewById(R.id.bttnVibration);
        bttnVibration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleVibration(TOGGLE_ANY);
            }
        });

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.VIBRATE) != PackageManager
                .PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.VIBRATE},
                    RC_VIBRATE);
        }

        if (personality == null || personality.getModDevice() == null) {
            Toast.makeText(DebugActivity.this, getString(R.string.sensor_not_available),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // PID_WALKASSIST, VER NUMERO!!!!!!!!!1111ONZE!!!!
        if (personality.getModDevice().getVendorId() != Constants.VID_DEVELOPER
                && !(personality.getModDevice().getVendorId() == Constants.VID_MDK
                && personality.getModDevice().getProductId() == Constants.PID_WALKASSIST)) {
            Toast.makeText(DebugActivity.this, getString(R.string.sensor_not_available),
                    Toast.LENGTH_LONG).show();
            return;
        }

        CheckBox autoCollect = (CheckBox)findViewById(R.id.autoCollect);
        autoCollect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    bttnADC.setEnabled(false);
                    bttnADC.setBackgroundColor(0xFF222222);
                    bttnADC.setTextColor(0xFF666666);
                    personality.getRaw().executeRaw(RAW_CMD_ADC_ON);
                } else {
                    VibrationAssist.cancelVibration(getApplicationContext());
                    bttnADC.setEnabled(true);
                    bttnADC.setBackgroundColor(0xFF5CC0A0);
                    bttnADC.setTextColor(0xFFFFFFFF);
                    personality.getRaw().executeRaw(RAW_CMD_ADC_OFF);
                }
            }
        });

        /** Save currently temperature recording status */
        SharedPreferences preference = getSharedPreferences("recordingRaw", MODE_PRIVATE);
        //preference.edit().putBoolean("recordingRaw", ).commit();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releasePersonality();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        /** Initial MDK Personality interface */
        initPersonality();

        /** Restore temperature record status */
        /*Switch switcher = (Switch) findViewById(R.id.sensor_switch);
        if (switcher != null) {
            SharedPreferences preference = getSharedPreferences("recordingRaw", MODE_PRIVATE);
            switcher.setChecked(preference.getBoolean("recordingRaw", false));
        }*/
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public void toggleVibration(int option){
        String onOff = bttnVibration.getText().toString();
        if(onOff.equals("Vibration OFF") && option != TOGGLE_OFF) {
            vibrate = true;
            bttnVibration.setText("Vibration ON");
            bttnVibration.setBackgroundColor(Color.parseColor("#5CC0A0"));
            VibrationAssist.vibrateProximity(100, getApplicationContext());
        } else if(option != TOGGLE_ON){
            vibrate = false;
            bttnVibration.setText("Vibration OFF");
            bttnVibration.setBackgroundColor(Color.parseColor("#FFFF4444"));
            VibrationAssist.cancelVibration(getApplicationContext());
        }
    }

    /** Initial MDK Personality interface */
    private void initPersonality() {
        if (null == personality) {
            personality = new RawPersonality(this, Constants.VID_MDK, Constants.PID_TEMPERATURE);
            personality.registerListener(handler);
        }
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

    /** Mod device attach/detach */
    public void onModDevice(ModDevice device) {
        /** Moto Mods Status */
        /**
         * Get mod device's Product String, which should correspond to
         * the product name or the vendor internal's name.
         */
        TextView tvName = (TextView) findViewById(R.id.mod_name);
        if (null != tvName) {
            if (null != device) {
                tvName.setText(device.getProductString());
                tvName.setTextColor(getColor(R.color.mod_match));
            } else {
                tvName.setText(getString(R.string.na));
                tvName.setTextColor(getColor(R.color.mod_na));
            }
        }

        /**
         * Get mod device's Vendor ID. This is assigned by the Motorola
         * and unique for each vendor.
         */
        TextView tvVid = (TextView) findViewById(R.id.mod_status_vid);
        if (null != tvVid) {
            if (device == null
                    || device.getVendorId() == Constants.INVALID_ID) {
                tvVid.setText(getString(R.string.na));
            } else {
                tvVid.setText(String.format(getString(R.string.mod_pid_vid_format),
                        device.getVendorId()));
            }
        }

        /** Get mod device's Product ID. This is assigned by the vendor */
        TextView tvPid = (TextView) findViewById(R.id.mod_status_pid);
        if (null != tvPid) {
            if (device == null
                    || device.getProductId() == Constants.INVALID_ID) {
                tvPid.setText(getString(R.string.na));
            } else {
                tvPid.setText(String.format(getString(R.string.mod_pid_vid_format),
                        device.getProductId()));
            }
        }

        /** Get mod device's version of the firmware */
        TextView tvFirmware = (TextView) findViewById(R.id.mod_status_firmware);
        if (null != tvFirmware) {
            if (null != device && null != device.getFirmwareVersion()
                    && !device.getFirmwareVersion().isEmpty()) {
                tvFirmware.setText(device.getFirmwareVersion());
            } else {
                tvFirmware.setText(getString(R.string.na));
            }
        }

        /**
         * Get the default Android application associated with the currently attached mod,
         * as read from the mod hardware manifest.
         */
        TextView tvPackage = (TextView) findViewById(R.id.mod_status_package_name);
        if (null != tvPackage) {
            if (device == null
                    || personality.getModManager() == null) {
                tvPackage.setText(getString(R.string.na));
            } else {
                if (personality.getModManager() != null) {
                    String modPackage = personality.getModManager().getDefaultModPackage(device);
                    if (null == modPackage || modPackage.isEmpty()) {
                        modPackage = getString(R.string.name_default);
                    }
                    tvPackage.setText(modPackage);
                }
            }
        }

        /**
         * Set Sensor Description text based on current state
         */
        TextView tvSensor = (TextView)findViewById(R.id.mod_status);
        if (tvSensor != null) {
            if (device == null) {
                tvSensor.setText(R.string.attach_pcard);
            } else if (device.getVendorId() == Constants.VID_DEVELOPER) {
                tvSensor.setText(R.string.sensor_description);
            } else if (device.getVendorId() == Constants.VID_MDK) {
                if (device.getProductId() == Constants.PID_WALKASSIST) {
                    tvSensor.setText(R.string.sensor_description);
                } else {
                    tvSensor.setText(R.string.mdk_switch);
                }
            } else if(device.getVendorId() == 0x00000128) {
                tvSensor.setText("\nMod adds a camera with optical zoom to the phone");
            } else {
                tvSensor.setText(getString(R.string.attach_pcard));
            }
        }
    }

    /** Check current mod whether in developer mode */
    private boolean isMDKMod(ModDevice device) {
        if (device == null) {
            /** Mod is not available */
            return false;
        } else if (device.getVendorId() == Constants.VID_DEVELOPER
                && device.getProductId() == Constants.PID_DEVELOPER) {
            // MDK in developer mode
            return true;
        } else {
            // Check MDK
            return device.getVendorId() == Constants.VID_MDK;
        }
    }

    /** Got data from mod device RAW I/O */
    public void onRawData(byte[] buffer, int length) {
        if (length == 2){
            byte[] bytes = { 0x00,0x00,buffer[1],buffer[0]};
            ByteBuffer wrapped = ByteBuffer.wrap(bytes);
            int integer = wrapped.getInt();
            if(vibrate){
                VibrationAssist.vibrateProximity(integer, getApplicationContext());
            } else {
                VibrationAssist.cancelVibration(getApplicationContext());
            }
            display.setText(Integer.toString(integer) + " cm");
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

    /** Handle the IO issue when write / read */
    public void onIOException() {
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

    /** Handle permission request result */
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == RAW_PERMISSION_REQUEST_CODE && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (null != personality) {
                    /** Permission grant, try to check RAW I/O of mod device */
                    personality.getRaw().checkRawInterface();
                }
            }  else if (requestCode == RC_VIBRATE){
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //do nothing
                } else {
                    Toast toast = Toast.makeText(this, "Vibrate permission is needed!", Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        } else {
                // TODO: user declined for RAW accessing permission.
                // You may need pop up a description dialog or other prompts to explain
                // the app cannot work without the permission granted.
        }

    }

    public void compareText(String speechRecognized){
        if(!speechRecognized.contains("desligar") && speechRecognized.contains("ligar vibração") || speechRecognized.contains("liga vibração")){
            toggleVibration(TOGGLE_ON);
            speechRecognizer.stopListening();
            bttnVoice.setBackgroundColor(Color.parseColor("#000000"));
        } else if(speechRecognized.contains("desligar vibração") || speechRecognized.contains("desliga vibração")){
            toggleVibration(TOGGLE_OFF);
            speechRecognizer.stopListening();
            bttnVoice.setBackgroundColor(Color.parseColor("#000000"));
        } else if(speechRecognized.contains("modo auxílio")){
            toggleVibration(TOGGLE_OFF);
            speechRecognizer.stopListening();
            startActivity(new Intent(this, BlindActivity.class));
        }
    }

    class listener implements RecognitionListener
    {
        public void onReadyForSpeech(Bundle params)
        {
        }
        public void onBeginningOfSpeech()
        {
            bttnVoice.setBackgroundColor(Color.parseColor("#00ff00"));
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
            display.setText(message);
        }
        public void onResults(Bundle results)
        {
            String str = new String();
            ArrayList data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            for (int i = 0; i < data.size(); i++)
            {
                str += data.get(i);
            }
            bttnVoice.setBackgroundColor(Color.parseColor("#000000"));
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
