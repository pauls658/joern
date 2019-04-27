#!/bin/bash

IFS=$'\n'
for l in $( cat tmp/input_params.csv ); do 
	IFS=$' '
	a=($l)
	echo ${a[1]} > tmp/info
	open_to_id.sh ${a[0]}
done
