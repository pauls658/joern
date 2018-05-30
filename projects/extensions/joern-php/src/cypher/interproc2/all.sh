#!/bin/bash

if [[ -z $1 ]]; then
	end=100
else
	end=$1
fi

for cmd_file in $( ls *.cypher ); do
	num=$( echo "$cmd_file" | grep -o -E "[1-9][0-9]*" )
	if [[ $num -gt $end ]]; then
		echo "Stopping"
		exit 0;
	fi
	echo "Running $cmd_file..."
	res=$( cat $cmd_file | cypher-shell -u neo4j -p " " )

	echo "$res" | head -1  > `dirname $0`/prev.csv
	echo "$res" | grep -E "[0-9]"  >> `dirname $0`/prev.csv

done
