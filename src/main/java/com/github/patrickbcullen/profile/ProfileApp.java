package com.github.patrickbcullen.profile;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KGroupedStream;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KStreamBuilder;
import org.apache.kafka.streams.kstream.TimeWindows;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


public class ProfileApp {

    static final String PROFILE_EVENTS_TOPIC = "profile-events";

    public static void main(String[] args) throws Exception {
        int port = 8080;
        if (args.length == 1) {
            port = Integer.valueOf(args[0]);
        }
        final String bootstrapServers = args.length > 1 ? args[1] : "localhost:9092";

        Properties streamsConfiguration = new Properties();
        // Give the Streams application a unique name.  The name must be unique in the Kafka cluster
        // against which the application is run.
        streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, "profiles-service");
        // Where to find Kafka broker(s).
        streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // Provide the details of our embedded http service that we'll use to connect to this streams
        // instance and discover locations of stores.
        streamsConfiguration.put(StreamsConfig.APPLICATION_SERVER_CONFIG, "localhost:" + port);
        final File example = Files.createTempDirectory(new File("/tmp").toPath(), "kafka-state").toFile();
        streamsConfiguration.put(StreamsConfig.STATE_DIR_CONFIG, example.getPath());

        final KafkaStreams streams = createStreams(streamsConfiguration);
        // Always (and unconditionally) clean local state prior to starting the processing topology.
        // We opt for this unconditional call here because this will make it easier for you to play around with the example
        // when resetting the application for doing a re-run (via the Application Reset Tool,
        // http://docs.confluent.io/current/streams/developer-guide.html#application-reset-tool).
        //
        // The drawback of cleaning up local state prior is that your app must rebuilt its local state from scratch, which
        // will take time and will require reading all the state-relevant data from the Kafka cluster over the network.
        // Thus in a production scenario you typically do not want to clean up always as we do here but rather only when it
        // is truly needed, i.e., only under certain conditions (e.g., the presence of a command line flag for your app).
        // See `ApplicationResetExample.java` for a production-like example.
        streams.cleanUp();
        // Now that we have finished the definition of the processing topology we can actually run
        // it via `start()`.  The Streams application as a whole can be launched just like any
        // normal Java application that has a `main()` method.
        streams.start();

        // Start the Restful proxy for servicing remote access to state stores
        final ProfileService restService = startRestProxy(streams, port);

        // Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                streams.close();
                restService.stop();
            } catch (Exception e) {
                // ignored
            }
        }));
    }


    static ProfileService startRestProxy(final KafkaStreams streams, final int port)
            throws Exception {
        final ProfileService
                profileService = new ProfileService(streams);
        profileService.start(port);
        return profileService;
    }

    static KafkaStreams createStreams(final Properties streamsConfiguration) {
        final Serde<String> stringSerde = Serdes.String();
        Map<String, Object> serdeProps = new HashMap<>();

        final Serializer<ProfileBean> profileBeanSerializer = new JsonPOJOSerializer<>();
        serdeProps.put("JsonPOJOClass", ProfileBean.class);
        profileBeanSerializer.configure(serdeProps, false);

        final Deserializer<ProfileBean> profileBeanDeserializer = new JsonPOJODeserializer<>();
        serdeProps.put("JsonPOJOClass", ProfileBean.class);
        profileBeanDeserializer.configure(serdeProps, false);

        final Serde<ProfileBean> profileBeanSerde = Serdes.serdeFrom(profileBeanSerializer, profileBeanDeserializer);
        KStreamBuilder builder = new KStreamBuilder();
        //TODO I need to create a stream for the POST events API and then use a store to get the data
        KStream<String, ProfileBean> textLines = builder.stream(stringSerde, profileBeanSerde, PROFILE_EVENTS_TOPIC);

        /* TODO this won't work because I need to just process the events and apply them to the event store
        final KGroupedStream<String, String> groupedByWord = textLines
                .flatMapValues(value -> Arrays.asList(value.toLowerCase().split("\\W+")))
                .groupBy((key, word) -> word, stringSerde, stringSerde);

         */

        return new KafkaStreams(builder, streamsConfiguration);
    }
}

