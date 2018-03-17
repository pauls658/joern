load csv with headers from "file:///home/brandon/joern/projects/extensions/joern-php/new_props.csv" as line match (a) where ID(a) = toInteger(line.id) set a.globalDef = line.globalName;
