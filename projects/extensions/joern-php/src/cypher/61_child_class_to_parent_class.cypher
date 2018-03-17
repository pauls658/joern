match

// the extended class' name always appears in the 0th child of the AST_CLASS
// there can only be one name since classes can only extend one class
(childclass:AST{type: "AST_CLASS"})-[:PARENT_OF]->(:AST{type: "AST_NAME", childnum: 0})-[:PARENT_OF]->(name:AST{type: "string"}), 
(parentclass:AST{type: "AST_CLASS"})

where 
name.code = parentclass.name 

create 
(childclass)-[:REACHES{var: "this"}]->(parentclass);
