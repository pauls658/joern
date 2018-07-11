load csv with headers from "file:///home/brandon/joern/projects/extensions/joern-php/src/scripts/interproc2/csv/rewrite_params.csv" as line

match (decl)-[:PARENT_OF]->(stmts:AST{type:"AST_STMT_LIST"})-[:PARENT_OF*1..]->(v:AST{type:"AST_VAR"})-[:PARENT_OF]->(name:AST{type:"string"})

where
ID(decl) = toInteger(line.func_id) and
(line.globals is NULL or not name.code in split(line.globals, ";")) and
not name.code in ["GLOBALS", "_SERVER", "_GET", "_POST", "_FILES", "_COOKIE", "_SESSION", "_REQUEST", "_ENV"]

set name.new_code = line.prefix + name.code

return
ID(decl) as bollocks1,
line.prefix + name.code as bollocks2;

load csv with headers from "file:///home/brandon/joern/projects/extensions/joern-php/src/scripts/interproc2/csv/rewrite_params.csv" as line

match
(decl)-[:PARENT_OF]->(:AST{type:"AST_PARAM_LIST"})-[:PARENT_OF]->(p:AST{type:"AST_PARAM"})-[:PARENT_OF]->(name:AST{childnum:1})

where
ID(decl) = toInteger(line.func_id)

set name.new_code = line.prefix + name.code

RETURN
ID(decl) as bollocks1,
CASE
WHEN "PARAM_REF" in p.flags
THEN "bollocks"
ELSE line.prefix + name.code END AS bollocks2;

match (a) where exists(a.new_code) set a.code = a.new_code;
