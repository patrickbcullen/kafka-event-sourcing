#!/bin/sh
docker run \
  --network "container:zookeeper" \
  --rm confluentinc/cp-kafka:3.2.0 \
  kafka-topics --create --topic foo --partitions 1 --replication-factor 1 --if-not-exists --zookeeper 127.0.0.1:2181
docker run \
  --network "container:zookeeper" \
  --rm confluentinc/cp-kafka:3.2.0 \
  kafka-topics --describe --topic foo --zookeeper zookeeper:2181
