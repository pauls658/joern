// requires:
// bbs.csv is a file (or sym links to a file) with column "pdoms" that has rows of single basic block ids

// outputs:
// a single column "var_top" where each entry is a single id of where to splice in a call to the tainting routine

load csv with headers from "file:///home/brandon/joern/projects/extensions/joern-php/src/cypher/instrument/bbs.csv" as line

match

upper=(bb:BB)-[:PARENT_OF]->({childnum:1})-[:PARENT_OF*0..]->(last),
(last)-[:PARENT_OF]->(var_top),
lower=(var_top)-[:PARENT_OF*0..]->({type:"AST_VAR"})-[:PARENT_OF]->({type:"string"})

where
ID(bb) = toInteger(line.pdoms) and
none(n in nodes(upper) where n.type in ["AST_VAR","AST_PROP","string","AST_DIM"]) and
all(n in nodes(lower) where n.type in ["AST_VAR","AST_PROP","string","AST_DIM"]) and
none(n in nodes(upper) + nodes(lower) where n:FUNCCALL or n.type = "AST_CONDITIONAL")

// for now just give back the node where to "splice" in the tainting routine
return ID(var_top) as var_top;
