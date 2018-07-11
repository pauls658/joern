#!/bin/bash

cat cypher/00_indexes.cypher | cypher-shell
cat cypher/01_do_our_labels.cypher | cypher-shell
cat cypher/get_decl_info.cypher | cypher-shell > csv/decl_info.csv

python py/generate_rewrite_csv.py

cat cypher/func_var_rewrite.cypher | cypher-shell
