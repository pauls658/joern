// Queries to be run on the finished ICFG to remove uneccessary def/uses. Later on, we remove nodes without any def/uses.

// First make sure everything part of the ICFG is labeled
match (a)-[:FLOWS_TO|INTERPROC]-() set a:BB;

// Delete def/use sets that are equal because they don't add any info to the taint analysis
//match (a) 
//where exists(a.defs) and exists(a.uses) and 
//a.defs = a.uses 
//remove a.defs remove a.uses;
match (a{type:"AST_PARAM"})
remove a.defs remove a.uses;
match (a{type:"arg_exit"})
where "PARAM_VAL" in a.flags
remove a.defs remove a.uses;
