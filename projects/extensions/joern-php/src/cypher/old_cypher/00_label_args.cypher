match (list)-[:PARENT_OF]->(arg) where list.type = "AST_ARG_LIST" set arg:ARG;
