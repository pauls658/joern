///////////////////////////////////////////////
// Connect the artificial args/rets together //
///////////////////////////////////////////////


// connect entries to each other
// 0 -> 1 -> 2...
// TODO: method call "this" arg
match
(a:FUNCCALL)<-[:CALL_ID]-(art_arg1{type:"arg_entry"}),
(a)<-[:CALL_ID]-(art_arg2{type:"arg_entry"})
where
art_arg1.childnum = art_arg2.childnum + 1
create // it looks backwards, but i promise its not
(art_arg1)<-[:FLOWS_TO]-(art_arg2);



// Connect exits to each other
// ... 2 -> 1 -> 0
// TODO: method call "this" arg
match
(a:FUNCCALL)<-[:CALL_ID]-(art_arg1{type:"arg_exit"}),
(a)<-[:CALL_ID]-(art_arg2{type:"arg_exit"})
where
art_arg1.childnum = art_arg2.childnum - 1
create // it looks backwards, but i promise its not
(art_arg1)<-[:FLOWS_TO]-(art_arg2);



// finally attach the first child to the ret
// 0 -> ret
// We don't make artificial "this" exit arg, so it is always 0 -> ret
match
(a:FUNCCALL)<-[:ASSOC]-(ret{type:"return"}),
(a)<-[:CALL_ID]-(arg_exit{type:"arg_exit", childnum:0})
create
(arg_exit)-[:FLOWS_TO]->(ret);


///////////////////////////////////////////////
// Make appropriate edges to artificial args //
///////////////////////////////////////////////


// move incoming basic block edges to the first function call of basic block
match
(origin)-[inc:FLOWS_TO]->(bb:BB)-[:PARENT_OF*0..]->(call:FUNCCALL{first_call:true})<-[:CALL_ID]-(entry{entry_arg:true})
where
not exists(inc.new_edge) and
ID(bb) = call.bb_id // make sure we don't match the parent in a nested blocks (e.g. a loop)
create
(origin)-[:FLOWS_TO{new_edge:true}]->(entry)
set
inc.delete = true;


// connect the return of the last function call to the basic block
match
(bb:BB)-[:PARENT_OF*0..]->(call:FUNCCALL{last_call:true})<-[:ASSOC]-(ret:ART_AST{type:"return"})
where
ID(bb) = call.bb_id // make sure we don't match the parent in a nested block (e.g. a loop)
create 
(ret)-[:FLOWS_TO{new_edge:true}]->(bb);


// do the middle calls
// we don't rely on child nums, so artif "this" arg won't affect us
match
(prev_call:FUNCCALL)<-[:ASSOC]-(prev_ret:ART_AST{type:"return"}),
(prev_call)-[:NEXT_CALL]->(call),
(call:FUNCCALL)<-[:CALL_ID]-(entry_arg:ART_AST{entry_arg:true})
create
(prev_ret)-[:FLOWS_TO{new_edge:true}]->(entry_arg);


// now do the ret_defs
// Frist do the shallowest calls that return to the top-level basic block
match
p=(bb:BB)-[:PARENT_OF*0..]->(call:FUNCCALL)<-[:ASSOC]-(ret:ART_AST{type:"return"})
where
ID(bb) = call.bb_id and // make sure we don't match the parent in a nested blocks (e.g. a loop)
ret.call_id = ID(call) and  // maybe not necessary
single(n in nodes(p) where n:FUNCCALL)

create 
(ret)-[:RET_DEF]->(bb);

// then do nested calls
match 

p=(top:FUNCCALL)-[:PARENT_OF]->(alist:AST{type:"AST_ARG_LIST"})-[:PARENT_OF]->(arg:AST)-[:PARENT_OF*0..]->(call:FUNCCALL),
(call)<-[:ASSOC]-(ret:ART_AST{type: "return"}), // return of nested call
(arg)<-[:ASSOC]-(assoc_arg:ART_AST{type: "arg_entry"}) // associated artificial arg of top call

where 
single(n in tail(nodes(p)) where n:FUNCCALL) // make sure we only deal with one level of nested call

// handle case where return value of a function is the calling object
match (a{type:"AST_METHOD_CALL"})<-[:ASSOC]-(entry{type:"arg_entry", childnum:-1}) where not exists(a.uses) set entry.uses = toString(a.id) + "_actual_ret";

create 
(ret)-[:RET_DEF]->(assoc_arg);


// delete the deleted edges
match ()-[r{delete:true}]-() delete r;
