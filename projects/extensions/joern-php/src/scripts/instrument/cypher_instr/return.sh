#!/bin/bash

script_dir=$( dirname $( readlink -f $0 ) )
. $script_dir/../common.sh

load_csv_by_type "return"

instr_ids=$( run_get_call_id )

direct_instr "$instr_ids"
