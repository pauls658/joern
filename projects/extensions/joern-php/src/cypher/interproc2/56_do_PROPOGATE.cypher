// delete duplicates
match (aentry)-[r:PROPOGATE]->(aexit) with aentry, aexit, collect(r) as rels foreach(r in tail(rels) | delete r);

// handle arg exits and returns seperately
match (aentry)-[:PROPOGATE]->(aexit) where exists(aentry.uses) set aexit.uses = coalesce(aexit.uses + ";" + aentry.uses, aentry.uses);

// Now we define an actual return variable and have the basic block use that
//match (aentry)-[:PROPOGATE]->(aexit{type:"return"})-[:RET_DEF]->(def) where exists(aentry.uses) set def.uses = coalesce(def.uses + ";" + aentry.uses, aentry.uses);
