match

(artarg:ART_AST)-[:ASSOC]->(arg:AST),
p=(arg)-[:PARENT_OF*0..]->(:AST{type:"AST_VAR"})-[:PARENT_OF]->(name:AST{type: "string"})

where
none(n in tail(nodes(p)) where n:FUNCCALL)

set
artarg.argname = name.code;
