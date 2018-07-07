// arg entries first
load csv with headers from "file:///home/brandon/joern/projects/extensions/joern-php/arg_def_uses.csv" as line
match
(a)<-[:ASSOC]-(arg{type:"arg_entry"})
where
ID(a) = toInteger(line.id)
set
arg.uses = line.symbols;


// arg exits next
load csv with headers from "file:///home/brandon/joern/projects/extensions/joern-php/arg_def_uses.csv" as line
match
(a)<-[:ASSOC]-(arg{type:"arg_exit"})
where
ID(a) = toInteger(line.id)
set
arg.defs = line.symbols;


// general basic block def/uses
load csv with headers from "file:///home/brandon/joern/projects/extensions/joern-php/BB_def_uses.csv" as line
match
(a)
where
ID(a) = toInteger(line.id)
set
a.defs = line.defs
set
a.uses = line.uses;


// TODO: can just delete all the def/uses from params
match (a:AST{type:"AST_PARAM"}) set a.uses = a.defs;
