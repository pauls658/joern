load csv with headers from "file:///home/brandon/joern/projects/extensions/joern-php/new_props.csv" as line 

match 
p=(a)-[:PARENT_OF*..]->(name:AST{type:"string"})

where 
ID(a) = toInteger(line.id) and
name.code = line.globalName and
none(n in nodes(p) where n:FUNCCALL)

set 
a.globalDef = line.globalName;
