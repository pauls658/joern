load csv with headers from "file:///home/brandon/joern/projects/extensions/joern-php/src/datalog/cypher_datadeps.csv" as line

match
(def),
(use)
where
ID(def) = toInteger(line.def_stmt) and
ID(use) = toInteger(line.use_stmt)
create
(def)-[:REACHES{var:line.var}]->(use);
