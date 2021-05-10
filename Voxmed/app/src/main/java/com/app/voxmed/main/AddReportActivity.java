package com.app.voxmed.main;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.text.Layout;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.app.voxmed.R;
import com.app.voxmed.base.BaseActivity;
import com.app.voxmed.classes.AudioTrackPlayer;
import com.app.voxmed.classes.MessageDialogFragment;
import com.app.voxmed.classes.SpeechService;
import com.app.voxmed.classes.VoiceRecorder;
import com.app.voxmed.commons.Commons;
import com.app.voxmed.models.Report;

import java.io.File;

public class AddReportActivity extends BaseActivity implements MessageDialogFragment.Listener {

    private static final String FRAGMENT_MESSAGE_DIALOG = "message_dialog";

    private static final String STATE_RESULTS = "results";

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;

    private SpeechService mSpeechService;

    private VoiceRecorder mVoiceRecorder;
    private final VoiceRecorder.Callback mVoiceCallback = new VoiceRecorder.Callback() {

        @Override
        public void onVoiceStart() {
            showStatus(true);
            if (mSpeechService != null) {
                mSpeechService.startRecognizing(mVoiceRecorder.getSampleRate());
            }
        }

        @Override
        public void onVoice(byte[] data, int size) {
            if (mSpeechService != null) {
                mSpeechService.recognize(data, size);
            }
        }

        @Override
        public void onVoiceEnd() {
            showStatus(false);
            if (mSpeechService != null) {
                mSpeechService.finishRecognizing();
            }
        }

    };

    // Resource caches
    private int mColorHearing;
    private int mColorNotHearing;

    // View references
    private TextView mStatus;
    private TextView mText;

    private EditText resultBox;
    private ImageButton nextButton;
    private FrameLayout itemResultBox;
    private CheckBox correctedBox;

    AudioTrackPlayer audioTrackPlayer = null;
    File audioFile = null;

    private Button play, stop, record;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            mSpeechService = SpeechService.from(binder);
            mSpeechService.addListener(mSpeechServiceListener);
            mStatus.setVisibility(View.VISIBLE);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mSpeechService = null;
        }

    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_report);

        Commons.doctorReportEditF = false;

        play = (Button) findViewById(R.id.play);
        stop = (Button) findViewById(R.id.stop);
        record = (Button) findViewById(R.id.record);

        itemResultBox = (FrameLayout) findViewById(R.id.item_result);
        correctedBox = (CheckBox)findViewById(R.id.chk_corrected);

        final Resources resources = getResources();
        final Resources.Theme theme = getTheme();
        mColorHearing = ResourcesCompat.getColor(resources, R.color.status_hearing, theme);
        mColorNotHearing = ResourcesCompat.getColor(resources, R.color.status_not_hearing, theme);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        mStatus = (TextView) findViewById(R.id.status);
        mText = (TextView) findViewById(R.id.text);

        resultBox = (EditText)findViewById(R.id.resultBox);
        nextButton = (ImageButton) findViewById(R.id.nextButton);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(audioFile != null && resultBox.getText().toString().trim().length() > 0 && !stop.isEnabled()) {
                    Report report = new Report();
                    report.set_member_id(Commons.thisUser.get_idx());
                    report.set_audio_file(audioFile);
                    report.set_body(resultBox.getText().toString());
                    report.set_corrected(correctedBox.isChecked());
                    Commons.report = report;
                    Intent intent = new Intent(getApplicationContext(), AddReport2Activity.class);
                    startActivity(intent);
                }else if(mVoiceRecorder != null)showToast(getString(R.string.please_stop_recording));
                else if(audioTrackPlayer != null)showToast(getString(R.string.please_stop_playing));
                else showToast(getString(R.string.please_record_report));
            }
        });

        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Prepare Cloud Speech API
                bindService(new Intent(getApplicationContext(), SpeechService.class), mServiceConnection, BIND_AUTO_CREATE);

                // Start listening to voices
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED) {
                    startVoiceRecorder();
                } else if (ActivityCompat.shouldShowRequestPermissionRationale(AddReportActivity.this,
                        Manifest.permission.RECORD_AUDIO)) {
                    showPermissionMessageDialog();
                } else {
                    ActivityCompat.requestPermissions(AddReportActivity.this, new String[]{Manifest.permission.RECORD_AUDIO},
                            REQUEST_RECORD_AUDIO_PERMISSION);
                }
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop();
            }
        });

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playAudio();
            }
        });

    }

    public void back(View view){
        onBackPressed();
        stop();
    }

    @Override
    public void onResume() {
        super.onResume();

        resume();
    }

    private void resume(){

        setButtonStatus(record, true);
        setButtonStatus(stop, false);

        File file = new File(Environment.getExternalStorageDirectory(), "recording.pcm");
        if(file.exists()){
            audioFile = file;
            setButtonStatus(play,true);
        }
        else {
            audioFile = null;
            setButtonStatus(play,false);
        }

    }

    @Override
    protected void onStart() {
        super.onStart();

        setButtonStatus(record, true);
        setButtonStatus(stop, false);

        File file = new File(Environment.getExternalStorageDirectory(), "recording.pcm");
        if(file.exists()){
            audioFile = file;
            setButtonStatus(play,true);
        }
        else {
            audioFile = null;
            setButtonStatus(play,false);
        }

    }

    @Override
    protected void onStop() {
        // Stop listening to voice
        stop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stop();
    }

    private void stop(){
        stopVoiceRecorder();
        // Stop Cloud Speech API
        if(mSpeechService != null){
            mSpeechService.removeListener(mSpeechServiceListener);
            unbindService(mServiceConnection);
            mSpeechService = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (permissions.length == 1 && grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                startVoiceRecorder();
            } else {
                showPermissionMessageDialog();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void setButtonStatus(Button button, boolean status){
        button.setEnabled(status);
        if(status){
            button.setTextColor(getColor(R.color.white));
            button.setTypeface(Typeface.DEFAULT_BOLD);
        }else {
            button.setTextColor(getColor(R.color.button_unabled));
            button.setTypeface(Typeface.DEFAULT);
        }
    }

    private void startVoiceRecorder() {
        if (mVoiceRecorder != null) {
            mVoiceRecorder.stop();
        }
        mVoiceRecorder = new VoiceRecorder(mVoiceCallback);
        mVoiceRecorder.start();

        showToast(getString(R.string.recording_started));

        setButtonStatus(record, false);
        setButtonStatus(stop, true);
        setButtonStatus(play, false);

        resultBox.setText("");
        itemResultBox.setVisibility(View.VISIBLE);
    }

    private void stopVoiceRecorder() {
        if (mVoiceRecorder != null) {
            mVoiceRecorder.stop();
            mVoiceRecorder = null;
            showToast(getString(R.string.recording_stopped));
            itemResultBox.setVisibility(View.GONE);
        }else if(audioTrackPlayer != null){
            audioTrackPlayer.stop();
            audioTrackPlayer = null;
            showToast(getString(R.string.playing_stopped));
        }

        resume();
    }

    private void showPermissionMessageDialog() {
        MessageDialogFragment
                .newInstance(getString(R.string.permission_message))
                .show(getSupportFragmentManager(), FRAGMENT_MESSAGE_DIALOG);
    }

    private void showStatus(final boolean hearingVoice) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mStatus.setTextColor(hearingVoice ? mColorHearing : mColorNotHearing);
            }
        });
    }

    @Override
    public void onMessageDialogDismissed() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                REQUEST_RECORD_AUDIO_PERMISSION);
    }

    private final SpeechService.Listener mSpeechServiceListener =
            new SpeechService.Listener() {
                @Override
                public void onSpeechRecognized(final String text, final boolean isFinal) {
                    if (isFinal) {
                        mVoiceRecorder.dismiss();
                    }
                    if (mText != null && !TextUtils.isEmpty(text)) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (isFinal) {
                                    mText.setText(null);
                                    resultBox.setText(resultBox.getText().toString() + (resultBox.getText().toString().length() == 0? "":" ") + text);
                                } else {
                                    mText.setText(text);
                                }
                            }
                        });
                    }
                }
            };

    private void playAudio(){
        if(audioFile != null){
            showToast(getString(R.string.audio_playing));
            audioTrackPlayer = new AudioTrackPlayer();
            audioTrackPlayer.prepare(audioFile.getAbsolutePath());
            audioTrackPlayer.play();
            setButtonStatus(play, false);
            setButtonStatus(stop, true);
            setButtonStatus(record, false);
        }
    }

}




















































