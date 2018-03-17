match 

//TODO: other func call types
// make sure only one BB in path to AST_CALL
q=(bb:BB)-[:PARENT_OF*0..]->(call:AST{type:"AST_CALL"}),

// get all the incoming REACHES edges
(bb)<-[r:REACHES]-(def),

// get each top level arg 
(call:AST{type:"AST_CALL"})-[:PARENT_OF]->(alist:AST{type:"AST_ARG_LIST"})-[:PARENT_OF]->(arg:AST),

// make sure only one call expression between AST_CALL and arg name (avoid getting nested func call arg name)
// also make sure we only get variables, i.e. no hard-coded args
p=(arg:AST)-[PARENT_OF*0..]->(var:AST{type: "AST_VAR"})-[:PARENT_OF]->(name:AST{type:"string"}),

(entry:ART_AST{type:"arg_entry"}) // finally, get the corresponding artificial node

where 
single(n in nodes(q) where n:BB) and // make sure we are considering the immediate basic block
none(n in nodes(p) where n.type in ["AST_CALL", "AST_METHOD_CALL", "AST_STATIC_CALL", "AST_NEW"]) and // no nested func calls
name.code = r.var and // the reaches edge corresponds to this argument
ID(call) = entry.call_id and // artificial node is associated with this AST_CALL node
entry.childnum = arg.childnum // verify arg numbers

create 
(def)-[:REACHES{var: name.code}]->(entry) 
delete r;
