match 

(a:AST),
(b:AST{type:"AST_GLOBAL"})

where 
exists(a.globalDef) and
a.globalDef = b.globalName 

create 
(a)-[:INTERPROC{var:a.globalDef, type: "global"}]->(b);
