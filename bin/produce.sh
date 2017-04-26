#!/bin/sh

docker run \
  --network "container:kafka" \
  -it --rm confluentinc/cp-kafka:3.2.0 \
  kafka-console-producer --topic UserRegions --broker-list 127.0.0.1:9092
