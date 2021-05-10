package com.app.voxmed.main;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.app.voxmed.R;
import com.app.voxmed.base.BaseActivity;
import com.app.voxmed.classes.AudioTrackPlayer;
import com.app.voxmed.classes.MessageDialogFragment;
import com.app.voxmed.classes.SpeechService;
import com.app.voxmed.classes.VoiceRecorder;
import com.app.voxmed.commons.Commons;
import com.app.voxmed.commons.ReqConst;
import com.app.voxmed.models.Field;
import com.app.voxmed.models.Report;
import com.wang.avi.AVLoadingIndicatorView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

public class AddNewReportActivity extends BaseActivity implements MessageDialogFragment.Listener {

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
    private LinearLayout container;
    private TextView addButton;
    private ImageButton nextButton;
    private FrameLayout itemResultBox;
    private CheckBox correctedBox;
    private AVLoadingIndicatorView progressBar;

    AudioTrackPlayer audioTrackPlayer = null;
    File audioFile = null;

    private Button play, stop, record;
    ArrayList<Field> fields = new ArrayList<>();

    String str = "";

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
        setContentView(R.layout.activity_new_report);

        Commons.doctorReportEditF = false;

        progressBar = (AVLoadingIndicatorView)findViewById(R.id.loading_bar);

        resultBox = (EditText)findViewById(R.id.resultBox);

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

        container = (LinearLayout) findViewById(R.id.container);
        addButton = (TextView) findViewById(R.id.btn_add);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                templateSetting();
            }
        });

        nextButton = (ImageButton) findViewById(R.id.nextButton);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(audioFile != null && fields.size() > 0 && !stop.isEnabled()) {
                    try {
                        String jsonStr = createTemplateJsonString();
                        if(jsonStr.length() > 0){
                            Report report = new Report();
                            report.set_member_id(Commons.thisUser.get_idx());
                            report.set_audio_file(audioFile);
                            report.set_body(fields.get(fields.size() - 1).getContent());
//                            String[] splitStr = resultBox.getText().toString().trim().split("\\s+");
//                            String sss = "";
//                            for(String string:splitStr){
//                                Log.d("String", string);
//                                if(string.endsWith(":")){
//                                    int i = 0;
//                                    for(Field field:fields){
//                                        i++;
//                                        if(string.toLowerCase().replace(":","").equals(field.getTitle().toLowerCase())){
//                                            string = "<br><br><b>" + string + "</b><br>";
//                                            sss = sss + string;
//                                            break;
//                                        }else {
//                                            if(i == fields.size()){
//                                                sss = sss + (sss.length() == 0?"":" ") + string;
//                                            }
//                                        }
//                                    }
//                                }else {
//                                    sss = sss + (sss.length() == 0?"":" ") + string;
//                                }
//                            }
//                            report.set_body(sss);
                            report.set_corrected(correctedBox.isChecked());
                            Commons.report = report;
                            Commons.fields.clear();
                            Commons.fields.addAll(fields);
                            Intent intent = new Intent(getApplicationContext(), AddReport2Activity.class);
                            intent.putExtra("jsonStr", jsonStr);
                            startActivity(intent);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
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
                } else if (ActivityCompat.shouldShowRequestPermissionRationale(AddNewReportActivity.this,
                        Manifest.permission.RECORD_AUDIO)) {
                    showPermissionMessageDialog();
                } else {
                    ActivityCompat.requestPermissions(AddNewReportActivity.this, new String[]{Manifest.permission.RECORD_AUDIO},
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

    ArrayList<View> views = new ArrayList<>();

    private void templateSetting(){
        Intent intent = new Intent(getApplicationContext(), TemplateListActivity.class);
        startActivity(intent);
    }

    public void back(View view){
        onBackPressed();
        stop();
    }

    @Override
    public void onResume() {
        super.onResume();

        resume();
        getAllKeywords();
        if(Commons.template != null){
            getKeywords();
        }
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

//        resultBox.setText("");
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

                                    String result = text;
                                    if(text.toLowerCase().equals(getString(R.string.new_line))) result = "\n";
                                    else if(text.toLowerCase().equals(getString(R.string.point))) result = ". ";
                                    else if(text.toLowerCase().equals(getString(R.string.comma))) result = ", ";
                                    else if(text.toLowerCase().equals(getString(R.string.space))) result = " ";
//                                    else if(text.toLowerCase().equals(getString(R.string.next_))) result = " ";

                                    int i = 0;

//                                    for(Field field:fields){
//                                        i++;
//                                        if(text.toLowerCase().equals(field.getTitle().toLowerCase())){
//                                            str = str + (resultBox.getText().toString().length() == 0? "":"<br><br>") + "<b>" + capitalize(result) + ":</b><br>";
//                                            resultBox.setText(Html.fromHtml(str));
//                                            resultBox.setSelection(resultBox.getText().length());
//                                            break;
//                                        }else {
//                                            if(i == fields.size()){
//                                                str = str + (resultBox.getText().toString().length() == 0? "":" ") + result;
//                                                resultBox.setText(Html.fromHtml(str));
//                                                resultBox.setSelection(resultBox.getText().length());
//                                            }
//                                        }
//                                    }

                                    for (View view : views){

                                        i++;

                                        TextView titleBox = (TextView)view.findViewById(R.id.titleBox);
                                        EditText contentBox = (EditText)view.findViewById(R.id.contentBox);

                                        if(result.toLowerCase().equals(titleBox.getText().toString().toLowerCase())){
                                            contentBox.setFocusable(true);
                                            contentBox.requestFocus();
                                            contentBox.setSelection(contentBox.getText().length());
                                            break;
                                        }else {
                                            if(i == views.size()){
                                                for (View view1 : views){
                                                    EditText contentBox1 = (EditText)view1.findViewById(R.id.contentBox);
                                                    if(contentBox1.isFocused()){
                                                        contentBox1.setText(contentBox1.getText().toString() + (contentBox1.getText().toString().length() == 0? "":" ") + (contentBox1.getText().toString().endsWith(".")? capitalize(result):result));
                                                        contentBox1.setSelection(contentBox1.getText().length());
                                                    }
                                                }
                                            }
                                        }
                                    }

                                } else {
                                    mText.setText(text);
                                }
                            }
                        });
                    }
                }
            };

    private String capitalize(String string){

        String[] strArray = string.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String s : strArray) {
            String cap = s.substring(0, 1).toUpperCase() + s.substring(1);
            builder.append(cap);
        }
        return builder.toString();
    }

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

    String tempStr = "";

    public String createTemplateJsonString()throws JSONException {

        tempStr = "";
        Commons.fields.clear();
        JSONObject jsonObj = null;
        JSONArray jsonArr = new JSONArray();
        if (fields.size()>0){
            for(int i=0; i<fields.size(); i++){

                Field field = fields.get(i);

                String title = ((TextView)field.getView().findViewById(R.id.titleBox)).getText().toString().trim();
                String content = ((EditText)field.getView().findViewById(R.id.contentBox)).getText().toString().trim();

                if(title.length() == 0 || content.length() == 0){
                    showToast(getString(R.string.fillinemptyfields));
                    return "";
                }

                field.setTitle(title);
                field.setContent(content);

                jsonObj=new JSONObject();

                try {
                    jsonObj.put("title",field.getTitle());
                    jsonObj.put("content",field.getContent());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                jsonArr.put(jsonObj);

                Commons.fields.add(field);

            }
            JSONObject obj = new JSONObject();
            obj.put("fields", jsonArr);
            tempStr = obj.toString();
        }

        return tempStr;

    }

    private void getKeywords(){

        progressBar.setVisibility(View.VISIBLE);
        AndroidNetworking.post(ReqConst.SERVER_URL + "getKeywords")
                .addBodyParameter("temp_id", String.valueOf(Commons.template.getId()))
                .setPriority(Priority.HIGH)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // do anything with response
                        Log.d("RESPONSE!!!", response.toString());
                        progressBar.setVisibility(View.GONE);
                        try {
                            String result = response.getString("result_code");
                            if(result.equals("0")){
                                fields.clear();
                                views.clear();
                                container.removeAllViews();
                                JSONArray dataArr = response.getJSONArray("data");
                                for(int i=0; i<dataArr.length(); i++) {
                                    JSONObject object = (JSONObject) dataArr.get(i);
                                    Field field = new Field();
                                    field.setId(fields.size());
                                    field.setTitle(object.getString("keyword"));
                                    field.setContent("");

                                    View view = getLayoutInflater().inflate(R.layout.layout_temp_field, container, false);
                                    container.addView(view);
                                    field.setView(view);

                                    fields.add(field);

                                    views.add(view);
                                    final ImageView cancelButton = (ImageView)view.findViewById(R.id.btn_cancel);
                                    final TextView titleBox =( TextView) view.findViewById(R.id.titleBox);
                                    final EditText contentBox = (EditText) view.findViewById(R.id.contentBox);
                                    if(i == 0) {
                                        contentBox.setFocusable(true);
                                        contentBox.requestFocus();
                                    }
                                    titleBox.setText(field.getTitle());
                                    titleBox.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            openKeywordMenu(keywordList, titleBox, AddNewReportActivity.this);
                                        }
                                    });
                                    cancelButton.setVisibility(View.GONE);
                                }

                                if(fields.size() == 0){
                                    showToast(getString(R.string.setupkeywords));
                                }

                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(ANError error) {
                        // handle error
                        Log.d("ERROR!!!", error.getErrorBody());
                        progressBar.setVisibility(View.GONE);
                    }
                });

    }

    ArrayList<String> keywordList = new ArrayList<>();

    private void getAllKeywords(){
        AndroidNetworking.post(ReqConst.SERVER_URL + "getAllKeywords")
                .addBodyParameter("member_id", String.valueOf(Commons.thisUser.get_idx()))
                .setPriority(Priority.HIGH)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // do anything with response
                        Log.d("RESPONSE!!!", response.toString());
                        try {
                            String result = response.getString("result_code");
                            if(result.equals("0")){
                                keywordList.clear();
                                JSONArray dataArr = response.getJSONArray("data");
                                for(int i=0; i<dataArr.length(); i++) {
                                    JSONObject object = (JSONObject) dataArr.get(i);
                                    keywordList.add(object.getString("keyword"));
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(ANError error) {
                        // handle error
                        Log.d("ERROR!!!", error.getErrorBody());
                    }
                });

    }

}




















































