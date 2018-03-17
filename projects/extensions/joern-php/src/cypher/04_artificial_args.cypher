match

// yeah, all four types of call nodes use AST_ARG_LIST for their arg nodes
p=(bb:BB)-[:PARENT_OF*0..]->(call:FUNCCALL)-[:PARENT_OF]->(alist:AST{type: "AST_ARG_LIST"})-[:PARENT_OF]->(arg:AST)

where
single(n in nodes(p) where n:BB)

create
(entry:ART_AST{type:"arg_entry",call_id:ID(call),childnum:arg.childnum,lineno:arg.lineno,bb_id:ID(bb)}), // artificial entry arg
(exit:ART_AST{type:"arg_exit",call_id:ID(call),childnum:arg.childnum,lineno:arg.lineno,bb_id:ID(bb)}), // artificial exit arg
(entry)-[:ASSOC]->(arg),
(entry)-[:CALL_ID]->(call),
(exit)-[:ASSOC]->(arg),
(exit)-[:CALL_ID]->(call);
