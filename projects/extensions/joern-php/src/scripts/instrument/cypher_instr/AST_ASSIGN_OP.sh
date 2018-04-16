#!/bin/bash

script_dir=$( dirname $( readlink -f $0 ) )
. $script_dir/../common.sh

load_csv_by_type "AST_ASSIGN_OP"

instr_ids=$( run_right_hand_vars )

direct_instr "$instr_ids"
