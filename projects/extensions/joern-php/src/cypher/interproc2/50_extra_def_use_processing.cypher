// create def val for AST_RETURN
match
(decl:FUNCDECL)-[:PARENT_OF]->(stmts:AST{type:"AST_STMT_LIST"})-[:PARENT_OF*1..]->(ret:AST{type:"AST_RETURN"})
set
decl.ret_def = toString(ID(decl)) + "_ret" 
set 
ret.defs = toString(ID(decl)) + "_ret";


// create a def for the actual return
match
(ret:ART_AST{type:"return"})
set
ret.defs = coalesce(toString(ret.call_id) + "_actual_ret;" + ret.defs, ret.call_id + "_actual_ret")
set
ret.actual_ret_name = toString(ret.call_id) + "_actual_ret";


// create a use for the actual return (indicated by "RET_DEF", which in hindsight was a bad choice of name)
match
(BB)<-[:RET_DEF]-(ret)
set
BB.uses = coalesce(BB.uses + ";" + ret.actual_ret_name, ret.actual_ret_name);


// create a use for the formal ret
match
(decl)<-[:CALLS]-(call)<-[:ASSOC]-(ret:ART_AST{type:"return"})-[:RET_DEF]->(BB)
set
ret.uses = coalesce(decl.ret_def + ";" + ret.uses, decl.ret_def);


// arif args
match
(aentry:ART_AST{type:"arg_entry"})-[:CALL_ID]->(call)<-[:CALL_ID]-(aexit:ART_AST{type:"arg_exit"}),
(call)-[:CALLS]->(decl:FUNCDECL)-[:PARENT_OF]->(:AST{type:"AST_PARAM_LIST"})-[:PARENT_OF]->(param:AST{type:"AST_PARAM"})
where
aentry.childnum = aexit.childnum and 
aentry.childnum = param.childnum 
set aentry.defs = coalesce(aentry.defs + ";" + param.defs, param.defs)
set aexit.uses = coalesce(aexit.uses + ";" + param.defs, param.defs)
set aexit.flags = coalesce(param.flags, ["PARAM_VAL"]);

// "this" arg
match
(aentry:ART_AST{type:"arg_entry",childnum:-1})-[:CALL_ID]->(call),
(call)-[:CALLS]->(decl:FUNCDECL)-[:PARENT_OF]->(:AST{type:"AST_PARAM_LIST"})-[:PARENT_OF]->(param:AST{type:"AST_PARAM",childnum:-1})
set aentry.defs = coalesce(aentry.defs + ";" + param.defs, param.defs);

// remove AST global defs
match
(a{type:"AST_GLOBAL"})
remove a.defs;
