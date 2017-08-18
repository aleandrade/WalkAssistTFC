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

package com.motorola.samples.mdksensor;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.motorola.mod.ModDevice;
import com.motorola.mod.ModManager;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;

/**
 * A class to represent main activity.
 */
public class MainActivity extends Activity {
    public static final String MOD_UID = "mod_uid";

    private static final int RAW_PERMISSION_REQUEST_CODE = 100;

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
                    Log.i(Constants.TAG, "MainActivity - Un-handle events: " + msg.what);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setActionBar(toolbar);


        if (null == personality || null == personality.getModDevice()) {
            Toast.makeText(MainActivity.this, getString(R.string.sensor_not_available),
                    Toast.LENGTH_SHORT).show();
            //buttonView.setChecked(false);
            return;
        }

        if (personality.getModDevice().getVendorId() != Constants.VID_DEVELOPER
                && !(personality.getModDevice().getVendorId() == Constants.VID_MDK
                && personality.getModDevice().getProductId() == Constants.PID_TEMPERATURE)) {
            Toast.makeText(MainActivity.this, getString(R.string.sensor_not_available),
                    Toast.LENGTH_SHORT).show();
            //buttonView.setChecked(false);
            return;
        }

        /** Write RAW command to toggle mdk temperature sensor on mod device */
        //if (isChecked) {
            String[] values = getResources().getStringArray(R.array.sensor_interval_values);
            int interval = Integer.valueOf(0);//CHANGED
            byte intervalLow = (byte) (interval & 0x00FF);
            byte intervalHigh = (byte) (interval >> 8);
            byte[] cmd = {Constants.TEMP_RAW_COMMAND_ON, Constants.SENSOR_COMMAND_SIZE,
                    intervalLow, intervalHigh};
            personality.getRaw().executeRaw(cmd);

            Toast.makeText(MainActivity.this, getString(R.string.sensor_start),
                    Toast.LENGTH_SHORT).show();
//        } else {
//            personality.getRaw().executeRaw(Constants.RAW_CMD_STOP);
//
//            Toast.makeText(MainActivity.this, getString(R.string.sensor_stop),
//                    Toast.LENGTH_SHORT).show();
//        }

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

                if ((device.getVendorId() == Constants.VID_MDK
                        && device.getProductId() == Constants.PID_TEMPERATURE)
                        || device.getVendorId() == Constants.VID_DEVELOPER) {
                    tvName.setTextColor(getColor(R.color.mod_match));
                } else {
                    tvName.setTextColor(getColor(R.color.mod_mismatch));
                }
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
                if (device.getProductId() == Constants.PID_TEMPERATURE) {
                    tvSensor.setText(R.string.sensor_description);
                } else {
                    tvSensor.setText(R.string.mdk_switch);
                }
            } else {
                tvSensor.setText(getString(R.string.attach_pcard));
            }
        }

        /**
         * Disable sampling toggle button here. If attached mod passed command
         * challenge, the toggle button will be enabled. Refer to handler of
         * Constants.TEMP_RAW_COMMAND_CHLGE_RESP in parseResponse().
         */
//        Switch switcher = (Switch) findViewById(R.id.sensor_switch);
//        if (switcher != null) {
//            switcher.setEnabled(false);
//
//            /** Reset Temperature switch button to off if mod detach */
//            if (device == null) {
//                if (switcher.isChecked()) {
//                    switcher.setChecked(false);
//                }
//            }
//        }
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
        /** Parse raw data to header and payload */
        int cmd = buffer[Constants.CMD_OFFSET] & ~Constants.TEMP_RAW_COMMAND_RESP_MASK & 0xFF;
        int payloadLength = buffer[Constants.SIZE_OFFSET];

        /** Checking the size of buffer we got to ensure sufficient bytes */
        if (payloadLength + Constants.CMD_LENGTH + Constants.SIZE_LENGTH != length) {
            return;
        }

        /** Parser payload data */
        byte[] payload = new byte[payloadLength];
        System.arraycopy(buffer, Constants.PAYLOAD_OFFSET, payload, 0, payloadLength);
        parseResponse(cmd, payloadLength, payload);
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
            } else {
                // TODO: user declined for RAW accessing permission.
                // You may need pop up a description dialog or other prompts to explain
                // the app cannot work without the permission granted.
            }
        }
    }

    /** Parse the data from mod device */
    private void parseResponse(int cmd, int size, byte[] payload) {
        if (cmd == Constants.TEMP_RAW_COMMAND_INFO) {
            /** Got information data from personality board */

            /**
             * Checking the size of payload before parse it to ensure sufficient bytes.
             * Payload array shall at least include the command head data, and exactly
             * same as expected size.
             */
            if (payload == null
                    || payload.length != size
                    || payload.length < Constants.CMD_INFO_HEAD_SIZE) {
                return;
            }

            int version = payload[Constants.CMD_INFO_VERSION_OFFSET];
            int reserved = payload[Constants.CMD_INFO_RESERVED_OFFSET];
            int latencyLow = payload[Constants.CMD_INFO_LATENCYLOW_OFFSET] & 0xFF;
            int latencyHigh = payload[Constants.CMD_INFO_LATENCYHIGH_OFFSET] & 0xFF;
            int max_latency = latencyHigh << 8 | latencyLow;

            StringBuilder name = new StringBuilder();
            for (int i = Constants.CMD_INFO_NAME_OFFSET; i < size - Constants.CMD_INFO_HEAD_SIZE; i++) {
                if (payload[i] != 0) {
                    name.append((char) payload[i]);
                } else {
                    break;
                }
            }
            Log.i(Constants.TAG, "command: " + cmd
                    + " size: " + size
                    + " version: " + version
                    + " reserved: " + reserved
                    + " name: " + name.toString()
                    + " latency: " + max_latency);
        } else if (cmd == Constants.TEMP_RAW_COMMAND_DATA) {
            /** Got sensor data from personality board */

            /** Checking the size of payload before parse it to ensure sufficient bytes. */
            if (payload == null
                    || payload.length != size
                    || payload.length != Constants.CMD_DATA_SIZE) {
                return;
            }

            int dataLow = payload[Constants.CMD_DATA_LOWDATA_OFFSET] & 0xFF;
            int dataHigh = payload[Constants.CMD_DATA_HIGHDATA_OFFSET] & 0xFF;

            /** The raw temperature sensor data */
            int data = dataHigh << 8 | dataLow;

            /** The temperature */
            double temp = ((0 - 0.03) * data) + 128;

            /** Draw temperature value to line chart */
            count++;
            Line line = chart.getLineChartData().getLines().get(0);
            if (null != line) {
                if (count > Constants.MAX_SAMPLING_SUM
                        && line.getValues() != null
                        && line.getValues().size() > 0) {
                    line.getValues().remove(0);
                }

                line.getValues().add(new PointValue(count, (float) temp));
                chart.animationDataUpdate(1);

                if (temp * 1.01f > maxTop) {
                    maxTop = (float) temp * 1.01f;
                }
                if (temp * 0.99f < minTop) {
                    minTop = (float) temp * 0.99f;
                }
                viewPort = chart.getMaximumViewport();
                viewPort.top = maxTop; //max value
                viewPort.bottom = minTop;  //min value
                chart.setMaximumViewport(viewPort);
                chart.setCurrentViewport(viewPort);
            }
        }
    }
}
