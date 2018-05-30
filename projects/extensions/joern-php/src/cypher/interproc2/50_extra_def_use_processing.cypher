// create def val for AST_RETURN
match
(decl:FUNCDECL)-[:PARENT_OF]->(stmts:AST{type:"AST_STMT_LIST"})-[:PARENT_OF*1..]->(ret:AST{type:"AST_RETURN"})
set
decl.ret_def = toString(ID(decl)) + "_ret" 
set 
ret.defs = toString(ID(decl)) + "_ret";

// now add the ret defs
match
(decl)<-[:CALLS]-(call)<-[:ASSOC]-(a:ART_AST{type:"return"})-[:RET_DEF]->(BB)
set
BB.uses = coalesce(BB.uses + ";" + decl.ret_def, decl.ret_def);

// arif args
match
(aentry:ART_AST{type:"arg_entry"})-[:CALL_ID]->(call)<-[:CALL_ID]-(aexit:ART_AST{type:"arg_exit"}),
(call)-[:CALLS]->(decl:FUNCDECL)-[:PARENT_OF]->(:AST{type:"AST_PARAM_LIST"})-[:PARENT_OF]->(param:AST{type:"AST_PARAM"})
where
aentry.childnum = aexit.childnum and 
aentry.childnum = param.childnum 
set 
aentry.defs = param.defs set aexit.uses = param.defs;

// remove AST global defs
match
(a{type:"AST_GLOBAL"})
remove a.defs;
