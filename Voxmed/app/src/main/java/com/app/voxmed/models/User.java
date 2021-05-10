package com.app.voxmed.models;

public class User {
    int _idx = 0;
    String _name = "";
    String _email = "";
    String _password = "";
    String _picture_url = "";
    String _age = "";
    String _registered_time = "";
    String _role = "";
    String _patientID = "";
    String _status = "";
    String _fcm_token = "";

    public User(){

    }

    public void set_age(String _age) {
        this._age = _age;
    }

    public String get_age() {
        return _age;
    }

    public void set_patientID(String _patientID) {
        this._patientID = _patientID;
    }

    public String get_patientID() {
        return _patientID;
    }

    public void set_idx(int _idx) {
        this._idx = _idx;
    }

    public void set_name(String _name) {
        this._name = _name;
    }

    public void set_email(String _email) {
        this._email = _email;
    }

    public void set_password(String _password) {
        this._password = _password;
    }

    public void set_picture_url(String _picture_url) {
        this._picture_url = _picture_url;
    }

    public void set_registered_time(String _registered_time) {
        this._registered_time = _registered_time;
    }

    public void set_role(String _role) {
        this._role = _role;
    }

    public void set_status(String _status) {
        this._status = _status;
    }

    public void set_fcm_token(String _fcm_token) {
        this._fcm_token = _fcm_token;
    }

    public int get_idx() {
        return _idx;
    }

    public String get_name() {
        return _name;
    }

    public String get_email() {
        return _email;
    }

    public String get_password() {
        return _password;
    }

    public String get_picture_url() {
        return _picture_url;
    }

    public String get_registered_time() {
        return _registered_time;
    }

    public String get_role() {
        return _role;
    }

    public String get_status() {
        return _status;
    }

    public String get_fcm_token() {
        return _fcm_token;
    }
}
