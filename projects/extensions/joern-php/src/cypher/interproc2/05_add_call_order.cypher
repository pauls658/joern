load csv with headers from "file:///home/brandon/joern/projects/extensions/joern-php/call_order.csv" as line

match
(a)

where ID(a) = toInteger(line.id)

set
a.prev_call_id = toInteger(line.prev_id)
set
a.first_call = toBoolean(line.first_call)
set
a.last_call = toBoolean(line.last_call);
