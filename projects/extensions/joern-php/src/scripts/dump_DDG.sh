#!/bin/bash

echo "match ()-[r]-() set r.id = ID(r);match (a) set a.id = ID(a);" | cypher-shell

wget \
	--post-data='{"statements":[{"statement": "match (a)-[:REACHES|INTERPROC]-() return distinct a"}]}' \
	--header="Accept: application/json; charset=UTF-8" \
	--header="Content-Type: application/json" \
	-O nodes.json -q \
	http://localhost:7474/db/data/transaction/commit


wget \
	--post-data='{"statements":[{"statement": "match (a)-[r:REACHES|INTERPROC]->(b) return ID(a) as a, r, ID(b) as b"}]}' \
	--header="Accept: application/json; charset=UTF-8" \
	--header="Content-Type: application/json" \
	-O rels.json -q \
	http://localhost:7474/db/data/transaction/commit
