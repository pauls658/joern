// requires:
// bbs.csv is a file (or sym links to a file) with column "pdoms" that has rows of single basic block ids

// outputs:
// a single column "var_top" where each entry is a single id of where to splice in a call to the tainting routine

load csv with headers from "file:///home/brandon/joern/projects/extensions/joern-php/src/cypher/instrument/bbs.csv" as line

match

upper=(bb:BB)-[:PARENT_OF*0..]->(var_top:FUNCCALL)

where
ID(bb) = toInteger(line.pdoms) and
single(n in nodes(upper) where n:FUNCCALL)

// for now just give back the node where to "splice" in the tainting routine
return ID(var_top) as var_top;
