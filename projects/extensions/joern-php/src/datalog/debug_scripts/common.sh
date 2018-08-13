#!/bin/bash

# $1: the translated id 
# echos the original id
map_node_for() {
	grep "^$1," $DIR/../tmp/id_map.csv | grep -o ",[0-9]*\$"
	if [[ "$?" == "1" ]]; then
		echo "Not found"
		return 1
	fi
}
# $1: the original id 
# echos the translated id(s)
map_node_rev() {
	grep ",$1\$" $DIR/../tmp/id_map.csv | grep -o "^[0-9]*"
	if [[ "$?" == "1" ]]; then
		echo "Not found"
		return 1
	fi
}

# $1: the var's name
# echos the var's id
map_var_name() {
	grep ",$1\$" $DIR/../tmp/var_map.csv | grep -o "^[0-9]*"
	if [[ "$?" == "1" ]]; then
		echo "Not found"
		return 1
	fi
	return 0
}
