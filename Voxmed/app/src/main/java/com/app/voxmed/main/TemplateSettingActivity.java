package com.app.voxmed.main;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.androidnetworking.interfaces.UploadProgressListener;
import com.app.voxmed.R;
import com.app.voxmed.adapters.ItemAdapter;
import com.app.voxmed.base.BaseActivity;
import com.app.voxmed.classes.MySwipeRefreshLayout;
import com.app.voxmed.commons.Commons;
import com.app.voxmed.commons.ReqConst;
import com.app.voxmed.models.Field;
import com.app.voxmed.models.Report;
import com.wang.avi.AVLoadingIndicatorView;
import com.woxthebox.draglistview.DragItem;
import com.woxthebox.draglistview.DragListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class TemplateSettingActivity extends BaseActivity {

    DragListView mDragListView;
    private MySwipeRefreshLayout mRefreshLayout;
    ArrayList<Pair<Long, String>> mItemArray = new ArrayList<>();;
    TextView addButton, saveButton;
    AVLoadingIndicatorView progressBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_template_setting);

        Commons.templateSettingActivity = this;

        progressBar = (AVLoadingIndicatorView)findViewById(R.id.loading_bar);

        mRefreshLayout = (MySwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);

        mDragListView = (DragListView) findViewById(R.id.drag_list_view);
        mDragListView.getRecyclerView().setVerticalScrollBarEnabled(true);
        mDragListView.setDragListListener(new DragListView.DragListListenerAdapter() {
            @Override
            public void onItemDragStarted(int position) {
                mRefreshLayout.setEnabled(false);
            }

            @Override
            public void onItemDragEnded(int fromPosition, int toPosition) {
                mRefreshLayout.setEnabled(true);
                if (fromPosition != toPosition) {
                    Log.d("ToPosition+++", String.valueOf(toPosition));
                    for(int i=0; i<mItemArray.size(); i++){
                        Log.d("Keyword" + i, String.valueOf(mItemArray.get(i).second));
                    }
                }
            }
        });

        mRefreshLayout.setScrollingView(mDragListView.getRecyclerView());
        mRefreshLayout.setColorSchemeColors(ContextCompat.getColor(getApplicationContext(), R.color.app_color));
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mRefreshLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mRefreshLayout.setRefreshing(false);
                    }
                }, 2000);
            }
        });

        addButton = (TextView)findViewById(R.id.btn_add_keyword);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInputAlertDialog();
            }
        });
        saveButton = (TextView)findViewById(R.id.btn_save);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Commons.template != null){
                    saveTemplate(Commons.template.getName());
                }else {
                    showInputTempNameAlertDialog();
                }

            }
        });

        if(Commons.template != null)
            getKeywords();

    }

    private void showInputTempNameAlertDialog(){
        AlertDialog.Builder alert = new AlertDialog.Builder(TemplateSettingActivity.this);
        alert.setTitle(getString(R.string.enter_temp_name));
// Set an EditText view to get user input
        final EditText input = new EditText(TemplateSettingActivity.this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setGravity(Gravity.CENTER);
        alert.setView(input);
        alert.setCancelable(false);
        alert.setPositiveButton(getString(R.string.save), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString();
                if(value.length() == 0){
                    showToast(getString(R.string.enter_temp_name));
                    return;
                }
                saveTemplate(value);
            }
        });

        alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();
    }

    private void saveTemplate(String name){

        if(mItemArray.size() > 0){
            try {
                String keywordJsonStr = createKeywordJsonString();
                progressBar.setVisibility(View.VISIBLE);
                AndroidNetworking.post(ReqConst.SERVER_URL + "saveKeywords")
                        .addBodyParameter("temp_id", Commons.template != null?String.valueOf(Commons.template.getId()):"0")
                        .addBodyParameter("member_id", String.valueOf(Commons.thisUser.get_idx()))
                        .addBodyParameter("name", name)
                        .addBodyParameter("keyword_json_str", keywordJsonStr)
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
                                        showToast(getString(R.string.saved));
                                        finish();
                                    }else {
                                        showToast(getString(R.string.server_issue));
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
                                showToast(error.getMessage());
                            }
                        });

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }

    public void back(View view){
        onBackPressed();
    }

    public void setupListRecyclerView() {
        mDragListView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        ItemAdapter listAdapter = new ItemAdapter(mItemArray, R.layout.item_keywordlist, R.id.text, false);
        mDragListView.setAdapter(listAdapter, true);
        mDragListView.setCanDragHorizontally(false);
        mDragListView.setCustomDragItem(new MyDragItem(getApplicationContext(), R.layout.item_keywordlist));
    }

    private static class MyDragItem extends DragItem {

        MyDragItem(Context context, int layoutId) {
            super(context, layoutId);
        }

        @Override
        public void onBindDragView(View clickedView, View dragView) {
            CharSequence text = ((TextView) clickedView.findViewById(R.id.text)).getText();
            ((TextView) dragView.findViewById(R.id.text)).setText(text);
            dragView.findViewById(R.id.item_layout).setBackgroundColor(dragView.getResources().getColor(R.color.list_item_background));
        }
    }

    private void showInputAlertDialog(){

        AlertDialog.Builder alert = new AlertDialog.Builder(TemplateSettingActivity.this);
        alert.setTitle(getString(R.string.new_keyword));
// Set an EditText view to get user input
        final EditText input = new EditText(TemplateSettingActivity.this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setGravity(Gravity.CENTER);
        alert.setView(input);
        alert.setCancelable(false);
        alert.setPositiveButton(getString(R.string.add), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString();
                // Do something with value!
                mItemArray.add(new Pair<>((long) mItemArray.size(), value));
                setupListRecyclerView();
            }
        });

        alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();

    }

    public String createKeywordJsonString()throws JSONException {

        String tempStr = "";
        JSONObject jsonObj = null;
        JSONArray jsonArr = new JSONArray();
        if (mItemArray.size()>0){
            for(int i=0; i<mItemArray.size(); i++){
                String keyword = mItemArray.get(i).second;
                jsonObj = new JSONObject();
                try {
                    jsonObj.put("keyword", keyword);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                jsonArr.put(jsonObj);
            }
            JSONObject obj = new JSONObject();
            obj.put("keywords", jsonArr);
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
                                mItemArray.clear();
                                JSONArray dataArr = response.getJSONArray("data");
                                for(int i=0; i<dataArr.length(); i++) {
                                    JSONObject object = (JSONObject) dataArr.get(i);
                                    mItemArray.add(new Pair<>((long) mItemArray.size(), object.getString("keyword")));
                                }
                                setupListRecyclerView();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Commons.templateSettingActivity = null;
    }
}

























