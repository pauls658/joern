load csv with headers from "file:///home/brandon/joern/projects/extensions/joern-php/call_order.csv" as line

match
(a),(b)

where
ID(a) = toInteger(line.id) and
ID(b) = toInteger(line.prev_id)

set
a.first_call = toBoolean(line.first_call)
set
a.last_call = toBoolean(line.last_call)
create
(a)<-[:NEXT_CALL]-(b);

match (a)-[r:NEXT_CALL]->(a)  delete r;
