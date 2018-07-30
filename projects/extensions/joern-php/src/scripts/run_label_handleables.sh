#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"
GSHELL=/home/brandon/tinkerpop/gshell.sh
$GSHELL -e $DIR/../groovy/labelhandleables.groovy
