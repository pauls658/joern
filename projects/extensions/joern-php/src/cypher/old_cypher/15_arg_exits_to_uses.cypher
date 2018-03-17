match 

// make sure only one BB in path to call
q=(bb:BB)-[:PARENT_OF*0..]->(call:FUNCCALL),

// get all the outgoing REACHES edges
(bb)-[r:REACHES]->(use),

// get each top level arg 
(call:FUNCCALL)-[:PARENT_OF]->(alist:AST{type:"AST_ARG_LIST"})-[:PARENT_OF]->(arg:AST),

// make sure only one call expression between AST_CALL and arg name (avoid getting nested func call arg name)
// also make sure we only get variables, i.e. no hard-coded args
p=(arg:AST)-[PARENT_OF*0..]->(var:AST{type: "AST_VAR"})-[:PARENT_OF]->(name:AST{type:"string"}),

(exit:ART_AST{type:"arg_exit"}) // finally, get the corresponding artificial node

where 
single(n in nodes(q) where n:BB) and // make sure we are considering the immediate basic block
none(n in nodes(p) where n:FUNCCALL) and // no nested func calls
name.code = r.var and // the reaches edge corresponds to this argument
ID(call) = exit.call_id and // artificial node is associated with this AST_CALL node
exit.childnum = arg.childnum // verify arg numbers

create 
(use)<-[:REACHES{var: name.code}]-(exit) 
delete r;
