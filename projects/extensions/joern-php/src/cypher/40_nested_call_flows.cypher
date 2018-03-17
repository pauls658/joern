match 

p=(top:FUNCCALL)-[:PARENT_OF]->(alist:AST{type:"AST_ARG_LIST"})-[:PARENT_OF]->(arg:AST)-[:PARENT_OF*0..]->(call:FUNCCALL),

(call)<-[:ASSOC]-(ret:ART_AST{type: "return"}), // return of nested call
(arg)<-[:ASSOC]-(aentry:ART_AST{type: "arg_entry"}) // artificial arg of top call

where 
arg.childnum = aentry.childnum and // get the entry that corresponds to the arg num of the nested call
single(n in tail(nodes(p)) where n:FUNCCALL) // make sure we only deal with one level of nested call

create 
(ret)-[:REACHES]->(aentry);
