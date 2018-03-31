match

(aarg:ART_AST{type: "arg_entry"}),
(def)-[r:REACHES]->(use)

where
ID(use) = aarg.bb_id and
r.var = aarg.argname

create
(aarg)<-[:REACHES{var:aarg.argname}]-(def)

delete
r;
