match
// match each arg of each call, TODO: "AST_METHOD_CALL", "AST_STATIC_CALL", "AST_NEW"
(call{type: "AST_CALL"})

create
(ret:ART_AST{type:"return",call_id:ID(call)}); // artificial ret arg
