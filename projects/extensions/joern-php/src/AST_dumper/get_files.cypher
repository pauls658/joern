match 
p=(top_d:Filesystem)-[:DIRECTORY_OF*0..]->(f:Filesystem{type:"File"}),
(f)-[:FILE_OF]->(:AST{type:"AST_TOPLEVEL"})-[:PARENT_OF]->(t:AST{type: "AST_STMT_LIST"})

where 
ID(top_d) = 0

return 
reduce(path = ".", name in extract(n in nodes(p) | n.name) | path + "/" + name) as path, 
ID(t) as start_id;
