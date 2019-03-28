// label basic blocks
match (a)-[:FLOWS_TO|INTERPROC]-() set a:BB;

// label function calls
match (a:AST) where a.type in ["AST_CALL", "AST_METHOD_CALL", "AST_STATIC_CALL", "AST_NEW"] set a:FUNCCALL;

// put function name on the call node
match (a{type:"AST_CALL"})-[r:PARENT_OF]->(b{childnum:0})-[:PARENT_OF]->(n) set a.name = n.code;
match (A{type:"AST_METHOD_CALL"})-[:PARENT_OF]->(n{childnum:1}) set A.name = n.code;
match (A{type:"AST_STATIC_CALL"})-[:PARENT_OF]->(n{childnum:1}) set A.name = n.code;

// label function declarations
match (a:AST) where a.type in ["AST_FUNC_DECL", "AST_METHOD"] or (a)-[:PARENT_OF]->(:AST{type:"AST_PARAM_LIST"}) set a:FUNCDECL;

