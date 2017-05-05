package com.github.patrickbcullen.profile;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.errors.InvalidStateStoreException;
import org.apache.kafka.streams.state.*;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

@Path("api")
public class ProfileService {

    private final KafkaStreams streams;
    private final String storeName;
    private Server jettyServer;
    private final KafkaProducer<String, ProfileEvent> profileProducer;
    private final String topic;

    ProfileService(final KafkaStreams streams, final String profileEventsTopic, final String profileStoreName, final String bootstrapServers) {
        this.streams = streams;
        this.storeName = profileStoreName;
        this.topic = profileEventsTopic;
        Map<String, Object> serdeProps = new HashMap<>();
        final Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        final Serializer<ProfileEvent> profileEventSerializer = new JsonPOJOSerializer<>();
        serdeProps.put("JsonPOJOClass", ProfileEvent.class);
        profileEventSerializer.configure(serdeProps, false);

        this.profileProducer = new KafkaProducer<>(producerProps, new StringSerializer(), profileEventSerializer);

    }

    @GET
    @Path("/profile/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ProfileBean getProfileByID(@PathParam("id") String id) {
        System.out.println("IN GET METHOD");
        System.out.println(id);
        final ReadOnlyKeyValueStore<String, ProfileBean> store;
        try {
            store = streams.store(storeName, QueryableStoreTypes.<String, ProfileBean>keyValueStore());
            if (store == null) {
                System.out.println("NO STORE");
                throw new NotFoundException();
            }
        } catch(InvalidStateStoreException ex) {
            System.out.println(ex);
            return null;
        }

        // Get the value from the store
        final ProfileBean value = store.get(id);
        if (value == null) {
            System.out.println("NO PROFILE");
            throw new NotFoundException();
        }
        System.out.println("RETURNING");
        return value;
    }

    @POST
    @Path("/profile")
    @Produces(MediaType.APPLICATION_JSON)
    public Response createProfile(ProfileBean profile) {
        profile.uid = newUUID();
        System.out.println("IN POST METHOD");
        System.out.println(profile.uid);
        profileProducer.send(new ProducerRecord<String, ProfileEvent>(topic, profile.uid, new ProfileEvent(profile)));
        return Response.status(201).entity(profile).build();
    }

    private String newUUID() {
        return String.valueOf(UUID.randomUUID());
    }

    void start(final int port) throws Exception {
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        jettyServer = new Server(port);
        jettyServer.setHandler(context);

        ResourceConfig rc = new ResourceConfig();
        rc.register(this);
        rc.register(JacksonFeature.class);

        ServletContainer sc = new ServletContainer(rc);
        ServletHolder holder = new ServletHolder(sc);
        context.addServlet(holder, "/*");

        jettyServer.start();
    }

    void stop() throws Exception {
        if (jettyServer != null) {
            jettyServer.stop();
        }
    }

}
