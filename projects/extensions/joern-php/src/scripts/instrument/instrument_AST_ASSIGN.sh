#!/bin/bash

. common.sh

load_csv_by_type "AST_ASSIGN"

instr_ids=$( run_right_hand_vars | tail -n +2 )

direct_instr "$instr_ids"
