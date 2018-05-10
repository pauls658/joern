#!/bin/bash

if [[ -z $1 ]]; then
	echo "which app to run on?"
	exit 1
fi

./HOWTO_PHP 1 $1
./HOWTO_PHP 3 $1
./HOWTO_PHP 4 $1

cd src/cypher/interproc2/
./all.sh
cd -
