/////////////////
// arg entries //
/////////////////
// normal case: have args
match 
(a:ART_AST{type:"arg_entry"})-[:CALL_ID]->(call:FUNCCALL)-[:CALLS]->(decl:FUNCDECL)-[:ENTRY]->(func_entry)
where 
not (a)-[:FLOWS_TO]->()
create 
(a)-[:INTERPROC{type:"entry"}]->(func_entry);



// special case: no args
match 
(a:ART_AST{type:"dummy_arg"})-[:CALL_ID]->(call:FUNCCALL)-[:CALLS]->(decl:FUNCDECL)-[:ENTRY]->(func_entry)
where 
not (a)-[:FLOWS_TO]->()
create 
(a)-[:INTERPROC{type:"entry"}]->(func_entry);



///////////////
// arg exits //
///////////////
match 
(a:ART_AST{type:"arg_exit"})-[:CALL_ID]->(call:FUNCCALL)-[:CALLS]->(decl:FUNCDECL)-[:EXIT]->(func_exit)
where 
not ()-[:FLOWS_TO]->(a)
create 
(a)<-[:INTERPROC{type:"exit"}]-(func_exit);



match 
(a:ART_AST{type:"return"})-[:ASSOC]->(call:FUNCCALL)-[:CALLS]->(decl:FUNCDECL)-[:EXIT]->(func_exit)
where 
not ()-[:FLOWS_TO]->(a)
create 
(a)<-[:INTERPROC{type:"exit"}]-(func_exit);




///////////////////////
// no implementation //
///////////////////////
match 
(aentry:ART_AST)-[:CALL_ID]->(call:FUNCCALL)<-[r]-(aexit:ART_AST)
where 
not exists(call.done) and
not (call)-[:CALLS]->() and
((aexit.type = "arg_exit" and type(r)="CALL_ID") or (aexit.type = "return" and type(r)="ASSOC"))
and aentry.type in ["arg_entry", "dummy_arg"]
and not (aentry)-[:FLOWS_TO]->()
and not ()-[:FLOWS_TO]->(aexit)
create 
(aentry)-[:FLOWS_TO]->(aexit);
