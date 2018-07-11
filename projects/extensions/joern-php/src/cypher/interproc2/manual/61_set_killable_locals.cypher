load csv with headers from "file:///home/brandon/joern/projects/extensions/joern-php/src/cypher/interproc2/prev.csv" as line 

match
(exit)<-[:EXIT]-(decl)-[:PARENT_OF]->(stmts:AST{type:"AST_STMT_LIST"})-[:PARENT_OF*1..]->(v:AST{type:"AST_VAR"})-[:PARENT_OF]->(name:AST{type:"string"})

where
ID(decl) = toInteger(line.func_id) and 
(line.globals is NULL or not name.code in split(line.globals, ";")) and 
not name.code in ["GLOBALS", "_SERVER", "_GET", "_POST", "_FILES", "_COOKIE", "_SESSION", "_REQUEST", "_ENV"]

with exit, decl, reduce(s = "", n in collect(distinct name.code) | s + n + ";") as vars

set
exit.defs = vars;

match
(exit)<-[:EXIT]-(decl:FUNCDECL)-[:PARENT_OF]->({type:"AST_PARAM_LIST"})-[:PARENT_OF]->(p{type:"AST_PARAM"})-[:PARENT_OF]->(name{childnum:1})
where
(not exists(p.flags) or not "PARAM_REF" in p.flags)
with
exit, decl, reduce(s = "", n in collect(distinct name.code) | s + n + ";") as params
set
exit.defs = coalesce(exit.defs + params, params);
