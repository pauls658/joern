match

(this:AST{type: "AST_PARAM", childnum: -1}),
(class:AST{type: "AST_CLASS"})

where
this.classname = class.name

create 
(class)-[:REACHES{var:"this"}]->(this);
