package com.app.voxmed.main;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;

import com.akexorcist.localizationactivity.ui.LocalizationActivity;
import com.app.voxmed.R;
import com.app.voxmed.base.BaseActivity;
import com.app.voxmed.commons.Commons;
import com.iamhabib.easy_preference.EasyPreference;

public class SettingsActivity extends BaseActivity {

    RadioGroup radioGroup;
    RadioButton radioButton, btn_en, btn_ro, btn_sv;
    Switch notiSwitchButton;
    TextView saveButton;
    String selectedLang = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        btn_en = (RadioButton)findViewById(R.id.en);
        btn_ro = (RadioButton)findViewById(R.id.ro);
        btn_sv = (RadioButton)findViewById(R.id.sv);

        saveButton = (TextView)findViewById(R.id.btn_save);

        radioGroup = (RadioGroup)findViewById(R.id.radioGroup);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                radioButton = (RadioButton)findViewById(group.getCheckedRadioButtonId());
                if (radioButton.getText().equals(btn_en.getText())){
                    selectedLang = "en";
                }else if (radioButton.getText().equals(btn_ro.getText())){
                    selectedLang = "ro";
                }else if (radioButton.getText().equals(btn_sv.getText())){
                    selectedLang = "sv";
                }

            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(selectedLang.equals("en")){
                    EasyPreference.with(getApplicationContext()).addString("lang", "en").save();
                    Commons.lang = "en";
                    setLanguage("en");
                }else if(selectedLang.equals("ro")){
                    EasyPreference.with(getApplicationContext()).addString("lang", "ro").save();
                    Commons.lang = "ro";
                    setLanguage("ro");
                }else if(selectedLang.equals("sv")){
                    EasyPreference.with(getApplicationContext()).addString("lang", "sv").save();
                    Commons.lang = "sv";
                    setLanguage("sv");
                }

                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
            }
        });

        String lang = EasyPreference.with(getApplicationContext()).getString("lang", "en");
        if(lang.equals("en")){
            btn_en.setChecked(true);
            Commons.lang = "en";
            selectedLang = "en";
        }else if(lang.equals("ro")){
            btn_ro.setChecked(true);
            Commons.lang = "ro";
            selectedLang = "ro";
        }else if(lang.equals("sv")){
            btn_sv.setChecked(true);
            Commons.lang = "sv";
            selectedLang = "sv";
        }
    }

    public void back(View view){
        onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
















































