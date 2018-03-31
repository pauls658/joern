#!/bin/bash

cat ../cypher/interproc/99_final_cleanup.cypher | cypher-shell

OUTDIR="output"

top_level=$( echo "match (a) where ID(a) = 0 return a.name;" | cypher-shell | tail -n +2 | tr -d "\"" ) 
rm -rf $OUTDIR/$top_level

rows=$( cat get_files.cypher | cypher-shell | tail -n +2 | tr -d  ",\"")

IFS=$'\n'
for row in $rows; do
	echo $row
	IFS=$' '
	arr=($row) #arr[0] = path, arr[1] = file id
	rm -f relations.json
	wget \
		--post-data='{"statements":[{"statement": "match (t:AST)-[:PARENT_OF*0..]->(a:AST)-[r:PARENT_OF]->(b:AST) where ID(t) = '${arr[1]}' return a, r, b"}]}' \
		--header="Accept: application/json; charset=UTF-8" \
		--header="Content-Type: application/json" \
		-O relations.json -q \
		http://localhost:7474/db/data/transaction/commit

	mkdir -p $OUTDIR/`dirname ${arr[0]}`

	php neo4j_AST_to_file.php ${arr[1]} > $OUTDIR/${arr[0]}

	if [[ "$?" != "0" ]]; then
		echo "Error"
		exit 1
	fi
done
