// Queries to be run on the finished ICFG to remove uneccessary nodes

// First make sure everything part of the ICFG is labeled
match (a)-[:FLOWS_TO|INTERPROC]-() set a:BB;

// Delete def/use sets that are equal because they don't add any info to the taint analysis
match (a) 
where exists(a.defs) and exists(a.uses) and 
a.defs = a.uses 
remove a.defs remove a.uses;

// Remove nodes that don't have any def/use info
// Remove intra-procedural nodes (connected only by FLOWS_TO relation)


// Remove inter-procedural nodes (connected by INTERPROC relation)
