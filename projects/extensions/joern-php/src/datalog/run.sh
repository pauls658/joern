#!/bin/bash

../scripts/dump_ICFG.sh
python datalog_facts.py
./do_z3.sh
