match 

(a:ART_AST{type:"arg_entry"})-[:CALL_ID]->(call:FUNCCALL)-[:CALLS]->(decl:FUNCDECL)-[:PARENT_OF]->(plist:AST{type: "AST_PARAM_LIST"})-[:EXTRA_PARAM]->(p:ART_AST{type: "extra_param"}) 

where 
not (a)-[:INTERPROC]->()

create 
(a)-[:INTERPROC{call_id:a.call_id, type:a.type}]->(p);
