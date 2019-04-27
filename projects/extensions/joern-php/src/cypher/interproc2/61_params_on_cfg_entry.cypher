match 

(entry{type:"CFG_FUNC_ENTRY"})<-[:ENTRY]-(decl:FUNCDECL)-[:PARENT_OF]->(plist{type:"AST_PARAM_LIST"})-[:PARENT_OF]->(param{type:"AST_PARAM"})

set entry.func_params = coalesce(entry.func_params + ";" + param.defs, param.defs);
