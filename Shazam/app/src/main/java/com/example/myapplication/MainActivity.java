package com.example.myapplication;

import android.annotation.SuppressLint;
import android.app.Activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.Console;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class MainActivity extends Activity {
    private static final int REQUEST_PERMISSION_CODE = 1000;
    private boolean isRecording = false;
    private MediaRecorder mediaRecorder;
    private String outputFile;
    private int bufferSize;
    private Thread recordingThread;
    private AudioRecord audioRecord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Button btnRecord = findViewById(R.id.btnRecord);

        // Request necessary permissions
        if (!checkPermissionFromDevice()) {
            requestPermissions();
        }

        // Set up recording functionality
        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRecording) {
                    startRecording();
                    btnRecord.setText("Stop");
                } else {
                    stopRecording();
                    btnRecord.setText("Record");
                }
            }
        });
    }

    // Check if required permissions are granted
    private boolean checkPermissionFromDevice() {
        int audioPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        int storagePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return audioPermission == PackageManager.PERMISSION_GRANTED && storagePermission == PackageManager.PERMISSION_GRANTED;
    }

    // Request required permissions
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_CODE);
    }

    // Handle permission request result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with the app
            } else {
                // Permission denied, handle accordingly
            }
        }
    }

    // Start recording
    @SuppressLint("MissingPermission")
    private void startRecording() {
        isRecording = true;
        outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording.pcm"; // Change the file extension if needed

        int audioSource = MediaRecorder.AudioSource.MIC;
        int sampleRate = 44100;  // You can adjust this based on your requirements
        int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;  // Adjust as needed

        bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);


        audioRecord = new AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, bufferSize);

        audioRecord.startRecording();

        Thread recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");

        recordingThread.start();
    }

    // Stop recording
    private void stopRecording() {
        isRecording = false;

        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

    // Write audio data to file
    private void writeAudioDataToFile() {
        byte[] data = new byte[bufferSize];
        FileOutputStream os = null;

        try {
            os = new FileOutputStream(outputFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        if (null != os) {
            while (isRecording) {
                int read = audioRecord.read(data, 0, bufferSize);
                Log.d("Audio Record: ", Arrays.toString(data));
                if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                    try {
                        os.write(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
