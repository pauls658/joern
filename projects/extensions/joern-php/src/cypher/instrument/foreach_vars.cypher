match

(a:AST{type:"AST_FOREACH"})-[:PARENT_OF]->(var),
var_path=(var)-[:PARENT_OF*0..]->({type:"string"}),
(a)-[:PARENT_OF]->(stmts{childnum:3})

where
var.childnum in [1, 2] and // child 1 is the val, child 2 is the key
any(n in nodes(var_path) where n.type = "AST_VAR")

unwind nodes(var_path) as n

create (new:AST{type:n.type})

with collect(new) as new_nodes

return new_nodes;
