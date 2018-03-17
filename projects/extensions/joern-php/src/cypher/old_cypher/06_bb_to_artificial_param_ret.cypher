match 
// sup
(b)-[r:REACHES]->(a{type:"CFG_FUNC_EXIT"}), 
(c:ART_AST{type: "param_exit"}) 

where 
b.funcid = c.funcid and 
r.childnum = c.childnum

create 
(b)-[:REACHES]->(c) 

delete r;
