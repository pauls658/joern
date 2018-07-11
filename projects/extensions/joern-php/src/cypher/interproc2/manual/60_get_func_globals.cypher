match
(decl:FUNCDECL)
optional match
(decl)-[:PARENT_OF]->(:AST{type:"AST_STMT_LIST"})-[:PARENT_OF*1..]->(p:AST{type:"AST_GLOBAL"})-[:PARENT_OF]->(:AST{type:"AST_VAR"})-[:PARENT_OF]->(name{type:"string"})

with decl as d, collect(distinct name.code) as globals

return ID(d) as func_id, reduce(s = "", g in globals | s + g + ";") as globals;
