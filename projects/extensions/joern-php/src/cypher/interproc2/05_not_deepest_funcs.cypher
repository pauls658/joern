match 

p=(top:FUNCCALL)-[:PARENT_OF]->(alist:AST{type:"AST_ARG_LIST"})-[:PARENT_OF]->(arg:AST)-[:PARENT_OF*0..]->(call:FUNCCALL)

where 
single(n in tail(nodes(p)) where n:FUNCCALL) // make sure we only deal with one level of nested call

set
top.not_deepest = true;
