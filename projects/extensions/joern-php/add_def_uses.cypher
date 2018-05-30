load csv with headers from "file:///home/brandon/joern/projects/extensions/joern-php/arg_def_uses.csv" as line

match

(a)<-[:ASSOC]-(arg)

where
ID(a) = toInteger(line.id)

set
arg.symbols = line.symbols;

load csv with headers from "file:///home/brandon/joern/projects/extensions/joern-php/BB_def_uses.csv" as line

match

(a)

where
ID(a) = toInteger(line.id)

set
a.defs = line.defs
set
a.uses = line.uses;
