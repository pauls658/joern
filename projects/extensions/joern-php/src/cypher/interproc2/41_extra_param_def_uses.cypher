// find function declarations with variable length arg lists
match
(call:FUNCCALL{type:"AST_CALL"})<-[:ASSOC]-(ret:ART_AST{type:"return"})-[:RET_DEF]->(bb),
(plist:AST{type:"AST_PARAM_LIST"}) 

where
call.name in ["func_get_arg", "func_get_args"] and
call.funcid = plist.funcid

set 
plist.make_extra_param = true
set
plist.max_params = 0
set
plist.extra_param_name = toString(plist.funcid) + "_extra_param"
set
bb.uses = coalesce(bb.uses + ";" + toString(plist.funcid) + "_extra_param", toString(plist.funcid) + "_extra_param");

// make the extra param
// UPDATE: dont actually need to do this
//match 
//(plist:AST{type:"AST_PARAM_LIST"}),
//(entry:Artificial{type:"CFG_FUNC_ENTRY"})-[old_r:FLOWS_TO]->(first) 
//
//where
//plist.funcid = entry.funcid
//and exists(plist.make_extra_param)
//
//create
//(extra_param:ART_AST{type:"extra_param", funcid:plist.funcid})<-[:EXTRA_PARAM]-(plist),
//(entry)-[:FLOWS_TO]->(extra_param)-[:FLOWS_TO]->(first)
//
//set
//old_r.delete = true;

// set max_params to the maximum number of params 
match
(plist:AST{type:"AST_PARAM_LIST"})-[:PARENT_OF]->(param)

where 
exists(plist.make_extra_param)

set 
plist.max_params = plist.max_params + 1;

// figure out which function calls are unambiguous
match
(a:FUNCCALL)-[:CALLS]->(decl) 

with a, count(decl) as num_call_edges 

where
num_call_edges = 1 

set a.unambiguous_call = true;

// figure out which args are extra
match
(arg:ART_AST{type:"arg_entry"})-[:CALL_ID]->(call{unambiguous_call:true})-[:CALLS]->(decl)-[:PARENT_OF]->(plist:AST{type:"AST_PARAM_LIST"})

where
exists(plist.max_params) and
arg.childnum >= plist.max_params

set arg.defs = coalesce(arg.defs + ";" + plist.extra_param_name, plist.extra_param_name);
