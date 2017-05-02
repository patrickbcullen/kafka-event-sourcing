package com.github.patrickbcullen.profile;

import org.apache.kafka.streams.KafkaStreams;
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

@Path("api")
public class ProfileService {

    private final KafkaStreams streams;
    private final String storeName;
    private Server jettyServer;

    ProfileService(final KafkaStreams streams) {
        this.streams = streams;
        this.storeName = "profile";
    }

    @GET
    @Path("/profile/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ProfileBean getProfileByID(@PathParam("id") String id) {
        System.out.println("IN GET METHOD");
        final ReadOnlyKeyValueStore<String, ProfileBean> store = streams.store(storeName, QueryableStoreTypes.<String, ProfileBean>keyValueStore());
        if (store == null) {
            throw new NotFoundException();
        }

        // Get the value from the store
        final ProfileBean value = store.get(id);
        if (value == null) {
            throw new NotFoundException();
        }
        return value;
    }

    @POST
    @Path("/profile")
    @Produces(MediaType.APPLICATION_JSON)
    public Response createProfile(ProfileBean profile) {
        System.out.println("IN POST METHOD");
        System.out.println(profile);
        return Response.status(201).entity(profile).build();
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
