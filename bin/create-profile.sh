#!/bin/sh
curl -XPOST -H "Content-Type: application/json" -d '{"username": "foobar", "email": "foobar@gmail.com"}' http://localhost:8080/api/profile
