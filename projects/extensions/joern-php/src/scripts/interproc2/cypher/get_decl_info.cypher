match 

//(decl:FUNCDECL)-[:PARENT_OF]->(:AST{type:"AST_PARAM_LIST"})-[:PARENT_OF]->(p:AST{type:"AST_PARAM"})-[:PARENT_OF]->(name{childnum:1})
(decl:FUNCDECL)

optional match
(decl)-[:PARENT_OF]->(:AST{type:"AST_STMT_LIST"})-[:PARENT_OF*1..]->(p:AST{type:"AST_GLOBAL"})-[:PARENT_OF]->(:AST{type:"AST_VAR"})-[:PARENT_OF]->(name{type:"string"})

return distinct
decl.name as func_name,
ID(decl) as func_id,
name.code as global_name;
