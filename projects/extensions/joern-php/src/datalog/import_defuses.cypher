load csv with headers from "file:///home/brandon/joern/projects/extensions/joern-php/src/datalog/cypher_defuses.csv" as line

match
(stmt)
where ID(stmt) = toInteger(line.id)
set stmt.defs2 = coalesce(line.defs, "")
set stmt.uses2 = coalesce(line.uses, "");
