match
(call:FUNCCALL)

create
(ret:ART_AST{type:"return",call_id:ID(call),lineno:call.lineno}), // artificial entry arg
(ret)-[:ASSOC]->(call);
