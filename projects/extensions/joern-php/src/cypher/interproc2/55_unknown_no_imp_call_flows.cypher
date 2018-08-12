// Remaining functions we assume everything flows to everything
// or functions where this is actually the case, namely:
// urlencode/urldecode
// quoted_printable_decode
// rawurlencode
// is_object
// defined
// serialize
// each
// usort
// sort

// array_merge
// array_flip
// reset
// key/next/prev

// call_user_func/call_user_func_array

// strtolower/strtoupper
// min/max
// str_pad
// array_shift/array_pop
// ini_get
// srand/rand
// bin2hex
load csv from "file:///home/brandon/joern/projects/extensions/joern-php/src/cypher/interproc2/prev.csv" as line
match (a)
where 
ID(a) = toInteger(line[0])
set a.done = true;

match 
(aentry:ART_AST{type:"arg_entry"})-[:CALL_ID]->(call:FUNCCALL)<-[:CALL_ID|ASSOC]-(aexit:ART_AST)
where 
not exists(call.done) and
not (call)-[:CALLS]->() and
aexit.type in ["arg_exit", "return"]
and aentry.call_id = aexit.call_id
create 
(aentry)-[:PROPOGATE]->(aexit); 
