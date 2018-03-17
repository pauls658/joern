match (a:AST) where a.type in ["AST_FUNC_DECL", "AST_METHOD"] or (a)-[:PARENT_OF]->(:AST{type:"AST_PARAM_LIST"}) set a:FUNCDECL;
