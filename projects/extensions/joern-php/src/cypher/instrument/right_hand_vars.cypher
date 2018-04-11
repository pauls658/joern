// required params:
// bb_id: the id of the basic block to search for vars
// var_types: the node types that can make up a single variable
export var_types=["AST_VAR", "AST_PROP", "string", "AST_DIM"];
export bb_id=29356

match

upper=(bb:BB)-[:PARENT_OF]->({childnum:1})-[:PARENT_OF*0..]->(last),
(last)-[:PARENT_OF]->(var_top),
lower=(var_top)-[:PARENT_OF*0..]->({type:"AST_VAR"})-[:PARENT_OF]->({type:"string"})

where
ID(bb) = toInteger({bb_id}) and
none(n in nodes(upper) where n.type in {var_types}) and
all(n in nodes(lower) where n.type in {var_types})

// for now just give back the node where to "splice" in the tainting routine
return ID(var_top);
//return extract( n in nodes(lower) | n.type),extract( n in nodes(upper) | n.type) ;
