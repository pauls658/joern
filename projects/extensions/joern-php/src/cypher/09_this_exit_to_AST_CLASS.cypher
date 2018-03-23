match 

(bb:BB)-[r:REACHES{childnum: -1}]->(exit:Artificial{type: "CFG_FUNC_EXIT"})<-[:EXIT]-(:FUNCDECL)<-[:PARENT_OF]-(:AST{type:"AST_STMT_LIST"})<-[:PARENT_OF]-(:AST{type: "AST_TOPLEVEL"})<-[:PARENT_OF]-(decl:AST{type: "AST_CLASS"})

create 
(bb)-[:INTERPROC{var:r.var}]->(decl);
