load csv with headers from "file:///home/brandon/joern/projects/extensions/joern-php/src/cypher/instrument/assign_vars.csv" as line

match

(left),
(right),
(stmt)

where
ID(left) = toInteger(line.leftid) and
ID(right) = toInteger(line.rightid) and
ID(stmt) = toInteger(line.stmtid)

create
(stmt)-[:PARENT_OF{child_rel:toFloat(line.childid)}]->(assign:AST{type:"AST_ASSIGN",lineno:0}),
(assign)-[:PARENT_OF{child_rel:"var"}]->(left),
(assign)-[:PARENT_OF{child_rel:"expr"}]->(right)

return ID(right) as id;
