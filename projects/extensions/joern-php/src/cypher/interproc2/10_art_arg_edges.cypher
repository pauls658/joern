///////////////////////////////////////////////
// Connect the artificial args/rets together //
///////////////////////////////////////////////

// connect entries to each other
// 0 -> 1 -> 2...
match

(a:FUNCCALL)<-[:CALL_ID]-(art_arg1{type:"arg_entry"}),
(a)<-[:CALL_ID]-(art_arg2{type:"arg_entry"})

where
art_arg1.childnum = art_arg2.childnum + 1

create // it looks backwards, but i promise its not
(art_arg1)<-[:FLOWS_TO]-(art_arg2);

// Connect exits to each other
// ... 2 -> 1 -> 0
match

(a:FUNCCALL)<-[:CALL_ID]-(art_arg1{type:"arg_exit"}),
(a)<-[:CALL_ID]-(art_arg2{type:"arg_exit"})

where
art_arg1.childnum = art_arg2.childnum - 1

create // it looks backwards, but i promise its not
(art_arg1)<-[:FLOWS_TO]-(art_arg2);

// finally attach the first child to the ret
// 0 -> ret
match

(a:FUNCCALL)<-[:ASSOC]-(ret{type:"return"}),
(a)<-[:CALL_ID]-(arg_exit{type:"arg_exit", childnum:0})

create
(arg_exit)-[:FLOWS_TO]->(ret);


///////////////////////////////////////////////
// Make appropriate edges to artificial args //
///////////////////////////////////////////////

////// Normal case: the function has arguments
// first do the arg entries. This query moves incoming CFG edges to the deepest func call artificial args
match

(origin)-[inc:FLOWS_TO]->(bb:BB)-[:PARENT_OF*0..]->(call:FUNCCALL)<-[:CALL_ID]-(entry{type:"arg_entry",childnum:0})

where
not exists(call.not_deepest) and
not exists(inc.new_edge) and
ID(bb) = call.bb_id // make sure we don't match the parent in a nested blocks (e.g. a loop)

create
(origin)-[:FLOWS_TO{new_edge:true}]->(entry)

set
inc.delete = true;

// next the return. this query connects the shallowest artifical rets to the top level basic block
match 

p=(top:BB)-[:PARENT_OF*0..]->(call:FUNCCALL)<-[:ASSOC]-(ret:ART_AST{type: "return"}) 

where ret.call_id = ID(call) and 
single(n in nodes(p) where n:FUNCCALL) and 
single(n in nodes(p) where n:BB) 

create 
(ret)-[:FLOWS_TO{new_edge:true}]->(top),
(ret)-[:RET_DEF]->(top);

// now do nested calls. connects the return to the first arg entry. no special case here because all function calls
// have a return, and a nested func call must have at least one argument
match 

p=(top:FUNCCALL)-[:PARENT_OF]->(alist:AST{type:"AST_ARG_LIST"})-[:PARENT_OF]->(arg:AST)-[:PARENT_OF*0..]->(call:FUNCCALL),

(call)<-[:ASSOC]-(ret:ART_AST{type: "return"}), // return of nested call
(arg)<-[:ASSOC]-(assoc_arg:ART_AST{type: "arg_entry"}), // associated artificial arg of top call
(top)<-[:CALL_ID]-(entry:ART_AST{type:"arg_entry", childnum: 0}) // first artificial arg of top call

where 
single(n in tail(nodes(p)) where n:FUNCCALL) // make sure we only deal with one level of nested call

create 
(ret)-[:FLOWS_TO]->(entry),
(ret)-[:RET_DEF]->(assoc_arg);

////// Special case: no args/params
// create edges to dummy func entry. this is a copy of the normal case, but with type = "dummy_arg"
match

(origin)-[inc:FLOWS_TO]->(:BB)-[:PARENT_OF*0..]->(call:FUNCCALL)<-[:CALL_ID]-(entry{type:"dummy_arg"})

where
not exists(call.not_deepest) and
not exists(inc.new_edge)

create
(origin)-[:FLOWS_TO{new_edge:true}]->(entry)

set
inc.delete = true;

match

()-[r{delete:true}]-()

delete r;
