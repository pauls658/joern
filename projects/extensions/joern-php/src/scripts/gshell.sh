#!/bin/bash

service neo4j stop
GSHELL=/home/brandon/tinkerpop/gshell.sh
$GSHELL -i /home/brandon/joern/projects/extensions/joern-php/src/groovy/func_summaries.groovy $@
