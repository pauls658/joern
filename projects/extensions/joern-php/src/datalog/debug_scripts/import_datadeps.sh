#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

python $DIR/../debug_tools.py datadeps
cat $DIR/../import_datadeps.cypher | cypher-shell
