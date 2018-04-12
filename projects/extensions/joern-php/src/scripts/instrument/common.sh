#!/bin/bash

# functions that are common to all the scripts in this folder

# args:
# $1 - the type to load
# afterwards:
# the csv file corresponding to the type will be set as the file to
# load for the instrumentation cypher queries
load_csv_by_type() {
	t=$1
	rm -f ../../cypher/instrument/bbs.csv
	ln -s ../../networkx/pdoms/$t.csv ../../cypher/instrument/bbs.csv
}

run_right_hand_vars() {
	cat ../../cypher/instrument/right_hand_vars.cypher | cypher-shell
}

# args:
# $1 - a new-line separated string with ids of where to splice the call
#      to the tainting routine
direct_instr() {
	echo "id
$1" > ../../cypher/instrument/taint.csv
	cat ../../cypher/instrument/direct_instr.cypher | cypher-shell
}
