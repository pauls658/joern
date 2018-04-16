#!/bin/bash

script_dir=$( dirname $( readlink -f $0 ) )
. $script_dir/../common.sh

run_new_vars_by_type "AST_FOREACH"
