package sample.chloe.theremin2;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.app.Activity;

/**
 * Created by Chloe on 11/1/2015.
 */

//Copyright (c) Microsoft Corporation All rights reserved.
//
//MIT License:
//
//Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
//documentation files (the  "Software"), to deal in the Software without restriction, including without limitation
//the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
//to permit persons to whom the Software is furnished to do so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in all copies or substantial portions of
//the Software.
//
//THE SOFTWARE IS PROVIDED ""AS IS"", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
//TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
//THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
//CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
//IN THE SOFTWARE.
//package com.microsoft.band.sdk.sampleapp.accelerometer;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.BandIOException;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.sensors.BandAccelerometerEvent;
import com.microsoft.band.sensors.BandAccelerometerEventListener;
import com.microsoft.band.sensors.SampleRate;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.app.Activity;
import android.os.AsyncTask;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class Start extends Activity {

    private BandClient client = null;
    private Button btnStart;
    private Button backButton;
    private TextView txtStatus;
    private TextView noteStatus;

    private final int duration = 90; // seconds
    private final int sampleRate = 8000;
    private final int numSamples = duration * sampleRate;
    private final double sample[] = new double[numSamples];
    private final double baseAccelerationZ = 1.00;
    double accelerationZ;
    private final double baseAccelerationY = 0.00;
    double accelerationY;
    private double freqOfTone = 261.63;

    private final byte generatedSnd[] = new byte[2 * numSamples];
    boolean isPlaying = false;

    Handler handler = new Handler();
    AudioTrack audioTrack;
    boolean once = true;

    void genTone(double freq){
        // fill out the array
        for (int i = 0; i < numSamples; ++i) {
            sample[i] = Math.sin(2 * Math.PI * i / (sampleRate/freq));
        }

        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        int idx = 0;
        for (final double dVal : sample) {
            // scale to maximum amplitude
            final short val = (short) ((dVal * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);

        }
    }

    void playSound(){
        int x;
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length,
                AudioTrack.MODE_STATIC);
        audioTrack.write(generatedSnd, 0, generatedSnd.length);
        audioTrack.play();
        do{                                                     // Wait until sound is complete
            x = audioTrack.getPlaybackHeadPosition();
        }while (x < generatedSnd.length / 2);
    }


    private BandAccelerometerEventListener mAccelerometerEventListener = new BandAccelerometerEventListener() {
        @Override
        public void onBandAccelerometerChanged(final BandAccelerometerEvent event) {
            if (event != null) {

                if(event.getAccelerationY() > 0.1) {
                    //appendToUI(String.format(" Note = %.3f \n, Y = %.3f \n", (268.69 * Math.pow(Math.E, (0.527 * (event.getAccelerationY() - 0.1)))), event.getAccelerationY()));
                    audioTrack.setPlaybackRate((int)(8000*(( 1.1111 *(event.getAccelerationY()) )+ 0.8889)));
                    printNotePlayed(getNote(( 1.1111 *(event.getAccelerationY()))+ 0.8889));
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start);
        final Context context = this;

        noteStatus = (TextView) findViewById(R.id.notePlayed);
        txtStatus = (TextView) findViewById(R.id.txtStatus);
        backButton = (Button) findViewById(R.id.button3);
        btnStart = (Button) findViewById(R.id.btnStart);
        btnStart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                txtStatus.setText("");
                noteStatus.setText("");
                new AccelerometerSubscriptionTask().execute();
            }
        });
        backButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                audioTrack.stop();
                Intent intent = new Intent(context, MainActivity.class);
                startActivity(intent);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        txtStatus.setText("");

        // Use a new tread as this can take a while
        final Thread thread = new Thread(new Runnable() {
            public void run() {
                //genTone(523.25);
                if(once) {
                    genTone(freqOfTone);
                }
                playSound();
                handler.post(new Runnable() {
                    public void run() {
                        //playSound();
                    }
                });
            }
        });
        thread.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        audioTrack.pause();
        if (client != null) {
            try {
                client.getSensorManager().unregisterAccelerometerEventListener(mAccelerometerEventListener);
            } catch (BandIOException e) {
                appendToUI(e.getMessage());
            }
        }
    }

    private class AccelerometerSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    appendToUI("Band is connected.\n");
                    client.getSensorManager().registerAccelerometerEventListener(mAccelerometerEventListener, SampleRate.MS128);
                } else {
                    appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
                String exceptionMessage="";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                        break;
                }
                appendToUI(exceptionMessage);

            } catch (Exception e) {
                appendToUI(e.getMessage());
            }
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        if (client != null) {
            try {
                client.disconnect().await();
            } catch (InterruptedException e) {
                // Do nothing as this is happening during destroy
            } catch (BandException e) {
                // Do nothing as this is happening during destroy
            }
        }
        super.onDestroy();
    }

    private void appendToUI(final String string) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtStatus.setText(string);
            }
        });
    }

    private void printNotePlayed(final String string) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                noteStatus.setText(string);
            }
        });
    }

    private boolean getConnectedBandClient() throws InterruptedException, BandException {
        if (client == null) {
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) {
                appendToUI("Band isn't paired with your phone.\n");
                return false;
            }
            client = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
        } else if (ConnectionState.CONNECTED == client.getConnectionState()) {
            return true;
        }

        appendToUI("Band is connecting...\n");
        return ConnectionState.CONNECTED == client.connect().await();
    }

    private String getNote(double scale) {

        if(1 > scale && scale < 1.0833)
            return "C";
        if(1.0833 > scale && scale < 1.1666)
            return "C#";
        if(1.1666 > scale && scale < 1.2499)
            return "D";
        if(1.2499 > scale && scale < 1.3332)
            return "D#";
        if(1.3332 > scale && scale < 1.4165)
            return "E";
        if(1.4165 > scale && scale < 1.5)
            return "F";
        if(1.5 > scale && scale < 1.5833)
            return "F#";
        if(1.5833 > scale && scale < 1.6666)
            return "G";
        if(1.6666 > scale && scale < 1.7499)
            return "G#";
        if(1.7499 > scale && scale < 1.8332)
            return "A";
        if(1.8332 > scale && scale < 1.9165)
            return "A#";
        if(1.9165 > scale && scale < 2)
            return "B";
        if(2 > scale && scale < 4)
            return "C5";
        if(scale > 2)
            return"C5";

        //appendToUI(String.format("%f\n", scale));
        return "Note not found";
    }
}



