// this needs to incorporate art_ast too
match 

(bb),
(exit:Artificial{type:"CFG_FUNC_EXIT"})  

where
not (bb)-[:REACHES]->() and 
bb.funcid = exit.funcid and
(bb:BB or 
 (bb:ART_AST and (bb.type = "arg_exit" or bb.type = "return")))

create (bb)-[:REACHES]->(exit);
