match

// we need to know which basic block each artificial arg is associated with, so add 
// this info to the call node
p=(bb:BB)-[:PARENT_OF*0..]->(call:FUNCCALL)
where
single(n in nodes(p) where n:BB)
set
call.bb_id = ID(bb);


// special case: no func args, so make a dummy arg. This serves as the entry arg too (unless its a method call, which is TODO)
match
// yeah, all four types of call nodes use AST_ARG_LIST for their arg nodes
(call:FUNCCALL)-[:PARENT_OF]->(alist:AST{type: "AST_ARG_LIST"})
where
not (alist)-[:PARENT_OF]->() and
call.type <> "AST_METHOD_CALL"
create
(entry:ART_AST{type:"dummy_arg",call_id:ID(call),lineno:call.lineno,bb_id:call.bb_id,funcid:call.funcid,entry_arg:true}), // dummy
(entry)-[:CALL_ID]->(call);

// Make a "this" arg for method calls. This is the entry arg
match
(call{type:"AST_METHOD_CALL"})-[:PARENT_OF]->(b{childnum:0}) 
create
(entry:ART_AST{type:"arg_entry",call_id:ID(call),childnum:-1,lineno:call.lineno,bb_id:call.bb_id,funcid:call.funcid,entry_arg:true,symbols:b.symbols}), 
(entry)-[:CALL_ID]->(call),
(entry)-[:ASSOC]->(b);

// make a return node. This is always the exit argument
match
(call:FUNCCALL)
create
(ret:ART_AST{type:"return",call_id:ID(call),lineno:call.lineno,bb_id:call.bb_id,funcid:call.funcid}), // artificial entry arg
(ret)-[:ASSOC]->(call);

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


// Mark the entry arg for everything except AST_METHOD_CALL
// this is always the 0th child
match
(call)<-[:CALL_ID]-(entry:ART_AST{type:"arg_entry",childnum:0})
where
call.type in ["AST_CALL", "AST_STATIC_CALL", "AST_NEW"]
set
entry.entry_arg = true;
