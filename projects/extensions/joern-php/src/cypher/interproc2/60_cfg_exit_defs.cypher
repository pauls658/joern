match
(a:FUNCDECL)-[:EXIT]-(e)
where
exists(a.defs)
set
e.defs = a.defs;
