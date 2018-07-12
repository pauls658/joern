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

# def/use
wget \
	--post-data='{"statements":[{"statement": "match (a) where exists(a.defs) or exists(a.uses) return ID(a) as id, a.defs as defs, a.uses as uses"}]}' \
	--header="Accept: application/json; charset=UTF-8" \
	--header="Content-Type: application/json" \
	-O def_use.json -q \
	http://localhost:7474/db/data/transaction/commit

# tainted echos
wget \
	--post-data='{"statements":[{"statement": "match (a:FUNCCALL{name:\"tainted_echo\"})<-[:CALL_ID]-(arg:ART_AST{type:\"arg_entry\",childnum:0}) return collect(ID(arg))"}]}' \
	--header="Accept: application/json; charset=UTF-8" \
	--header="Content-Type: application/json" \
	-O tainted_sinks.json -q \
	http://localhost:7474/db/data/transaction/commit

wget \
	--post-data='{"statements":[{"statement": "match (a:FUNCCALL{name:\"safe_echo\"})<-[:CALL_ID]-(arg:ART_AST{type:\"arg_entry\",childnum:0}) return collect(ID(arg))"}]}' \
	--header="Accept: application/json; charset=UTF-8" \
	--header="Content-Type: application/json" \
	-O safe_sinks.json -q \
	http://localhost:7474/db/data/transaction/commit


# sources
wget \
	--post-data='{"statements":[{"statement": "match (:FUNCCALL{name:\"mime_fetch_body\"})<-[:ASSOC]-(:ART_AST{type:\"return\"})-[:RET_DEF]->(a) return collect(ID(a))"}]}' \
	--header="Accept: application/json; charset=UTF-8" \
	--header="Content-Type: application/json" \
	-O sources.json -q \
	http://localhost:7474/db/data/transaction/commit
echo "Remember to change back to sensitive_data"

# sinks - shouldn't return anything, but need it so my scripts dont complain
wget \
	--post-data='{"statements":[{"statement": "match (a{type:\"AST_ECHO\"}) return collect(ID(a))"}]}' \
	--header="Accept: application/json; charset=UTF-8" \
	--header="Content-Type: application/json" \
	-O sinks.json -q \
	http://localhost:7474/db/data/transaction/commit
