#!/bin/bash

script_dir=$( dirname $( readlink -f $0 ) )
. $script_dir/../common.sh

load_csv_by_type "AST_RETURN"

instr_ids=$( run_all_vars )

direct_instr "$instr_ids"
