match 

(in)-[inE:PARENT_OF]->(a:AST{type: "AST_TOPLEVEL"})-[outE:PARENT_OF]->(out) 

delete
inE, outE 

create 
(in)-[:PARENT_OF{child_rel:"stmts"}]->(out);
