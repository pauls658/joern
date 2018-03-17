match path=(end)-[:PARENT_OF*1..]->(call) where call.type = "AST_CALL" and single(n in nodes(path) where n:ART_ARG) and end:ART_ARG create (call)-[:REACHES]->(end);
