package com.app.voxmed.main;

import android.os.Bundle;
import android.view.View;

import com.app.voxmed.R;
import com.app.voxmed.base.BaseActivity;

public class GeneralKeywordsActivity extends BaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_general_keywords);

    }

    public void back(View view){
        onBackPressed();
    }

}
