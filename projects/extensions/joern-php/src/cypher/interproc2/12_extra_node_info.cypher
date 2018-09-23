// put the FUNC_EXIT id on the FUNC_ENTRY
match 
(a:Artificial{type:"CFG_FUNC_ENTRY"})<-[:ENTRY]-()-[:EXIT]->(e)
set 
a.exit_id = ID(e);

// put the arg_exit/return id on the entry interproc edge
// first handle case where no args, so exit is the artif return
match
()<-[r:INTERPROC{type:"entry"}]-(entry)-[:CALL_ID]->()<-[:ASSOC]-(exit{type:"return"})<-[:INTERPROC{type:"exit"}]-()
set r.exit_id = ID(exit);


// now handle the case where exit is an artif arg
match
()<-[r:INTERPROC{type:"entry"}]-(entry)-[:CALL_ID]->()<-[:CALL_ID]-(exit)<-[:INTERPROC{type:"exit"}]-()
set r.exit_id = ID(exit);
