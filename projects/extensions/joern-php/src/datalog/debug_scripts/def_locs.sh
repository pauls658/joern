#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

. $DIR/common.sh

if [[ -z $1 || -z $2 ]]; then
	echo "usage: $0 <stmt-id> <var-name>"
	exit 1
fi

var_id=$( map_var_name $2 ) 
if [[ "$?" == "1" ]]; then
	exit "var: $2 not found"
	exit 1
fi

stmts=$( map_node_rev $1 )
if [[ "$?" == "1" ]]; then
	exit "stmt: $1 not found"
	exit 1
fi

echo "$stmts $var_id"

for stmt in $stmts; do
	for def in $( grep -P "\t$var_id\t$stmt\$" $DIR/../livedef.csv | grep -o -E "^[0-9]*" ); do
		map_node_for $def
	done
done
