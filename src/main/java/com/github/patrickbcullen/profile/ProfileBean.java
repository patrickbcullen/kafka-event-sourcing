package com.github.patrickbcullen.profile;

/**
 * Created by cullenp on 4/27/17.
 */
public class ProfileBean {
    public String uid;
    public String username;
    public String email;

    public ProfileBean() {}

    public ProfileBean(String uid, String username, String email) {
        this.uid = uid;
        this.username = username;
        this.email = email;
    }
}
