match (bb:BB),(exit:Artificial{type:"CFG_FUNC_EXIT"})  where not (bb)-[:REACHES]->() and bb.funcid = exit.funcid create (bb)-[:REACHES]->(exit);
