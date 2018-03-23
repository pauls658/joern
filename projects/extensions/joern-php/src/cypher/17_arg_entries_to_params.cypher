match 

(a:ART_AST{type:"arg_entry"})-[:CALL_ID]->(call:FUNCCALL)-[:CALLS]->(decl:FUNCDECL)-[:PARENT_OF]->(plist:AST{type: "AST_PARAM_LIST"})-[:PARENT_OF]->(p:AST{type: "AST_PARAM"}) 

where 
a.childnum = p.childnum 

create 
(a)-[:INTERPROC{call_id:a.call_id, type:a.type}]->(p);
