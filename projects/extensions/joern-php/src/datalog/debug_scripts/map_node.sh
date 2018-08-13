#!/bin/bash

if [[ -z $1 || -z $2 ]]; then
	echo "usage: ./$0 <stmt-id> <for|rev>"
	exit 1
fi

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

if [[ "$2" == "rev" ]]; then
	grep ",$1\$" $DIR/../tmp/id_map.csv | grep -o "^[0-9]*"
	if [[ "$?" == "1" ]]; then
		echo "Not found"
	fi
elif [[ "$2" == "for" ]]; then
	grep "^$1," $DIR/../tmp/id_map.csv | grep -o ",[0-9]*\$"
	if [[ "$?" == "1" ]]; then
		echo "Not found"
	fi
else
	echo "Bad input: $2"
	exit 1
fi
