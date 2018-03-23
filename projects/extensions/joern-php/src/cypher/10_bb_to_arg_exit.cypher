match 

// get the CFG_FUNC_EXIT for the function being called
(aexit:ART_AST{type: "arg_exit"})-[:CALL_ID]->(call:FUNCCALL)-[:CALLS]->(decl:FUNCDECL)-[:EXIT]->(fexit:Artificial{type: "CFG_FUNC_EXIT"})<-[r:REACHES]-(bb)

where 
r.childnum = aexit.childnum 

create 
(bb)-[:INTERPROC{call_id: aexit.call_id, type: aexit.type}]->(aexit);
