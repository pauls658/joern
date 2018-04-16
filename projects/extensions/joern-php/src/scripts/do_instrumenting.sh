#!/bin/bash

dom_csvs=$( ls ../networkx/pdoms/* )
service neo4j stop
# gremlin stuff first
echo "Doing gremlin instrumention queries..."
for dom_csv in $dom_csvs; do
	dtype=$( basename $dom_csv )
	dtype=${dtype%.csv}
	instr_file="instrument/gremlin_instr/$dtype.sh"
	if [[ -f  $instr_file ]]; then
		echo "Running $instr_file..."
		$instr_file
	fi
done
echo "Gremlin done!"
service neo4j start
sleep 4

# finish it up with cypher
echo "Doing cypher instrumention queries..."
for dom_csv in $dom_csvs; do
	dtype=$( basename $dom_csv )
	dtype=${dtype%.csv}
	instr_file="instrument/cypher_instr/$dtype.sh"
	if [[ ! -f  $instr_file ]]; then
		echo "Warning: $instr_file not found. Skipping..."
		continue
	fi
	echo "Running $instr_file"
	$instr_file
done

echo "All done!"
