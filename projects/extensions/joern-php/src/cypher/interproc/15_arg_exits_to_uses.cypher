match

(aarg:ART_AST{type: "arg_exit"}),
(def:BB)-[r:REACHES]->(use)

where
ID(def) = aarg.bb_id and
r.var = aarg.argname

create
(aarg)-[:REACHES{var:aarg.argname}]->(use);
// We can't assume we can delete this
// We may assign to a param of a function
// e.g. $var = func($var);
//delete
//r;
