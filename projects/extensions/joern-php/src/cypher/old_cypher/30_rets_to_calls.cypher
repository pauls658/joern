match (ret{type:"AST_RETURN"})-[:FLOWS_TO]->(exit{type:"CFG_FUNC_EXIT"})<-[:EXIT]-(decl)<-[:CALLS]-(call{type: "AST_CALL"}) create (ret)-[:INTERPROC]->(call);
