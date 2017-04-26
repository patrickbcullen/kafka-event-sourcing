#!/bin/sh

function create_topic () {
  local topic="$1"
  docker run \
    --network "container:zookeeper" \
    --rm confluentinc/cp-kafka:3.2.0 \
    kafka-topics --create --topic $topic --partitions 1 --replication-factor 1 --if-not-exists --zookeeper 127.0.0.1:2181
  docker run \
    --network "container:zookeeper" \
    --rm confluentinc/cp-kafka:3.2.0 \
    kafka-topics --describe --topic $topic --zookeeper zookeeper:2181
}

create_topic UserRegions
create_topic LargeRegions
