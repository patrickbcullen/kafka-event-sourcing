package com.github.patrickbcullen.profile;

/**
 * Created by cullenp on 5/3/17.
 */
public class ProfileEvent {
    public String uid;
    public String eventType;
    public String email;
    public String username;
    public ProfileEvent() {}

    public ProfileEvent(String eventType, ProfileBean profile) {
        this.uid = profile.uid;
        this.username = profile.username;
        this.email = profile.email;
        this.eventType = eventType;
    }

    public ProfileEvent(String eventType, String uid) {
        this.uid = uid;
        this.eventType = eventType;
    }
}
