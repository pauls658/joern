load csv with headers from "file:///home/brandon/joern/projects/extensions/joern-php/src/cypher/instrument/bbs.csv" as line

match

(art_ast)

where
ID(art_ast) = toInteger(line.pdoms)

return art_ast.call_id;
