#!/bin/bash

script_dir=$( dirname $( readlink -f $0 ) )
. $script_dir/../common.sh

link_assign_vars_by_type "AST_PARAM"

instr_ids=$( create_assigns )

direct_instr "$instr_ids"
