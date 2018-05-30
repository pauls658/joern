match

(aarg:ART_AST{type: "arg_entry"}),
(def)-[r:REACHES]->(use)

where
ID(use) = aarg.bb_id and
r.var in aarg.argname

create
(aarg)<-[:REACHES{var:r.var}]-(def)

set
r.delete = true;

match ()-[r]-() where exists(r.delete) delete r;
