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
import java.util.*;

@Path("api")
public class ProfileService {

    private final KafkaStreams streams;
    private final String profileStoreName;
    private final String searchStoreName;
    private Server jettyServer;
    private final KafkaProducer<String, ProfileEvent> profileProducer;
    private final String topic;

    ProfileService(final KafkaStreams streams, final String profileEventsTopic, final String profileStoreName, final String searchStoreName,
                   final String bootstrapServers) {
        this.streams = streams;
        this.profileStoreName = profileStoreName;
        this.searchStoreName = searchStoreName;
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
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    public ProfileBean searchProfile(@QueryParam("email") String email) {
        return findProfileByKey(email, streams.store(searchStoreName, QueryableStoreTypes.<String, ProfileBean>keyValueStore()));
    }

    @GET
    @Path("/profile/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ProfileBean getProfileByID(@PathParam("id") String id) {
        return findProfileByKey(id, streams.store(profileStoreName, QueryableStoreTypes.<String, ProfileBean>keyValueStore()));
    }

    private ProfileBean findProfileByKey(String key, ReadOnlyKeyValueStore<String, ProfileBean> stateStore) {
        final ProfileBean value = stateStore.get(key);
        if (value == null) {
            throw new NotFoundException();
        }
        return value;
    }

    @PUT
    @Path("/profile/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateProfile(@PathParam("id") String id, ProfileBean profile) {
        profileProducer.send(new ProducerRecord<String, ProfileEvent>(topic, id, new ProfileEvent("update", profile)));
        return Response.status(200).entity(profile).build();
    }

    @POST
    @Path("/profile")
    @Produces(MediaType.APPLICATION_JSON)
    public Response createProfile(ProfileBean profile) {
        profile.uid = newUUID();
        profileProducer.send(new ProducerRecord<String, ProfileEvent>(topic, profile.uid, new ProfileEvent("create", profile)));
        return Response.status(201).entity(profile).build();
    }

    @DELETE
    @Path("/profile/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteProfile(@PathParam("id") String id) {
        profileProducer.send(new ProducerRecord<String, ProfileEvent>(topic, id, new ProfileEvent("delete")));
        return Response.status(204).build();
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
