match (a:FUNCCALL)<-[:CALL_ID]-(b:ART_AST{type:"arg_entry",childnum:2}) where a.name in ["preg_match", "preg_match_all"] set b.defs = b.uses remove b.uses;
match (a:FUNCCALL)<-[:CALL_ID]-(b:ART_AST{type:"arg_entry",childnum:1}) where a.name in ["parse_str"] set b.defs = b.uses remove b.uses;
