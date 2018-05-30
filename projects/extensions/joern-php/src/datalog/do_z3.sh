#!/bin/bash

cat rules.smt2 > test.smt2
cat facts.smt2 >> test.smt2
cat query.smt2 >> test.smt2

z3 test.smt2 | grep -o -E "#x[0-9a-f]*" > reachables.csv

