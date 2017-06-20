package com.github.patrickbcullen.profile;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;

/**
 * Created by cullenp on 5/3/17.
 */
public class ProfileEventProcessor implements Processor<String, ProfileEvent> {
    private ProcessorContext context;
    private KeyValueStore<String, ProfileBean> profileStore;
    private KeyValueStore<String, ProfileBean> searchStore;

    @Override
    public void init(ProcessorContext context) {
        this.context = context;
        this.context.schedule(10);
        profileStore = (KeyValueStore) context.getStateStore(ProfileApp.PROFILE_STORE_NAME);
        searchStore = (KeyValueStore) context.getStateStore(ProfileApp.SEARCH_STORE_NAME);
    }

    @Override
    public void process(String uid, ProfileEvent profileEvent) {
        if (profileEvent.eventType == null) {
            System.out.println(profileEvent.uid);
            return;
        }

        ProfileBean profileBean = profileStore.get(uid);
        String email = null;

        switch (profileEvent.eventType) {
            case "delete":
                if (profileBean != null && profileBean.email != null) {
                    email = profileBean.email;
                }
                profileBean = null;
                break;
            case "create":
                profileBean = new ProfileBean(profileEvent.uid, profileEvent.username, profileEvent.email);
                email = profileBean.email;
                break;
            case "update":
                if (profileEvent.email != null) {
                    //remove the old email by tombstoning the previous record
                    searchStore.put(profileBean.email, null);
                    profileBean.email = profileEvent.email;
                }
                if (profileEvent.username != null) {
                    profileBean.username = profileEvent.username;
                }
                if (profileBean != null && profileBean.email != null) {
                    email = profileBean.email;
                }
                break;
        }
        profileStore.put(uid, profileBean);
        if (email != null) {
            searchStore.put(email, profileBean);
        }
    }

    @Override
    public void punctuate(long timestamp) {
        context.commit();
    }

    @Override
    public void close() {
        profileStore.close();
        searchStore.close();
    }
}
