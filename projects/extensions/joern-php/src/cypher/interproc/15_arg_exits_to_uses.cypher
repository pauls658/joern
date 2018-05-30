match

(aarg:ART_AST{type: "arg_exit"}),
(def:BB)-[r:REACHES]->(use)

where
ID(def) = aarg.bb_id and
r.var in aarg.argname

create
(aarg)-[:REACHES{var:r.var}]->(use)
// We can't assume we can delete this
// We may assign to a param of a function
// e.g. $var = func($var);
// update: this shouldn't matter actually assuming we create the correct reaching edges
set
r.delete = true;

match ()-[r]-() where exists(r.delete) delete r;
