#!/bin/bash

dom_csvs=$( ls ../networkx/pdoms/* )
for dom_csv in $dom_csvs; do
	dtype=$( basename $dom_csv )
	dtype=${dtype%.csv}
	instr_file="instrument/instrument_$dtype.sh"
	if [[ ! -f  $instr_file ]]; then
		echo "No instrumentation for $dtype exists!"
		exit 1
	fi
	$instr_file
done
