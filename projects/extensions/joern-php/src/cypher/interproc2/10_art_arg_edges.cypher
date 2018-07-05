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
ID(bb) = call.bb_id // make sure we don't match the parent in a nested blocks (e.g. a loop)
create 
(ret)-[:FLOWS_TO{new_edge:true}]->(bb);


// do the middle calls
match
(prev_call:FUNCCALL)<-[:ASSOC]-(prev_ret:ART_AST{type:"return"}),
(prev_call)-[:NEXT_CALL]->(call),
(call:FUNCCALL)<-[:CALL_ID]-(entry_arg:ART_AST{entry_arg:true})
create
(prev_ret)-[:FLOWS_TO{new_edge:true}]->(entry_arg);

// now do the ret_defs

match

()-[r{delete:true}]-()

delete r;
