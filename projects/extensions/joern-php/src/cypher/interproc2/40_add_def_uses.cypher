// arg entries first
load csv with headers from "file:///home/brandon/joern/projects/extensions/joern-php/defuse_csv/arg_symbols.csv" as line
match
(a)<-[:ASSOC]-(arg{type:"arg_entry"})
where
ID(a) = toInteger(line.id)
set
arg.uses = coalesce(arg.uses + ";" + line.symbol, line.symbol);


// arg exits next
load csv with headers from "file:///home/brandon/joern/projects/extensions/joern-php/defuse_csv/arg_symbols.csv" as line
match
(a)<-[:ASSOC]-(arg{type:"arg_exit"})
where
ID(a) = toInteger(line.id)
set
arg.defs = coalesce(arg.defs + ";" + line.symbol, line.symbol);


// general basic block defs
load csv with headers from "file:///home/brandon/joern/projects/extensions/joern-php/defuse_csv/BB_def.csv" as line
match
(a)
where
ID(a) = toInteger(line.id)
set
a.defs = coalesce(a.defs + ";" + line.symbol, line.symbol);

// general basic block uses
load csv with headers from "file:///home/brandon/joern/projects/extensions/joern-php/defuse_csv/BB_use.csv" as line
match
(a)
where
ID(a) = toInteger(line.id)
set
a.uses = coalesce(a.uses + ";" + line.symbol, line.symbol);

// this is probably unecessary
match (a:AST{type:"AST_PARAM"}) set a.uses = a.defs;
