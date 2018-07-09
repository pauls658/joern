#!/bin/bash

if [[ -z $1 ]]; then
	echo "usage: ./$0 <entry-id>"
	exit 1
fi

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

mapped_id=$( $DIR/map_node.sh $1 rev )

sed -e "s/{{id}}/$mapped_id/g" $DIR/../souffle/flow_reach.dl
