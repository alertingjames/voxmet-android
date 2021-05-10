package com.app.voxmed.models;

import java.io.File;
import java.util.ArrayList;

public class Report {

    int _idx = 0;
    int _member_id = 0;
    String _subject = "";
    String _patientID = "";
    String _body = "";
    String _picture_url = "";
    String _audio_url = "";
    File _audio_file = null;
    String _date_time = "";
    String _status = "";
    boolean _corrected = false;
    ArrayList<Field> fields = new ArrayList<>();

    public Report(){

    }

    public void setFields(ArrayList<Field> fields) {
        this.fields.clear();
        this.fields.addAll(fields);
    }

    public ArrayList<Field> getFields() {
        return fields;
    }

    public void set_corrected(boolean _corrected) {
        this._corrected = _corrected;
    }

    public boolean is_corrected() {
        return _corrected;
    }

    public void set_audio_url(String _audio_url) {
        this._audio_url = _audio_url;
    }

    public void set_audio_file(File _audio_file) {
        this._audio_file = _audio_file;
    }

    public String get_audio_url() {
        return _audio_url;
    }

    public File get_audio_file() {
        return _audio_file;
    }

    public void set_idx(int _idx) {
        this._idx = _idx;
    }

    public void set_member_id(int _member_id) {
        this._member_id = _member_id;
    }

    public void set_subject(String _subject) {
        this._subject = _subject;
    }

    public void set_patientID(String _patientID) {
        this._patientID = _patientID;
    }

    public void set_body(String _body) {
        this._body = _body;
    }

    public void set_picture_url(String _picture_url) {
        this._picture_url = _picture_url;
    }

    public void set_date_time(String _date_time) {
        this._date_time = _date_time;
    }

    public void set_status(String _status) {
        this._status = _status;
    }

    public int get_idx() {
        return _idx;
    }

    public int get_member_id() {
        return _member_id;
    }

    public String get_subject() {
        return _subject;
    }

    public String get_patientID() {
        return _patientID;
    }

    public String get_body() {
        return _body;
    }

    public String get_picture_url() {
        return _picture_url;
    }

    public String get_date_time() {
        return _date_time;
    }

    public String get_status() {
        return _status;
    }
}
