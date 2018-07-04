#!/bin/bash

if [[ -z $1 || -z $2 ]]; then
	echo "usage: ./$0 <stmt-id> <pred|succ>"
	exit 1
fi

if [[ "$2"	== "pred" ]]; then
	grep -P "\t$1\$" tmp/edge.csv | grep -o -E "^[0-9]*"
	if [[ "$?" == "1" ]]; then
		echo "None found"
	fi
elif [[ "$2" == "succ" ]]; then
	grep -P "^$1\t" tmp/edge.csv | grep -o -E "[0-9]*\$"
	if [[ "$?" == "1" ]]; then
		echo "None found"
	fi
else
	echo "Bad input arg: $2"
	exit 1
fi
