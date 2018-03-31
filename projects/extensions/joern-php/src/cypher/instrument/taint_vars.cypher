load csv with headers from "file:///home/brandon/taint.csv" as line

match 
(p:AST)-[r:PARENT_OF]->(a) 

where 
ID(a) = toInteger(line.id)

create 
(p)-[:PARENT_OF{child_rel: r.child_rel}]->(call:AST{type:"AST_CALL",lineno:a.lineno})-[:PARENT_OF{child_rel:"args"}]->(:AST{type:"AST_ARG_LIST",lineno:a.lineno})-[:PARENT_OF{child_rel: 0}]->(a),
(call)-[:PARENT_OF{child_rel:"expr"}]->(dim:AST{type:"AST_DIM",lineno:a.lineno})-[:PARENT_OF{child_rel:"expr"}]->(:AST{type:"AST_VAR",lineno:a.lineno})-[:PARENT_OF{child_rel:"name"}]->(:AST{type:"string",code:"GLOBALS",lineno:a.lineno}),
(dim)-[:PARENT_OF{child_rel:"dim"}]->(:AST{type:"string",code:"DBRT",lineno:a.lineno})

set
r.delete = true;

match

(a)

where
not exists(a.id)

set
a.id = ID(a);

match

()-[r]-()

where
exists(r.delete)

delete r;
