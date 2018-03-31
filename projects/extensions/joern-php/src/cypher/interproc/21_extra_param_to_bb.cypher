load csv with headers from "file:///home/brandon/joern/projects/extensions/joern-php/src/cypher/interproc/prev.csv" as line 

match (bb),(eparam:ART_AST{type:"extra_param"})

where
ID(bb) = toInteger(line.bbid) and
eparam.funcid = bb.funcid

create
(eparam)-[:REACHES{var:"*"}]->(bb);
