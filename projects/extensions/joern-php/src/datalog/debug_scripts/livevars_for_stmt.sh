#!/bin/bash

if [[ -z $1 ]]; then
	echo "usage: ./$0 <stmt-id>"
	exit 1
fi

# First map the actual id to the new ids
if [[ -z $2 ]]; then
	ids=$( grep ",$1\$" tmp/id_map.csv | grep -o "^[0-9]*" )
else
	ids=$1
fi

# Second, get the var ids
for id in $ids; do
	var_ids=$( grep "$id\$" livedef.csv | grep -o -P "\t[0-9]*\t" | grep -oE "[0-9]*" | sort | uniq )
done

declare -A var_map
for line in $( cat tmp/var_map.csv ); do 
	id=${line/,*/}
	var=${line/*,/}
	var_map[$id]=$var
done

for id in $var_ids; do
	echo ${var_map[$id]}
done
