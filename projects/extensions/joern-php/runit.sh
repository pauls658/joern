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

exit 0

$GSHELL -e `pwd`/src/groovy/dataflows.groovy

service neo4j start

sleep 3

cat src/cypher/instrument/taint_vars.cypher | cypher-shell
