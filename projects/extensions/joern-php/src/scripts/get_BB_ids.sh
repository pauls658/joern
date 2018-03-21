#!/bin/bash

service neo4j start
echo "Waiting for neo4j to come up..."
sleep 3
echo "Running query"
echo "match (a:BB) return distinct ID(a);" | cypher-shell --format plain | tail -n +2 > ids
echo "Stopping neo4j"
service neo4j stop
