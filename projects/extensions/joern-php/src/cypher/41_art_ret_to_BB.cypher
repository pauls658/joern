match 

p=(top:BB)-[:PARENT_OF*1..]->(call:FUNCCALL)<-[:ASSOC]-(ret:ART_AST{type: "return"}) 

where ret.call_id = ID(call) and 
single(n in nodes(p) where n:FUNCCALL) and 
single(n in nodes(p) where n:BB) 

create 
(ret)-[:REACHES]->(top);
