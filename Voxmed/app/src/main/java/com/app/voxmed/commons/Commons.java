package com.app.voxmed.commons;

import android.widget.EditText;

import com.app.voxmed.main.AddReport2Activity;
import com.app.voxmed.main.PatientListActivity;
import com.app.voxmed.main.PatientsActivity;
import com.app.voxmed.main.TemplateListActivity;
import com.app.voxmed.main.TemplateSettingActivity;
import com.app.voxmed.models.Field;
import com.app.voxmed.models.Report;
import com.app.voxmed.models.Template;
import com.app.voxmed.models.User;

import java.io.File;
import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class Commons {

    public static String lang = "en";
    public static User thisUser = null;

    public static File file = null;
    public static Report report = null;

    public static PatientListActivity patientListActivity = null;
    public static EditText editText = null;

    public static boolean doctorReportEditF = false;

    public static TemplateSettingActivity templateSettingActivity = null;

    public static Template template = null;
    public static TemplateListActivity templateListActivity = null;

    public static ArrayList<Field> fields = new ArrayList<>();

    public static User patient = null;

}
