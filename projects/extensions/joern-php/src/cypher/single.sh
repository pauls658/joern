#!/bin/bash

if [[ -z $1 ]]; then
	echo "which query to run?"
	exit 1
fi

cat $1 | cypher-shell -u neo4j -p " " --format plain --non-interactive
