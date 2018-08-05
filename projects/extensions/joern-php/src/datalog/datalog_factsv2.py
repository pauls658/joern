import json, sys
import networkx as nx
from collections import defaultdict

def graph_from_json():
    g = nx.MultiDiGraph()

    rows = json.load(open("tmp/nodes.json", "rb"))["results"][0]["data"]
    for row in rows:
        row = row["row"][0]
        i = int(row["id"])
        g.add_node(i, **row) 

    rows = json.load(open("tmp/flows_to.json", "rb"))["results"][0]["data"]
    for row in rows:
        row = row["row"]
        g.add_edge(int(row[0]), int(row[1]), label="FLOWS_TO")

    rows = json.load(open("tmp/interproc.json", "rb"))["results"][0]["data"]
    for row in rows:
        row = row["row"]
        rel_props = row[2]
        rel_props["label"] = "INTERPROC"
        g.add_edge(int(row[0]), int(row[1]), **rel_props)

    return g

# not really used
# dumps out the handleable info for translated node ids and
# avoids overwriting previous analysis results
def load_id_map():
    global id_map
    fd = open("tmp/id_map.csv", "r")
    for line in fd:
        new, orig = map(int, line.split(","))
        id_map[new] = orig

def dump_handleable():
    global g
    g = graph_from_json()
    load_id_map()
    write_handleable_info()

def only_FLOWSTO_edges(g, n):
    return g.out_degree(nbunch=n) > 0 and \
            g.in_degree(nbunch=n) > 0 and \
            all([d["label"] == "FLOWS_TO" for u, v, d in g.in_edges(nbunch=n, data=True)]) and \
            all([d["label"] == "FLOWS_TO" for u, v, d in g.out_edges(nbunch=n, data=True)])

def remove_node(g, n, label):
    preds = map(lambda e: e[0], g.in_edges(nbunch=n))
    succs = map(lambda e: e[1], g.out_edges(nbunch=n))
    for p in preds:
        for s in succs:
            g.add_edge(p, s, label=label)
    g.remove_node(n)

def preprocesses_graph(g):
    orig_node_count = g.order()
    for n, d in g.nodes(data=True):
        if "uses" not in d and \
        "defs" not in d and \
        only_FLOWSTO_edges(g, n):
            remove_node(g, n, "FLOWS_TO")

    print "Reduced graph to %0.2f%% original size" % ((float(g.order())/float(orig_node_count))*100)

class Symbol(object):
    ARRAY_CONST_INDEX = 0
    ARRAY_VAR_INDEX = 1
    ARRAY_UNKNOWN_INDEX = 2

    unknown_index = "cid_unknown"

    var_ids = {}
    var_idc = 0

    # enc: the encoded symbol
    def __init__(self, enc):
        self.enc = enc
        self.star = False
        if enc.endswith("*"):
            self.star = True
            enc = enc[:-1]

        # handle array stuff
        pieces = enc.split("[")
        self.name = pieces[0]
        self.isArray = False
        if len(pieces) > 1:
            # we have an array
            assert len(pieces) == 2
            self.isArray = True
            if pieces[1].startswith("$"):
                self.index = pieces[1][1:]
                self.arrayType = Symbol.ARRAY_VAR_INDEX
            elif pieces[1] == Symbol.unknown_index:
                self.index = None
                self.arrayType = Symbol.ARRAY_UNKNOWN_INDEX
            else:
                self.index = pieces[1]
                self.arrayType = Symbol.ARRAY_CONST_INDEX

    def __hash__(self):
        return hash(self.enc)

    def __eq__(self, other):
        return self.enc == other.enc

    def convert_var_index(self):
        assert self.isArray and self.arrayType == Symbol.ARRAY_VAR_INDEX
        self.enc = self.name + "[" + Symbol.unknown_index
        self.index = Symbol.unknown_index
        self.arrayType = Symbol.ARRAY_UNKNOWN_INDEX

    def is_unknown_index(self):
        return self.isArray and \
                (self.arrayType == Symbol.ARRAY_UNKNOWN_INDEX or \
                self.arrayType == Symbol.ARRAY_VAR_INDEX)

    def get_unknown_array_enc(self):
        assert self.is_unknown_index()
        return self.name + "[?"

    def get_symbol_names(self):
        ret = [self.name]
        if self.isArray and self.arrayType == Symbol.ARRAY_VAR_INDEX:
            ret.append(self.index)
        return ret

    def final_enc(self):
        if self.enc.endswith("*"):
            var_enc = self.enc[:-1]
        else:
            var_enc = self.enc
        if var_enc in Symbol.var_ids:
            return Symbol.var_ids[var_enc]
        else:
            Symbol.var_ids[var_enc] = Symbol.var_idc
            Symbol.var_idc += 1
            return Symbol.var_ids[var_enc]

# id -> set([Symbol])
stmt_defs = defaultdict(set)
stmt_uses = defaultdict(set)
var_map = {}
def load_def_use_info():
    global stmt_defs
    global stmt_uses
    global var_map
    distinct_vars = set()
    # defs/uses for stmts by stmt_id
    rows = json.load(open("tmp/def_use.json", "rb"))["results"][0]["data"]
    for row in rows:
        row = row["row"] # id, defs, uses
        stmt_id = int(row[0])
        if row[1] is not None:
            defs = set(map(lambda x: Symbol(x), filter(lambda x: x, row[1].split(";"))))
            stmt_defs[stmt_id] = defs
        if row[2] is not None:
            uses = set(map(lambda x: Symbol(x), filter(lambda x: x, row[2].split(";"))))
            stmt_uses[stmt_id] = uses

indexes = {}
def make_graph_indexes(g):
    indexes["symbol_uses"] = defaultdict(set)
    for n, data in g.nodes(data=True):
        for sym in data.get("uses", []):
            indexes["symbol_uses"][sym.enc].add(n)

"""
The keys of array_indexes are symbols that are indexed (implying array-hood)
somewhere in the program. Also removes def of whole array symbol, and replaces it
with defining the unknown index of the array.
"""
def handle_whole_arrays(udg, array_indexes):
    global indexes

    # Array symbols that we need to check for shit
    work = array_indexes.keys()
    # Symbols that we analyzed
    analyzed_syms = set()
    # Stmts that we have added defs for
    modified_stmts = set()
    
    new_syms = []
    remove_defs = set()
    sym_obj = Symbol("dummy")
    while work:
        sym = work.pop()
        if sym in analyzed_syms: continue
        analyzed_syms.add(sym)

        sym_obj.__init__(sym) # lol
        use_stmts = indexes["symbol_uses"].get(sym, [])
        for n in use_stmts:
            if not sym_obj.isArray:
                # use the known indexes and the special unknown index
                new_syms.append((n, sym + "[" + Symbol.unknown_index, "uses"))
                for i in array_indexes.get(sym, []):
                    new_syms.append((n, sym + "[" + i, "uses"))
            # else: we don't track beyond 1 dimension

            # we have already add extra defs for each def at this
            # statement
            if n in modified_stmts: continue
            modified_stmts.add(n)
            remove_defs.clear()
            for d in udg.nodes[n].get("defs", []):
                if not d.isArray:
                    # add a def of unknown index. Also remove the def of
                    # the whole symbol because, in the next step, we
                    # add a use of the unknown index for every use of
                    # the whole symbol. This cuts down on complexity of
                    # reaching definitions.
                    new_syms.append((n, d.name + "[" + Symbol.unknown_index, "defs"))
                    remove_defs.add(d)
                    work.append(d.name)
                elif d.arrayType == Symbol.ARRAY_CONST_INDEX:
                    work.append(d.name + "[" + d.index)
                    work.append(d.name + "[" + Symbol.unknown_index)
                else:
                    assert d.arrayType == Symbol.ARRAY_UNKNOWN_INDEX
                    work.append(d.name + "[" + Symbol.unknown_index)
                    for i in array_indexes.get(d.name, []):
                        work.append(d.name + "[" + Symbol.unknown_index)
            for d in remove_defs:
                udg.nodes[n]["defs"].remove(d)

    # make sure defintions of array symbols are converted to define their
    # unknown index.
    new_defs = set()
    for n, data in udg.nodes(data=True):
        new_defs.clear()
        for d in data.get("defs", []):
            if not d.isArray and d.name in array_indexes:
                new_defs.add(Symbol(d.name + "[" + Symbol.unknown_index))
            else:
                new_defs.add(d)
        if new_defs:
            data["defs"].clear()
            data["defs"].update(new_defs)
    return new_syms

"""
Adds extra uses for known/unknown array indexes.
    If the index is known, adds a use of the unknown index.
    If the index is unknown, adds uses of all known indexes.
"""
def handle_array_indexes(udg, array_indexes):
    new_uses = []

    for n, data in udg.nodes(data=True):
        for u in data.get("uses", []):
            if u.isArray:
                if u.arrayType == Symbol.ARRAY_CONST_INDEX:
                    new_uses.append((n, u.name + "[" + Symbol.unknown_index, "uses"))
                else:
                    assert u.arrayType == Symbol.ARRAY_UNKNOWN_INDEX
                    for i in array_indexes.get(u.name, []):
                        new_uses.append((n, u.name + "[" + i, "uses"))

    return new_uses

"""
uses is a list for triples, first element is a int node index, and
the second is a String symbol, and the third element is a String "defs" or "uses".
"""
def add_syms_to_udg(udg, syms):
    for n, sym, uod in syms:
        data = udg.nodes[n]
        if uod not in data:
            data[uod] = set()
        data[uod].add(Symbol(sym))

sinks = set()
safe_sinks = set()
tainted_sinks = set()
def load_sinks():
    global sinks
    sinks.update(json.load(open("tmp/sinks.json", "rb"))["results"][0]["data"][0]["row"][0])

def load_testcase_sinks():
    global sinks, safe_sinks, tainted_sinks
    safe_sinks.update(json.load(open("tmp/safe_sinks.json", "rb"))["results"][0]["data"][0]["row"][0])
    tainted_sinks.update(json.load(open("tmp/tainted_sinks.json", "rb"))["results"][0]["data"][0]["row"][0])
    sinks.update(safe_sinks)
    sinks.update(tainted_sinks)

sources = set()
def load_sources():
    global sources
    sources.update(json.load(open("tmp/sources.json", "rb"))["results"][0]["data"][0]["row"][0])

def profile_memory():
    from memory_profiler import memory_usage
    mem_usage = memory_usage(graph_from_json)
    print('Maximum memory usage: %s' % max(mem_usage))

def add_graph_edge(start, end, copied_cfg):
    copied_cfg.add_edge(start, end)

def write_datalog_edge(start, end):
    global datalog_edge_fd
    datalog_edge_fd.write("%d\t%d\n" % (start, end))

def write_datalog_def(stmt_id, sym):
    global datalog_def_fd
    datalog_def_fd.write("%d\t%s\n" % (stmt_id, sym.final_enc()))
    if sym.star or sym.is_unknown_index():
        write_datalog_star(stmt_id, sym)

def write_datalog_star(stmt_id, sym):
    global datalog_star_fd
    datalog_star_fd.write("%d\t%s\n" % (stmt_id, sym.final_enc()))

def write_datalog_use(stmt_id, sym):
    global datalog_use_fd
    datalog_use_fd.write("%d\t%s\n" % (stmt_id, sym.final_enc()))

def write_datalog_sink(i):
    global out_format
    if out_format == "smt2":
        global datalog_fd
        datalog_fd.write("(rule (sink #x%08x))\n" % (i))
    elif out_format == "souffle":
        global datalog_sink_fd
        datalog_sink_fd.write("%d\n" % (i))

def write_datalog_tainted_sink(i):
    global out_format
    if out_format == "smt2":
        global datalog_fd
        datalog_fd.write("(rule (gold_tainted_sink #x%08x))\n" % (i))
    elif out_format == "souffle":
        global datalog_tainted_sink_fd
        datalog_tainted_sink_fd.write("%d\n" % (i))

def write_datalog_safe_sink(i):
    global out_format
    if out_format == "smt2":
        global datalog_fd
        datalog_fd.write("(rule (safe_sink #x%08x))\n" % (i))
    elif out_format == "souffle":
        global datalog_safe_sink_fd
        datalog_safe_sink_fd.write("%d\n" % (i))

def write_datalog_source(i):
    global out_format
    if out_format == "smt2":
        global datalog_fd
        datalog_fd.write("(rule (source #x%08x))\n" % (i))
    elif out_format == "souffle":
        global datalog_source_fd
        datalog_source_fd.write("%d\n" % (i))

def write_datalog_nonkilling_stmt(stmt_id, copied_cfg):
    global datalog_nokill_fd
    datalog_nokill_fd.write("%d\n" % (stmt_id))

def add_def_use_to_graph(copied_cfg):
    global idc, id_map
    global stmt_defs, stmt_uses

    for i in range(0, idc): # idc itself has not been used yet
        orig_id = id_map[i]
        for d in stmt_defs.get(orig_id, []):
            if "defs" not in copied_cfg.nodes[i]:
                copied_cfg.nodes[i]["defs"] = set()
            copied_cfg.nodes[i]["defs"].add(Symbol(d.enc))
        for u in stmt_uses.get(orig_id, []):
            if "uses" not in copied_cfg.nodes[i]:
                copied_cfg.nodes[i]["uses"] = set()
            copied_cfg.nodes[i]["uses"].add(Symbol(u.enc))

def write_datalog_nonkilling_args(g, call_id, id_translation, copied_cfg):
    for n, _ in filter(
            lambda (_, d): 
            d["type"] == "arg_entry" and
            d["call_id"] == call_id,
            g.nodes(data=True)):
        write_datalog_nonkilling_stmt(id_translation[n], copied_cfg)

def get_argentries_for_callid(g, call_id):
    return [n for n, _ in filter(
            lambda (_, d): 
            d["type"] == "arg_entry" and
            d["call_id"] == call_id,
            g.nodes(data=True))]

func_depth = {}
def calc_func_depths(g, func_entry, cur_depth=0):
    global func_depth

    if func_entry in func_depth:
        return (func_depth[func_entry] - 1) + cur_depth

    func_exit = int(g.nodes[func_entry]["exit_id"])
    leaf_call = True
    max_call_depth = 0

    visited = set([func_exit]) # un-translated ids of nodes who's successors we have processed
    work = [func_entry] # added to visited on first iter
    while work:
        cur = work.pop()
        if cur in visited: continue
        visited.add(cur)
        for s in g.adj[cur]:

            e = g.get_edge_data(cur, s)[0]
            if e["label"] == "INTERPROC":
                arg_exit = e["exit_id"]

                assert e["type"] == "entry" and \
                        arg_exit not in visited
               
                # the only way to the arg_exit is through the function,
                # regardless if this is a recursive call or not
                work.append(arg_exit)

                if s in visited:
                    # recursion
                    pass
                else:
                    # regular function call
                    call_depth = calc_func_depths(g, s, cur_depth + 1)
                    max_call_depth = max(max_call_depth, call_depth)
                    leaf_call = False

            else:
                if s not in visited:
                    # haven't processed this node's successors
                    work.append(s)
    if leaf_call:
        max_call_depth = cur_depth
   
    func_depth[func_entry] = (max_call_depth - cur_depth) + 1

    return max_call_depth


# func entry -> (entry, exit)
last_created = {}
def make_copied_cfg(g, func_entry, max_depth, copied_cfg):
    global idc, last_created, id_map, func_depth

    if func_depth[func_entry] >= max_depth and func_entry in last_created:
        # no copy
        return last_created[func_entry]

    # copy

    # orig id -> new id
    id_translation = {func_entry : idc}
    id_map[idc] = func_entry
    idc += 1
    
    func_exit = int(g.nodes[func_entry]["exit_id"])
    id_translation[func_exit] = idc
    id_map[idc] = func_exit
    idc += 1
    
    last_created[func_entry] = (id_translation[func_entry], id_translation[func_exit])

    work = [func_entry]
    while work:
        cur = work.pop()
        for s in g.adj[cur]:
            e = g.get_edge_data(cur, s)[0]

            if e["label"] == "INTERPROC":
                arg_exit = e["exit_id"]

                assert e["type"] == "entry" and \
                        arg_exit not in id_translation

                id_translation[arg_exit] = idc
                id_map[idc] = arg_exit
                idc += 1

                if s in id_translation: 
                    # recursion
                    assert s == func_entry
                    add_graph_edge(id_translation[cur], id_translation[s], copied_cfg)
                    add_graph_edge(id_translation[func_exit], id_translation[arg_exit], copied_cfg)
                    # don't kill the param definitions of the parent
                    # call
                    write_datalog_nonkilling_args(g, g.nodes[cur]["call_id"], id_translation, copied_cfg)
                else:
                    # regular function call
                    call_entry, call_exit = make_copied_cfg(g, s, max_depth, copied_cfg)
                    add_graph_edge(id_translation[cur], call_entry, copied_cfg)
                    add_graph_edge(call_exit, id_translation[arg_exit], copied_cfg)

                # the only way to the arg exit is through the function,
                # so in either case we want to add it
                work.append(arg_exit)

            elif s not in id_translation:
                # we haven't visited s yet, add it to work
                id_translation[s] = idc
                id_map[idc] = s
                idc += 1
                work.append(s)
                add_graph_edge(id_translation[cur], id_translation[s], copied_cfg)

            else:
                # we have visited s, or it is the func_entry/exit, in
                # either case, do not continue
                add_graph_edge(id_translation[cur], id_translation[s], copied_cfg)

    return id_translation[func_entry], id_translation[func_exit]

def write_datalog_sinks_and_sources():
    global sinks, tainted_sinks, safe_sinks
    global sources
    global idc
    global id_map
    for i in range(0, idc):
        if id_map[i] in sinks:
            write_datalog_sink(i)
            if id_map[i] in tainted_sinks:
                write_datalog_tainted_sink(i)
            elif id_map[i] in safe_sinks:
                write_datalog_safe_sink(i)
        elif id_map[i] in sources:
            write_datalog_source(i)

def save_maps():
    global idc
    global id_map
    out_fd = open("tmp/id_map.csv", "w+")
    for i in range(0, idc):
        out_fd.write(str(i) + "," + str(id_map[i]) + "\n")
    out_fd.close()

    # for debugging
    global var_map
    out_fd = open("tmp/var_map.csv", "w+")
    for name, i in var_map.iteritems():
        out_fd.write("%d,%s\n" % (i, name))
    out_fd.close()

def sqmail_entries(g, file_name):
    return filter(
            lambda (_, x): "name" in x and \
                    x["name"].endswith(file_name) and \
                    x["type"] == "CFG_FUNC_ENTRY",
            g.nodes(data=True))

def get_all_toplevel_entrys(g):
    starts = filter(lambda (n, d): 
            d["type"] == "CFG_FUNC_ENTRY" and
            d["name"].endswith("right_main.php"),
            g.nodes(data=True))

    return map(lambda (n, d): n, starts)

def write_func_depths():
    global func_depth
    fd = open("tmp/func_depths", "w+")
    g = graph_from_json()
    entry = sqmail_entries(g)[0][0]
    calc_func_depths(g, entry)

    for eid, depth in func_depth.iteritems():
        fd.write("%s (%d): %d\n" % (g.nodes[eid]["name"], eid, depth))
    fd.close()


def udg_to_datalog(g):
    for n in g.nodes():
        for s in g.adj[n]:
            write_datalog_edge(n, s)
        for d in g.nodes[n].get("defs", []):
            write_datalog_def(n, d)
        for u in g.nodes[n].get("uses", []):
            write_datalog_use(n, u)

idc = 0
# copied id -> orig id
id_map = {}
def open_output_files():
	global datalog_fd
	global datalog_edge_fd
	global datalog_def_fd
	global datalog_use_fd
	global datalog_source_fd
	global datalog_sink_fd
	global datalog_safe_sink_fd
	global datalog_tainted_sink_fd
	global datalog_star_fd
	global datalog_nokill_fd

	datalog_fd = open("tmp/facts", "w+")
	datalog_edge_fd = open("tmp/edge.csv", "w+")
	datalog_def_fd = open("tmp/def.csv", "w+")
	datalog_use_fd = open("tmp/use.csv", "w+")
	datalog_source_fd = open("tmp/source.csv", "w+")
	datalog_sink_fd = open("tmp/sink.csv", "w+")
	datalog_safe_sink_fd = open("tmp/safe_sink.csv", "w+")
	datalog_tainted_sink_fd = open("tmp/tainted_sink.csv", "w+")
	datalog_star_fd = open("tmp/star_def.csv", "w+")
	datalog_nokill_fd = open("tmp/nokill.csv", "w+")

g = None
copied_funcs = 0
unique_funcs = set()
out_format = "souffle"
def testcases():
    global datalog_fd, copied_funcs
    global g

    load_sinks()
    load_testcase_sinks()
    load_sources()
    load_def_use_info()
    g = graph_from_json()
    entries = get_all_toplevel_entrys(g)
    for entry in entries:
        write_bounded_copied_cfg(g, entry, 1)
    add_def_use_to_graph()
    write_datalog_sinks_and_sources()
    save_maps()

    datalog_fd.close()

def debug():
    global datalog_fd, copied_funcs
    global g

    load_sinks()
    load_testcase_sinks()
    load_sources()
    load_def_use_info()
    g = graph_from_json()
    entry = sqmail_entries(g)[0][0]
    depth = 6
    print "write_bounded_copied_cfg to depth %d" % (depth)
    write_bounded_copied_cfg(g, entry, depth)
    add_def_use_to_graph()
    write_datalog_sinks_and_sources()
    save_maps()

    datalog_fd.close()

def write_handleable_info():
    global id_map,g
    fd = open("tmp/handleable.csv", "w+")
    
    for new, orig in id_map.iteritems():
        fd.write("%d,%d\n" % (new, g.nodes[orig]["handleable"]))
    fd.close()

# Dear elves,
# I'm impressed with your ability to cause me days
# of frustration with such small changes to my code.
# I propose an alliance. In return, I grant you 
# bouncing privileges on my big blue/gray exercise ball.
# I eagerly await your response.
# Sincerely,
#   Brandon
def main(args):
    global datalog_fd, copied_funcs
    global g
    file_name = args[0]
    open_output_files()
    load_sinks()
    load_sources()
    load_def_use_info()
    g = graph_from_json()
    preprocesses_graph(g)
    entry = sqmail_entries(g, file_name)[0][0]
    calc_func_depths(g, entry)
    depth = 6
    print "write_bounded_copied_cfg to depth %d" % (depth)
    make_copied_cfg(g, entry, depth)
    with open("tmp/cfg_exit", "w+") as fd:
        fd.write(str(g.nodes[entry]["exit_id"]))
    add_def_use_to_graph()
    write_datalog_sinks_and_sources()

    save_maps()
    write_handleable_info()

    datalog_fd.close()

def collect_array_indexes(g):
    array_indexes = defaultdict(set)
    for n, data in g.nodes(data=True):
        for d in data.get("defs", []):
            if d.isArray:
                # accessing puts the name in the
                # dictionary
                i = array_indexes[d.name]
                if d.arrayType == Symbol.ARRAY_CONST_INDEX:
                    # only add constant indexes
                    # (unknown indexes are implied 
                    # if the name is in the dict)
                    i.add(d.index)
        for u in data.get("uses", []):
            if u.isArray:
                i = array_indexes[u.name]
                if u.arrayType == Symbol.ARRAY_CONST_INDEX:
                   i.add(u.index)
    return array_indexes

def resolve_array_indexes(udg):
    # TODO: resolve array indexes
    # Final step... convert any unresolved indexes to cid_unknown. If a
    # the variable in any index is removed and added as a use to the
    # statement.
    new_syms = set()
    for n, data in udg.nodes(data=True):
        new_syms.clear()
        for sym in data.get("defs", []):
            if sym.isArray and sym.arrayType == Symbol.ARRAY_VAR_INDEX:
                if "uses" not in data:
                    data["uses"] = set()
                data["uses"].add(Symbol(sym.index))
                sym.convert_var_index()
                new_syms.add(sym)
            else:
                new_syms.add(sym)
        if "defs" in data:
            data["defs"].clear()
            data["defs"].update(new_syms)

        new_syms.clear()
        for sym in data.get("uses", []):
            if sym.isArray and sym.arrayType == Symbol.ARRAY_VAR_INDEX:
                new_syms.add(Symbol(sym.index))
                sym.convert_var_index()
                new_syms.add(sym)
            else:
                new_syms.add(sym)
        if "uses" in data:
            data["uses"].clear()
            data["uses"].update(new_syms)

def arrays(args):
    global datalog_fd
    global g
    file_name = args[0]
    open_output_files()
    #load_sinks()
    load_testcase_sinks()
    load_sources()
    load_def_use_info()
    g = graph_from_json()
    #preprocesses_graph(g)
    entry = sqmail_entries(g, file_name)[0][0]
    calc_func_depths(g, entry)
    depth = 0
    print "write_bounded_copied_cfg to depth %d" % (depth)
    copied_cfg = nx.MultiDiGraph()
    make_copied_cfg(g, entry, depth, copied_cfg)
    with open("tmp/cfg_exit", "w+") as fd:
        fd.write(str(g.nodes[entry]["exit_id"]))
    add_def_use_to_graph(copied_cfg)
    array_indexes = collect_array_indexes(copied_cfg)
    resolve_array_indexes(copied_cfg)
    make_graph_indexes(copied_cfg)
    new_syms1 = handle_whole_arrays(copied_cfg, array_indexes)
    new_syms2 = handle_array_indexes(copied_cfg, array_indexes)
    add_syms_to_udg(copied_cfg, new_syms1)
    add_syms_to_udg(copied_cfg, new_syms2)
    udg_to_datalog(copied_cfg)
    write_datalog_sinks_and_sources()

    save_maps()
    #write_handleable_info()

    datalog_fd.close()

if __name__ == "__main__":
    if len(sys.argv) < 2:
        cmd = "<none>"
    else:
        cmd = sys.argv[1]
    cmds = {
            "main" : main,
            "func_depths" : write_func_depths,
            "debug" : debug,
            "handleable" : dump_handleable,
            "arrays" : arrays
    }
    if cmd not in cmds:
        print "Invalid command: %s" % (cmd)
        print "Valid commands:"
        print cmds.keys()
    else:
        if len(sys.argv) == 2:
            cmds[cmd]()
        else:
            cmds[cmd](sys.argv[2:])