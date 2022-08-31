package com.example.volumecontroller;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.ConsumerIrManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

// TODO settings page
/*
choose:
    device type {
        TV
            PANASONIC  {"Protocol":"PANASONIC","Bits":48,"Data":0x100BCBD}
                0x400401000405="Vol+"
                0x400401008485="Vol-"

            IR Remote Codes Sony KDL-EX540~
                Vol + {"Protocol":"SONY","Bits":12,"Data":"0x490"}
                Vol - {"Protocol":"SONY","Bits":12,"Data":"0xC90"}

            Generic VEON TV (eg model SRO322016)
                Vol+	{"Protocol":"NEC","Bits":32,"Data":"0x00FED827"}
                Vol-	{"Protocol":"NEC","Bits":32,"Data":"0x00FE58A7"}

        Set-top Boxes
            IR Codes for VU+ Duo2 {"Protocol":"RC6","Bits":36,"Data":0x8052900C}
                0xC80529010="Vol+"
                0xC80521011="Vol-"

            IR Remote Codes AppleTV Gen4~
                Up {"Protocol":"NEC","Bits":32,"Data":"0x77E15080"}
                Down {"Protocol":"NEC","Bits":32,"Data":"0x77E13080"}

            IR Remote Codes Humax HMS-1000T DVB-T2 DVR PAL 4-Tune
                Vol + {"Protocol":"NEC","Bits":32,"Data":"0x0008F807"}
                Vol - {"Protocol":"NEC","Bits":32,"Data":"0x000802FD"}

            IR Remote Codes FetchTV Mini(Hybroad H626T)
                Up {"Protocol":"NEC","Bits":32,"Data":"0x26629B64"}
                Down {"Protocol":"NEC","Bits":32,"Data":"0x266223DC"}

            Smart Receiver VX/CX
                volume + {"Protocol":"NEC","Bits":32,"Data":"0x00FF5AA5","DataLSB":"0x00FF5AA5","Repeat":0}
                volume - {"Protocol":"NEC","Bits":32,"Data":"0x00FFDA25","DataLSB":"0x00FF5BA4","Repeat":0}

        BD/DVD players
            ä IR Remote Codes Sony BD-S1500
                Vol + {"Protocol":"SONY","Bits":12,"Data":"0x490"}
                Vol - {"Protocol":"SONY","Bits":12,"Data":"0xC90"}

        Projectors
            IR Remote Codes Acer K132
                * Vol+  {"Protocol":"NEC","Bits":32,"Data":"0x10C8C639"}
                * Vol-  {"Protocol":"NEC","Bits":32,"Data":"0x10C826D9"}

        Soundbars
            IR Codes Soundbar Panasonic SCALL70T {"Protocol":"PANASONIC","Bits":48,"Data":0x40040500BCB9}
                0x400405000401="Vol+"
                0x400405008481="Vol-"

            IR Codes Soundcore Infini Pro {"Protocol":"NEC","Bits":32,"Data":"0xFD256897"}
                0xFD256897="Vol+"
                0xFD2558A7="Vol-"

            IR Codes Goodmans GDSBT1000P {"Protocol":"NEC","Bits":32,"Data":"0x4FB30CF"}
                0x20DF02FD="Volume +"
                0x20DF05FA="Volume -"
    }
    interval
    frequency
    range
        up range
        down range
 */

// TODO control algorithm
/*
time variable range
 */
public class MainActivity extends AppCompatActivity {

    // edit text parameters
    private TextView stateElement;
    private Button startElement;
    private TextView rawVolumeElement;
    private TextView avgVolumeElement;
    private EditText optVolumeElement;


    // record audio parameters
    MediaRecorder mediaRecorder;
    private boolean recordingState = false;
    private String filename;

    // send IR signal parameters
    private final int patternFrequency = 33000;
    private final int[] patternUP = {9000, 4500,550, 600, 550, 600, 550, 1700, 550, 600, 550, 600, 550, 600, 550, 600, 550, 600,550, 1700, 550, 1700, 550, 600, 550, 1700, 550, 1700, 550, 1700, 550,1700, 550, 1700,
                                550, 600, 550, 1700, 550, 600, 550, 600, 550, 600, 550, 600, 550, 600, 550, 600,550, 1700, 550, 600, 550, 1700, 550, 1700, 550, 1700, 550, 1700, 550, 1700, 550, 1700,600};
    private final int[] patternDOWN = {9000, 4500, 550, 600, 550, 600, 550, 1700, 550, 600, 550, 600, 550, 600,550, 600, 550, 600, 550, 1700, 550, 1700, 550, 600, 550, 1700, 550, 1700, 550, 1700, 550, 1700, 550,
                                    1700,550, 1700, 550, 1700, 550, 600, 550, 600, 550, 600, 550, 600, 550, 600, 550, 600, 550, 600, 550, 600, 550, 1700, 550, 1700, 550, 1687, 550, 1700, 550, 1700, 550, 1700, 600};
    Timer controllerTimer;

    // controller parameters
    private double optimalVolume;
    private int interval = 50;
    private double frequency = 2;
    private double range = 5;
    private boolean customRange = false;
    private double rangeTop = 5;
    private double rangeBottom = 5;

    private int counter;
    private boolean loadingState = true;

    // calculate average parameters
    private double[] levels;
    private double total;
    private int index;
    private double volumeLevel;

    private static final DecimalFormat df = new DecimalFormat("#.##");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // check if permission
        if (isMicrophonePresent()) getMicrophonePermission();

        // get view elements
        stateElement = findViewById(R.id.state);
        startElement = findViewById(R.id.start);
        rawVolumeElement = findViewById(R.id.rawVolume);
        avgVolumeElement = findViewById(R.id.avgVolume);
        optVolumeElement = findViewById(R.id.optVolume);

        // check if IR emitter exists
        checkIR();
    }

    public void mainStart(View view) {
        if (!recordingState) {
            startRecording();
            startController();
        } else {
            stopController();
            stopRecording();
        }
    }


    private void startRecording() {
        // get file name and path
        String filepath = getRecordingFilePath();

        try {
            // start recorder
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setOutputFile(filepath);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.prepare();
            mediaRecorder.start();

            // set initial values
            recordingState = true;
            stateElement.setText(R.string.startedStateText);
            startElement.setText(R.string.stopButtonText);
            //Toast.makeText(this, "Recording has started", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getRecordingFilePath() {
        Random rand = new Random();
        int upperbound = 10000;
        String int_random = String.valueOf(rand.nextInt(upperbound));
        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File musicDirectory = contextWrapper.getExternalFilesDir((Environment.DIRECTORY_MUSIC));
        filename = "RF" + int_random + ".mp3";
        File file = new File(musicDirectory, filename);
        return file.getPath();
    }


    private void stopRecording() {
        // stop recorder
        mediaRecorder.stop();
        mediaRecorder.release();
        mediaRecorder = null;

        // reset values
        recordingState = false;
        stateElement.setText(R.string.stoppedStateText);
        startElement.setText(R.string.startButtonText);
        rawVolumeElement.setText(R.string.defaultRaw);
        avgVolumeElement.setText(R.string.defaultAvg);
        //Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show();

        deleteRecording();
    }

    private void deleteRecording() {
        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File musicDirectory = contextWrapper.getExternalFilesDir((Environment.DIRECTORY_MUSIC));
        File file = new File(musicDirectory, filename);
        boolean deleted = file.delete();
        //if (deleted) Toast.makeText(this, "File deleted", Toast.LENGTH_SHORT).show();
    }


    private void startController() {
        // set-up and get settings values
        optimalVolume = getDouble(optimalVolume, optVolumeElement);
        Intent intent = getIntent();
        interval = intent.getIntExtra("interval", 50);
        frequency = intent.getDoubleExtra("frequency", 2.0);
        range = intent.getDoubleExtra("range", 5.0);
        customRange = intent.getBooleanExtra("custom", false);
        rangeTop = intent.getDoubleExtra("topRange", 5.0);
        rangeBottom = intent.getDoubleExtra("botRange", 5.0);

        // reset controller values
        levels = new double[interval];
        Arrays.fill(levels, 0);
        total = 0;
        index = 0;
        volumeLevel = 0;
        counter = 0;

        // start timer
        controllerTimer = new Timer();
        controllerTimer.scheduleAtFixedRate(
                new TimerTask() {
                    public void run() {
                        control(calculateAvg());
                    }
                },
                0,
                100);
    }

    private double getDouble(double prev, EditText element) {
        String prov = String.valueOf(element.getText());
        if (prov.trim().length() != 0) { // check if empty
            if (Double.parseDouble(prov) > 0) { // check if 0
                return Double.parseDouble(prov); // return new one
            }
        }
        return prev; // return old one
    }

    // TODO improve algorithm
    private void control(double volume) {

        optimalVolume = getDouble(optimalVolume, optVolumeElement);
        if (!customRange) {
            rangeTop = range;
            rangeBottom = range;
        }

        if (!loadingState) {
            counter++;
            if (counter > interval / frequency) {
                if (volume > optimalVolume + rangeTop) {
                    volumeDown(null);
                    wait(200);
                } else if (volume < optimalVolume - rangeBottom) {
                    volumeUp(null);
                    wait(250);
                }
                counter = 0;
            }
        }
    }

    private double calculateAvg() {
        total = total - levels[index]; // subtract from total
        levels[index] = getVolume(); // get new value
        total = total + levels[index]; // add new value to the total
        index = index + 1; // increase index
        if (index >= interval) {
            index = 0; // reset index
            loadingState = false; // check if first cycle
        }
        volumeLevel = total / interval; // calculate average
        avgVolumeElement.setText(df.format(volumeLevel));
        return volumeLevel;
    }

    private double getVolume() {
        int BASE = 1;
        double db = 0;

        if (mediaRecorder != null) {
            double ratio = (double) mediaRecorder.getMaxAmplitude() / BASE;
            if (ratio > 1) {
                db = 20 * Math.log10(ratio);
                rawVolumeElement.setText(df.format(db));
            }
        }
        return db;
    }


    private void stopController() {
        //reset values
        avgVolumeElement.setText(R.string.defaultAvg);
        rawVolumeElement.setText(R.string.defaultRaw);

        //stop timer
        controllerTimer.cancel();
        controllerTimer.purge();
        loadingState = false; // reset first cycle
    }

    //settings
    public void openSettings(View view) {
        if (recordingState) {
            stopController();
            stopRecording();
            recordingState = false;
        }

        // create element with current settings values as parameters
        final SettingsSheet bottomSheet = new SettingsSheet(interval, frequency, range, customRange, rangeTop, rangeBottom);
        bottomSheet.show(getSupportFragmentManager(), "TAG");
    }


    //utils
    public void volumeUp(View view) {
        ConsumerIrManager transmitterUP = (ConsumerIrManager) getSystemService(CONSUMER_IR_SERVICE);
        transmitterUP.transmit(patternFrequency, patternUP);
    }

    public void volumeDown(View view) {
        ConsumerIrManager transmitterDown = (ConsumerIrManager) getApplicationContext().getSystemService(Context.CONSUMER_IR_SERVICE);
        transmitterDown.transmit(patternFrequency, patternDOWN);
    }

    public void optimalUp(View view) {
        int val = Integer.parseInt(String.valueOf(optVolumeElement.getText())); // get current
        val++; // increase current by one
        optVolumeElement.setText(String.valueOf(val)); // set edited one to view
    }

    public void optimalDown(View view) {
        int val = Integer.parseInt(String.valueOf(optVolumeElement.getText())); // get current
        val--; // decrease current by one
        optVolumeElement.setText(String.valueOf(val)); // set edited one to view
    }

    private void checkIR() {
        ConsumerIrManager transmitterCheck = (ConsumerIrManager) getSystemService(CONSUMER_IR_SERVICE);
        boolean feature_consumer_ir = transmitterCheck.hasIrEmitter();
        if (feature_consumer_ir) {
            Toast.makeText(this, "Has IR Hardware Component", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Doesn't have IR Hardware Component", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isMicrophonePresent() {
        return this.getPackageManager().hasSystemFeature((PackageManager.FEATURE_MICROPHONE));
    }

    private void getMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_DENIED) {
            int MICROPHONE_PERMISSION_CODE = 200;
            ActivityCompat.requestPermissions(this, new String[]
                    {Manifest.permission.RECORD_AUDIO}, MICROPHONE_PERMISSION_CODE);
        }
    }

    private void wait(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}


