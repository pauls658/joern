#!/bin/bash

IFS=$'\n'
for l in $( grep -P "\t[0-9][0-9]*:" tmp_Res ); do
	echo "$l" > info
	id=$( echo "$l" | grep -o -E "[0-9][0-9]*:" | tr -d ':' )
	open_to_id.sh $id
done
