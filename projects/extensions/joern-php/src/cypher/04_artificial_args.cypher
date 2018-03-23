match

// we need to know which basic block each artificial arg is associated with, so do this first
p=(bb:BB)-[:PARENT_OF*0..]->(call:FUNCCALL)

where
single(n in nodes(p) where n:BB)

set
call.bb_id = ID(bb);

// Now make the artificial args
match

// yeah, all four types of call nodes use AST_ARG_LIST for their arg nodes
(call:FUNCCALL)-[:PARENT_OF]->(alist:AST{type: "AST_ARG_LIST"})-[:PARENT_OF]->(arg:AST)

create
(entry:ART_AST{type:"arg_entry",call_id:ID(call),childnum:arg.childnum,lineno:arg.lineno,bb_id:call.bb_id}), // artificial entry arg
(exit:ART_AST{type:"arg_exit",call_id:ID(call),childnum:arg.childnum,lineno:arg.lineno,bb_id:call.bb_id}), // artificial exit arg
(entry)-[:ASSOC]->(arg),
(entry)-[:CALL_ID]->(call),
(exit)-[:ASSOC]->(arg),
(exit)-[:CALL_ID]->(call);

//And finally the returns
match
(call:FUNCCALL)

create
(ret:ART_AST{type:"return",call_id:ID(call),lineno:call.lineno,bb_id:call.bb_id}), // artificial entry arg
(ret)-[:ASSOC]->(call);
