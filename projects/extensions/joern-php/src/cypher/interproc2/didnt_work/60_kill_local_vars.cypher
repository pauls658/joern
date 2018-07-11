load csv from "file:///home/brandon/php_apps/rewritten/extra_stuff/killable_local_vars.csv" as line 
match
(decl)-[:EXIT]->(exit)
where
ID(decl) = toInteger(line[0])
set
exit.defs = coalesce(exit.defs + ";" + line[1], line[1]);
