#!/bin/bash

echo "match ()-[r]-() set r.id = ID(r);match (a) set a.id = ID(a);" | cypher-shell

wget \
	--post-data='{"statements":[{"statement": "match (a)-[:FLOWS_TO]-() return distinct a"}]}' \
	--header="Accept: application/json; charset=UTF-8" \
	--header="Content-Type: application/json" \
	-O nodes.json -q \
	http://localhost:7474/db/data/transaction/commit


wget \
	--post-data='{"statements":[{"statement": "match (a)-[r:FLOWS_TO]->(b) return ID(a) as a, ID(b) as b"}]}' \
	--header="Accept: application/json; charset=UTF-8" \
	--header="Content-Type: application/json" \
	-O flows_to.json -q \
	http://localhost:7474/db/data/transaction/commit

wget \
	--post-data='{"statements":[{"statement": "match (a)-[r:INTERPROC]->(b) return ID(a) as a, ID(b) as b, r"}]}' \
	--header="Accept: application/json; charset=UTF-8" \
	--header="Content-Type: application/json" \
	-O interproc.json -q \
	http://localhost:7474/db/data/transaction/commit
