#!/bin/bash

# functions that are common to all the scripts in this folder

script_dir=$( dirname $( readlink -f $0 ) )
cypher_dir=$script_dir/../../../cypher/instrument
# args:
# $1 - the type to load
# afterwards:
# the csv file corresponding to the type will be set as the file to
# load for the instrumentation cypher queries
load_csv_by_type() {
	t=$1
	rm -f $cypher_dir/bbs.csv
	ln -s $script_dir/../../../networkx/pdoms/$t.csv $cypher_dir/bbs.csv
}

run_right_hand_vars() {
	cat $cypher_dir/right_hand_vars.cypher | cypher-shell | tail -n +2
	run_func_calls
}

run_all_vars() {
	cat $cypher_dir/all_vars.cypher | cypher-shell | tail -n +2
	run_func_calls
}

# args:
# $1 - a new-line separated string with ids of where to splice the call
#      to the tainting routine
direct_instr() {
	echo "id
$1" > $cypher_dir/taint.csv
	run_direct_instr
}

run_direct_instr() {
	cat $cypher_dir/direct_instr.cypher | cypher-shell
}

# args:
# $1 - the type
# this function sponsored apache tinkerpop
run_new_vars_by_type() {
	$script_dir/../run_new_stmt_instr.sh "$1" "/home/brandon/joern/projects/extensions/joern-php/src/networkx/pdoms/$1.csv"
}

link_assign_vars_by_type() {
	rm -f $cypher_dir/assign_vars.csv
	ln -s $script_dir/../../../groovy/instrument/$1.csv $cypher_dir/assign_vars.csv
}

create_assigns() {
	cat $cypher_dir/create_assigns.cypher | cypher-shell | tail -n +2
}

run_get_call_id() {
	cat $cypher_dir/get_call_id.cypher | cypher-shell | tail -n +2
}

run_func_calls() {
	cat $cypher_dir/func_calls.cypher | cypher-shell | tail -n +2
}
