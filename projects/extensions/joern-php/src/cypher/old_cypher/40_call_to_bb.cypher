match path=(a)-[:PARENT_OF*1..]->(calls) where not any(n in nodes(path) where n:ART_ARG or n.type = "AST_STMT_LIST") and calls.type = "AST_CALL" and a:BB create (calls)-[:REACHES]->(a);
