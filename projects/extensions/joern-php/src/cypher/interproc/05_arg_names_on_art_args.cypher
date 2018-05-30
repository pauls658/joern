match

(artarg:ART_AST)-[:ASSOC]->(arg:AST),
p=(arg)-[:PARENT_OF*0..]->(:AST{type:"AST_VAR"})-[:PARENT_OF]->(name:AST{type: "string"})

where
none(n in nodes(p) where n:FUNCCALL) and
artarg.type in ["arg_entry", "arg_exit"]

set
artarg.argname = coalesce(artarg.argname + [name.code], [name.code]);
