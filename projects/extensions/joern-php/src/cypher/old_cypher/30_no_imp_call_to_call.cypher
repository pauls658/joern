match (call)-[:PARENT_OF]->(alist)-[:PARENT_OF]-(arg) where not (call)-[:CALLS]->() and call.type = "AST_CALL" and alist.type = "AST_ARG_LIST" create (arg)-[:REACHES]->(call);
