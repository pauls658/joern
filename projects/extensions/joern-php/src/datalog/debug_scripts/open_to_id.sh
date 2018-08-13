#!/bin/bash
if [[ -z $1 ]]; then
	echo "need ID"
	exit 1
fi
fp=$(echo "match (a:AST{type:'AST_TOPLEVEL'})-[:PARENT_OF*0..]->(b),(c) where ID(c) = $1 and ID(b) = c.funcid and 'TOPLEVEL_FILE' in a.flags return a.name + ' +' + c.lineno;" | cypher-shell | tail -1 | tr -d \")
vi $fp
