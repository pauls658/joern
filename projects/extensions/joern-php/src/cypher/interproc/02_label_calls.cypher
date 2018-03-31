match (a:AST) where a.type in ["AST_CALL", "AST_METHOD_CALL", "AST_STATIC_CALL", "AST_NEW"] set a:FUNCCALL;
