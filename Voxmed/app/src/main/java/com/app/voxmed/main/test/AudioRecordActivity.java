package com.app.voxmed.main.test;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.app.voxmed.R;
import com.app.voxmed.base.BaseActivity;
import com.app.voxmed.commons.Commons;

import java.io.IOException;
import java.util.Date;

public class AudioRecordActivity extends BaseActivity {

    private Button play, stop, record;
    public EditText resultBox;
    private MediaRecorder myAudioRecorder;
    private String outputFile;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_record);

        play = (Button) findViewById(R.id.play);
        stop = (Button) findViewById(R.id.stop);
        record = (Button) findViewById(R.id.record);
        stop.setEnabled(false);
        play.setEnabled(false);

        resultBox = (EditText)findViewById(R.id.resultBox);

        outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/audio_reports_" + new Date().getTime() + ".3gp";

        myAudioRecorder = new MediaRecorder();
        myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        myAudioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
        myAudioRecorder.setOutputFile(outputFile);

        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    myAudioRecorder.prepare();
                    myAudioRecorder.start();
                } catch (IllegalStateException ise) {
                    // make something ...
                    ise.printStackTrace();
                } catch (IOException ioe) {
                    // make something
                    ioe.printStackTrace();
                }
                record.setEnabled(false);
                stop.setEnabled(true);
                showToast(getString(R.string.recording_started));
//                Intent intent = new Intent(getApplicationContext(), AddReportActivity.class);
//                startActivity(intent);
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    myAudioRecorder.release();
                    myAudioRecorder = null;
                }catch (IllegalStateException ise) {
                    // make something ...
                    ise.printStackTrace();
                }catch (Exception e){
                    e.printStackTrace();
                }
                record.setEnabled(true);
                stop.setEnabled(false);
                play.setEnabled(true);
                showToast(getString(R.string.recording_stopped));
            }
        });

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaPlayer mediaPlayer = new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(outputFile);
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                    showToast(getString(R.string.audio_playing));
                } catch (Exception e) {
                    // make something
                }
            }
        });

    }

    public void stopRecording(){
        try{
            myAudioRecorder.release();
            myAudioRecorder = null;
        }catch (IllegalStateException ise) {
            // make something ...
            ise.printStackTrace();
        }catch (Exception e){
            e.printStackTrace();
        }
        record.setEnabled(true);
        stop.setEnabled(false);
        play.setEnabled(true);
        showToast(getString(R.string.recording_stopped));
    }

    public void back(View view){
        onBackPressed();
    }

}





























