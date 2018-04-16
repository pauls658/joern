#!/bin/bash

service neo4j stop
GSHELL=/home/brandon/tinkerpop/gshell.sh
$GSHELL -e /home/brandon/joern/projects/extensions/joern-php/src/groovy/new_stmt_instr.groovy $@
