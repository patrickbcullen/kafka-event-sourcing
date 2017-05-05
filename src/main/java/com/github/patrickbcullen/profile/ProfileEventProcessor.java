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
    private KeyValueStore<String, ProfileBean> kvStore;

    @Override
    public void init(ProcessorContext context) {
        this.context = context;
        this.context.schedule(10);
        kvStore = (KeyValueStore) context.getStateStore(ProfileApp.PROFILE_STORE_NAME);
    }

    @Override
    public void process(String uid, ProfileEvent profileEvent) {
        if (profileEvent.eventType == null) {
            System.out.println(profileEvent.uid);
            return;
        }

        ProfileBean profileBean = kvStore.get(uid);

        switch (profileEvent.eventType) {
            case "delete":
                profileBean = null;
            case "create":
                profileBean = new ProfileBean(profileEvent.uid, profileEvent.username, profileEvent.email);
            case "update":
                if (profileEvent.email != null) {
                    profileBean.email = profileEvent.email;
                }
                if (profileEvent.username != null) {
                    profileBean.username = profileEvent.username;
                }
        }
        kvStore.put(uid, profileBean);
    }

    @Override
    public void punctuate(long timestamp) {
        context.commit();
    }

    @Override
    public void close() {
        kvStore.close();
    }
}
