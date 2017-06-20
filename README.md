# kafka-event-sourcing
Kafka has traditionally been used for storing ephemeral data for replication between persistance systems. But Kafka Interactive Queries  and Kafka Streams with log compaction adds the ability to persistent state to the Kafka cluster without the need for a backend database.

This Kafka event sourcing demo app uses the Kafka Streams and Interactive Queries features to build a simple REST API for storing user profiles. There is POSt endpoint for creating new profiles and a GET endpoint for retrieving those profiles. But instead of using a traditional database, Kafka is used to store the profile state indefinitely. The profile data is never removed from the cluster because the topic uses log compaction.

So why build a system like this? There are two main benefits to this: scalability and replication. Because Kafka partitions the state and keeps it locally on the same server as the API code. In effect the data moves to the code. The data is still replicated to other nodes so it is never lost if a single server fails. If the request lands on a server without the data, it can be forwarded to the correct server to get the data. If you don't care about scalability of the data, you can reduce the latency by keeping the same state on every node. This will increase the cost of replicating changes but reduce the cost of querying the data since it will never need to query another node to get the data. The partitioning also provides a built in method for scaling the service instead of trying to build that later. 

The other benefit is the ability to replicate the state to other systems. For example, you can have a Kafka consumer listen for state changes on the topic and send them to another Kafka cluster in another region or even another system like an Elasticsearch cluster.

This projects wraps the Kafka Streams and Interactive Queries feature in a Jersey REST API. For clients calling the system it acts like any other REST API. This gives you the benefits of Kafka, without changing the architecture pattern between services.  Just a like a regular database, the data is replicated to multiple servers and kept persistently. Think of it like using Kafka to build a microservice, but remove the typical database backend for the REST API and use the data directly from Kafka.

## Setup
To run the project you will need Kafka and Zookeeper running first and then launch the Java application that hosts the stream processor and REST API.

To get Kafka and Zookeeper running do this:
```
cd kafka-event-sourcing
docker-compose up
```

Then run the Java application.
```
./gradlew run
```

To test that it is working, create a new user.
```
curl -XPOST -H "Content-Type: application/json" -d '{"username": "bob", "email": "bob1@mail.com"}' http://localhost:8080/api/profile
```

Then query for this user using the uid parameter returned from the POST command.
```
curl -XGET -H "Accept: application/json" http://localhost:8080/api/profile/74292b21-0fbb-4f1d-a18f-fa80f947fa5d
```

Try querying for the user using email.
```
curl -XGET -H "Accept: application/json" http://localhost:8080/api/search\?email\=bob1@mail.com
```

Now change the email address.
```
curl -XPUT -H "Content-Type: application/json" -d '{"username": "bob", "email": "bob2@mail.com"}' http://localhost:8080/api/profile/74292b21-0fbb-4f1d-a18f-fa80f947fa5d
curl -XGET -H "Accept: application/json" http://localhost:8080/api/search\?email\=bob2@mail.com
```

Finally delete the user.
```
curl -XDELETE http://localhost:8080/api/profile/74292b21-0fbb-4f1d-a18f-fa80f947fa5d
```

## Architecture
[describe what each Java class does]
