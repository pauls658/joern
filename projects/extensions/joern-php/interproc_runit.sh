#!/bin/bash

if [[ -z $1 ]]; then
	echo "Which app to run on?"
	exit 1;
fi

app=$1

GSHELL=/home/brandon/tinkerpop/gshell.sh

./HOWTO_PHP 1 $app
./HOWTO_PHP 3 $app
./HOWTO_PHP 4 $app
./HOWTO_PHP 5 $app

service neo4j stop

src/scripts/run_label_handleables.sh

service neo4j start
exit 0
