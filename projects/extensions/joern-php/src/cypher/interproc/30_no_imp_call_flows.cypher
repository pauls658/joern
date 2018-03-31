match 

(aentry:ART_AST{type:"arg_entry"})-[:CALL_ID]->(call:FUNCCALL)<-[:CALL_ID|ASSOC]-(aexit:ART_AST)

where 
not (call)-[:CALLS]->() and
aexit.type in ["arg_exit", "return"]

create 
(aentry)-[:REACHES]->(aexit);
