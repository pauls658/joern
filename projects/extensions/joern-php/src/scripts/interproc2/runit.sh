#!/bin/bash

cat cypher/get_decl_info.cypher | cypher-shell > csv/decl_info.csv

python py/generate_rewrite_csv.py

cat cypher/func_var_rewrite.cypher | cypher-shell
