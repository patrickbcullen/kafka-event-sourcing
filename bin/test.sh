#!/bin/bash
set -x

PROFILE_ID=$(curl -s -XPOST -H "Content-Type: application/json" -d '{"username": "bob", "email": "bob1@mail.com"}' http://localhost:8080/api/profile | perl -e 'while(<>) {$_ =~ /"uid":"([^"]+)"/; print "$1\n"}')

curl -s -XGET -H "Accept: application/json" http://localhost:8080/api/profile/$PROFILE_ID
curl -s -XGET -H "Accept: application/json" http://localhost:8080/api/search\?email\=bob1@mail.com
curl -s -XPUT -H "Content-Type: application/json" -d '{"username": "bob", "email": "bob2@mail.com"}' http://localhost:8080/api/profile/$PROFILE_ID
curl -s -XGET -H "Accept: application/json" http://localhost:8080/api/search\?email\=bob2@mail.com
curl -s -XDELETE http://localhost:8080/api/profile/$PROFILE_ID
curl -s -XGET -H "Accept: application/json" http://localhost:8080/api/search\?email\=bob2@mail.com
