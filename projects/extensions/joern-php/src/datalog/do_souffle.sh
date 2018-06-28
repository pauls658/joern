#!/bin/bash

if [[ -z $1 ]]; then
	echo "Which rules to run on?"
	for f in $( ls souffle/*.dl ); do
		echo $( basename ${f/\.dl/} );
	done
	exit 1
fi

cat souffle/$1.dl > test.dl
cat tmp/facts >> test.dl
souffle -j2 -D. test.dl
