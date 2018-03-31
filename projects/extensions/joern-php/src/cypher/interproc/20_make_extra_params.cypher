match

p=(bb:BB)-[:PARENT_OF*0..]->(c:AST{type: "AST_CALL"})-[:PARENT_OF*2]->(name:AST{type:"string"}),
(plist:AST{type:"AST_PARAM_LIST"})

where
name.code in ["func_get_arg", "func_get_args"] and
single(n in nodes(p) where n:BB) and
bb.funcid = plist.funcid

with
collect(distinct c.funcid) as fids,
collect(distinct ID(bb)) as bbids,
plist as plist

foreach(id in fids| create (plist)-[:EXTRA_PARAM]->(:ART_AST{type:"extra_param",funcid:id})) 

with bbids as bbids 
unwind bbids as bbid return bbid;
