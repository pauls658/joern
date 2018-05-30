match

// we need to know which basic block each artificial arg is associated with, so do this first
p=(bb:BB)-[:PARENT_OF*0..]->(call:FUNCCALL)

where
single(n in nodes(p) where n:BB)

set
call.bb_id = ID(bb);

// special case: no func args
// TODO: maybe this shouldn't be how it works for method calls? cause of the -1 param
match

// yeah, all four types of call nodes use AST_ARG_LIST for their arg nodes
(call:FUNCCALL)-[:PARENT_OF]->(alist:AST{type: "AST_ARG_LIST"})

where
not (alist)-[:PARENT_OF]->()

create
(entry:ART_AST{type:"dummy_arg",call_id:ID(call),lineno:call.lineno,bb_id:call.bb_id,funcid:call.funcid}), // dummy
(entry)-[:CALL_ID]->(call);

// make a return node
match
(call:FUNCCALL)

create
(ret:ART_AST{type:"return",call_id:ID(call),lineno:call.lineno,bb_id:call.bb_id,funcid:call.funcid}), // artificial entry arg
(ret)-[:ASSOC]->(call);

// make the art args for actual args
match

// yeah, all four types of call nodes use AST_ARG_LIST for their arg nodes
(call:FUNCCALL)-[:PARENT_OF]->(alist:AST{type: "AST_ARG_LIST"})-[:PARENT_OF]->(arg:AST)

create
(entry:ART_AST{type:"arg_entry",call_id:ID(call),childnum:arg.childnum,lineno:arg.lineno,bb_id:call.bb_id,funcid:arg.funcid,symbols:arg.symbols}), // artificial entry arg
(exit:ART_AST{type:"arg_exit",call_id:ID(call),childnum:arg.childnum,lineno:arg.lineno,bb_id:call.bb_id,funcid:arg.funcid,symbols:arg.symbols}), // artificial exit arg
(entry)-[:ASSOC]->(arg),
(entry)-[:CALL_ID]->(call),
(exit)-[:ASSOC]->(arg),
(exit)-[:CALL_ID]->(call);
