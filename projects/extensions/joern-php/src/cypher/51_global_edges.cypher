match (a:AST),(b:AST{type:"AST_GLOBAL"}) where exists(a.globalDef) and a.globalDef = b.globalName create (a)-[:REACHES{var:a.globalDef}]->(b);
