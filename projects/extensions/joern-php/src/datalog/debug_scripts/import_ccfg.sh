#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

python $DIR/../debug_tools.py copiedcfg

echo 'using periodic commit
load csv with headers from "file:///home/brandon/joern/projects/extensions/joern-php/src/datalog/nodes.csv" as line
create
(new:CCFG{id:toInteger(line.id),defs:coalesce(line.def, ""),uses:coalesce(line.use, ""),kill:coalesce(line.kill, ""),orig_id:toInteger(line.orig_id),ctrltainted:toBoolean(line.ctrltainted)})
return toInteger(line.id),ID(new);' | cypher-shell > $DIR/../cypher_id_map

declare -A cypher_id_map

# create a map from the CCFG id to the id given by neo4j
for l in $( tail -n+2 $DIR/../cypher_id_map | tr -d ' ' ); do
	new_id=${l/,*/}
	cypher_id=${l/*,/}
	cypher_id_map[$new_id]=$cypher_id
done

head -1 $DIR/../datadeps.csv > $DIR/../tmp_datadeps.csv
for l in $( tail -n+2 $DIR/../datadeps.csv ); do
	s=${l/,*/}
	e=${l#*,}
	e=${e%,*}
	v=${l##*,}
	trans_s=${cypher_id_map[$s]}
	trans_e=${cypher_id_map[$e]}
	echo "$trans_s,$trans_e,$v" >> $DIR/../tmp_datadeps.csv
done

echo 'using periodic commit
load csv with headers from "file:///home/brandon/joern/projects/extensions/joern-php/src/datalog/tmp_datadeps.csv" as line
match
(s:CCFG),(e:CCFG)
where
ID(s) = toInteger(line.start) and 
ID(e) = toInteger(line.end)
create
(s)-[:REACHES{var:line.var}]->(e);' | cypher-shell

rm $DIR/../tmp_datadeps.csv

echo "n,d" > $DIR/../tmp_ctrldeps.csv
IFS=$'\n'
for l in $( cat tmp/ctrldep.csv ); do
    IFS=$'\t'
    arr=($l)
    n=${cypher_id_map[${arr[0]}]}
    d=${cypher_id_map[${arr[1]}]}
    echo "$n,$d" >> $DIR/../tmp_ctrldeps.csv
done
IFS=$'\n'

echo 'using periodic commit
load csv with headers from "file:///home/brandon/joern/projects/extensions/joern-php/src/datalog/tmp_ctrldeps.csv" as line
match
(s:CCFG),(e:CCFG)
where
ID(s) = toInteger(line.n) and 
ID(e) = toInteger(line.d)
create
(s)-[:CTRLDEP]->(e);' | cypher-shell
rm $DIR/../cypher_id_map
