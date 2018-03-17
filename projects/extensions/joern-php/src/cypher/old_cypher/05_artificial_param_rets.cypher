match 
(a{type: "AST_PARAM_LIST"})-[:PARENT_OF]->(b{type: "AST_PARAM"}) 

create 
(c:ART_AST{childnum:b.childnum, funcid:b.funcid, type:"param_exit"});
