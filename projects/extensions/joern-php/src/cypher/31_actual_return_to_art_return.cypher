match 

(art_ret:ART_AST{type: "return"})-[:ASSOC]->(call:FUNCCALL)-[:CALLS]->(decl:FUNCDECL),
(actual_ret:AST{type: "AST_RETURN"})


where
actual_ret.funcid = ID(decl)

create 
(actual_ret)-[:INTERPROC{call_id:art_ret.call_id}]->(art_ret);
