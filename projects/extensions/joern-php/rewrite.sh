#!/bin/bash
if [[ -z $1 ]]; then
	echo "which app to run on?"
	exit 1
fi

PHPJOERN=src/phpjoern
PHPINPUT=/home/brandon/php_apps/originals/$1

$PHPJOERN/php2ast -f neo4j -n nodes.csv -r edges.csv $PHPINPUT 2> /dev/null

service neo4j stop
rm -rf /var/lib/neo4j/data/databases/cur.db/
neo4j-admin import --multiline-fields=true --database cur.db --nodes nodes.csv --relationships edges.csv
chown -R neo4j:adm /var/lib/neo4j/data/databases/cur.db
service neo4j start
sleep 3

cd src/scripts/interproc2
./runit.sh
mkdir ~/php_apps/rewritten/extra_stuff/$1
mv killable_local_vars.csv ~/php_apps/rewritten/extra_stuff/$1/
cd -

cd src/AST_dumper
./dump_files.sh
cp -r output/$1 ~/php_apps/rewritten
cd -
