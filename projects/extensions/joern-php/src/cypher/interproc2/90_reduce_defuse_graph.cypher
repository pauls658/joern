// Queries to be run on the finished ICFG to remove uneccessary def/uses. Later on, we remove nodes without any def/uses.

// First make sure everything part of the ICFG is labeled
match (a)-[:FLOWS_TO|INTERPROC]-() set a:BB;

// Delete def/use sets that are equal because they don't add any info to the taint analysis
match (a:ART_AST) 
where exists(a.defs) and exists(a.uses) and 
a.defs = a.uses 
remove a.defs remove a.uses;
match (a{type:"AST_PARAM"})
remove a.defs remove a.uses;
match (a{type:"arg_exit"})
where "PARAM_VAL" in a.flags
remove a.defs remove a.uses;


// label BB's that have concats
match (a:BB)-[:PARENT_OF*..]->(c:AST{type:"AST_BINARY_OP"}) where "BINARY_CONCAT" in c.flags set a.has_concat = true;
match (a:BB) where not exists(a.has_concat) set a.has_concat = false;
