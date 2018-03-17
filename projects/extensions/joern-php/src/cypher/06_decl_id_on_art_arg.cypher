match

(artarg:ART_AST)-[:CALL_ID]->(call:FUNCCALL)-[:CALLS]->(decl:FUNCDECL)

set
artarg.decl_id = ID(decl);
