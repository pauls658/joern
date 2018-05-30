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

# read/write
wget \
	--post-data='{"statements":[{"statement": "match (a) where exists(a.defs) or exists(a.uses) return ID(a) as id, a.defs as defs, a.uses as uses"}]}' \
	--header="Accept: application/json; charset=UTF-8" \
	--header="Content-Type: application/json" \
	-O store_load.json -q \
	http://localhost:7474/db/data/transaction/commit

# echos
wget \
	--post-data='{"statements":[{"statement": "match (a{type:\"AST_ECHO\"}) return collect(ID(a))"}]}' \
	--header="Accept: application/json; charset=UTF-8" \
	--header="Content-Type: application/json" \
	-O echos.json -q \
	http://localhost:7474/db/data/transaction/commit

wget \
	--post-data='{"statements":[{"statement": "match (:FUNCCALL{name:\"sensitive_data\"})<-[:ASSOC]-(:ART_AST{type:\"return\"})-[:RET_DEF]->(a) return collect(ID(a))"}]}' \
	--header="Accept: application/json; charset=UTF-8" \
	--header="Content-Type: application/json" \
	-O tainted.json -q \
	http://localhost:7474/db/data/transaction/commit
