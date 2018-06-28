#!/bin/bash

cat rules.smt2 > test.smt2
cat tmp/facts >> test.smt2
cat query.smt2 >> test.smt2

z3 test.smt2 > reachable.csv #| grep -o -E "#x[0-9a-f]*" > reachables.csv
