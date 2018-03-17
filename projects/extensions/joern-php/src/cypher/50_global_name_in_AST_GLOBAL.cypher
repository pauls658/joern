match (b:AST{type:"AST_GLOBAL"})-[:PARENT_OF]->(v:AST{type: "AST_VAR"})-[:PARENT_OF]->(name:AST{type: "string"}) set b.globalName = name.code;
