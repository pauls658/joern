#!/bin/bash

if [[ -z $1 ]]; then
	echo "Need orig node id"
	exit 1
fi

echo "match (a) where ID(a) = $1 return a;" | cypher-shell
