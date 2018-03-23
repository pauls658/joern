match
(call:FUNCCALL)

create
(ret:ART_AST{type:"return",call_id:ID(call),lineno:call.lineno,bb_id:call.bb_id}), // artificial entry arg
(ret)-[:ASSOC]->(call);
