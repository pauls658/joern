#!/bin/bash

echo "match ()-[r]-() set r.id = ID(r);match (a) set a.id = ID(a);" | cypher-shell

wget \
	--post-data='{"statements":[{"statement": "match (a)-[:FLOWS_TO|INTERPROC]-() return distinct a"}]}' \
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
	-O def_use.json -q \
	http://localhost:7474/db/data/transaction/commit

# echos
wget \
	--post-data='{"statements":[{"statement": "match (a{type:\"AST_ECHO\"}) return collect(ID(a))"}]}' \
	--header="Accept: application/json; charset=UTF-8" \
	--header="Content-Type: application/json" \
	-O sinks.json -q \
	http://localhost:7474/db/data/transaction/commit

	#--post-data='{"statements":[{"statement": "match (call:FUNCCALL)<-[:ASSOC]-(:ART_AST{type:\"return\"})-[:RET_DEF]->(a) where call.name in [\"mime_fetch_body\", \"sqimap_get_small_header_list\"] return collect(ID(a))"}]}' \
wget \
	--post-data='{"statements":[{"statement": "match (call:FUNCCALL)<-[:ASSOC]-(:ART_AST{type:\"return\"})-[:RET_DEF]->(a) where call.name in [\"sqimap_run_command\", \"sqimap_run_command_list\"] return collect(ID(a))"}]}' \
	--header="Accept: application/json; charset=UTF-8" \
	--header="Content-Type: application/json" \
	-O sources.json -q \
	http://localhost:7474/db/data/transaction/commit
