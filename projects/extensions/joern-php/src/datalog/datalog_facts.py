import json
import networkx as nx

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
            changed = True
            remove_node(g, n, "FLOWS_TO")

    print "Reduced graph to %0.2f%% original size" % ((float(g.order())/float(orig_node_count))*100)

stmt_defs = {}
stmt_uses = {}
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
            defs = row[1].split(";")
            distinct_vars.update(defs)
            stmt_defs[stmt_id] = defs
        if row[2] is not None:
            uses = row[2].split(";")
            distinct_vars.update(uses)
            stmt_uses[stmt_id] = uses

    # symbol_map 
    var_id = 0
    for symbol in distinct_vars:
        var_map[symbol] = var_id
        var_id += 1

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

def write_datalog_edge(start, end):
    global out_format
    #global g
    #global id_map
    if out_format == "smt2":
        global datalog_fd
        datalog_fd.write("(rule (po #x%08x #x%08x))\n" % (start, end))
            #(start, end, g.nodes[id_map[start]]["type"], g.nodes[id_map[end]]["type"]))
    elif out_format == "souffle":
        global datalog_edge_fd
        datalog_edge_fd.write("%d\t%d\n" % (start, end))

def write_datalog_def(stmt_id, var_id):
    global out_format
    if out_format == "smt2":
        global datalog_fd
        datalog_fd.write("(rule (def #x%08x #x%08x))\n" % (stmt_id, var_id))
    elif out_format == "souffle":
        global datalog_def_fd
        datalog_def_fd.write("%d\t%d\n" % (stmt_id, var_id))

def write_datalog_use(stmt_id, var_id):
    global out_format
    if out_format == "smt2":
        global datalog_fd
        datalog_fd.write("(rule (use #x%08x #x%08x))\n" % (stmt_id, var_id))
    elif out_format == "souffle":
        global datalog_use_fd
        datalog_use_fd.write("%d\t%d\n" % (stmt_id, var_id))

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

def write_datalog_nonkilling_stmt(stmt_id):
    global out_format
    if out_format == "smt2":
        global datalog_fd
        raise Exception("not implemented")
    elif out_format == "souffle":
        global datalog_nokill_fd
        datalog_nokill_fd.write("%d\n" % (stmt_id))

def write_datalog_node_def_use():
    global idc
    global stmt_defs
    global stmt_uses
    global var_map
    global id_map
    for i in range(0, idc): # idc itself has not been used yet
        orig_id = id_map[i]
        for d in stmt_defs.get(orig_id, []):
            write_datalog_def(i, var_map[d])
        for u in stmt_uses.get(orig_id, []):
            write_datalog_use(i, var_map[u])

def write_datalog_nonkilling_args(g, call_id, id_translation):
    for n, _ in filter(
            lambda (_, d): 
            d["type"] == "arg_entry" and
            d["call_id"] == call_id,
            g.nodes(data=True)):
        write_datalog_nonkilling_stmt(id_translation[n])

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
def write_copied_cfg(g, func_entry, max_depth):
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
                    write_datalog_edge(id_translation[cur], id_translation[s])
                    write_datalog_edge(id_translation[func_exit], id_translation[arg_exit])
                    # don't kill the param definitions of the parent
                    # call
                    write_datalog_nonkilling_args(g, g.nodes[cur]["call_id"], id_translation)
                else:
                    # regular function call
                    call_entry, call_exit = write_copied_cfg(g, s, max_depth)
                    write_datalog_edge(id_translation[cur], call_entry)
                    write_datalog_edge(call_exit, id_translation[arg_exit])

                # the only way to the arg exit is through the function,
                # so in either case we want to add it
                work.append(arg_exit)

            elif s not in id_translation:
                # we haven't visited s yet, add it to work
                id_translation[s] = idc
                id_map[idc] = s
                idc += 1
                work.append(s)
                write_datalog_edge(id_translation[cur], id_translation[s])

            else:
                # we have visited s, or it is the func_entry/exit, in
                # either case, do not continue
                write_datalog_edge(id_translation[cur], id_translation[s])

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

def sqmail_entries(g):
    return filter(
            lambda (_, x): "name" in x and \
                    x["name"].endswith("read_body.php") and \
                    x["type"] == "CFG_FUNC_ENTRY",
            g.nodes(data=True))

def get_all_toplevel_entrys(g):
    starts = filter(lambda (n, d): 
            d["type"] == "CFG_FUNC_ENTRY" and
            d["name"].endswith(".php"),
            g.nodes(data=True))

    return map(lambda (n, d): n, starts)

def write_func_depths():
    global func_depth

    g = graph_from_json()
    entry = sqmail_entries(g)[0][0]
    calc_func_depths(g, entry)

    for eid, depth in func_depth.iteritems():
        print "%s (%d): %d" % (g.nodes[eid]["name"], eid, depth)

idc = 0
# copied id -> orig id
id_map = {}
datalog_fd = open("tmp/facts", "w+")
datalog_edge_fd = open("tmp/edge.csv", "w+")
datalog_def_fd = open("tmp/def.csv", "w+")
datalog_use_fd = open("tmp/use.csv", "w+")
datalog_source_fd = open("tmp/source.csv", "w+")
datalog_sink_fd = open("tmp/sink.csv", "w+")
datalog_safe_sink_fd = open("tmp/safe_sink.csv", "w+")
datalog_tainted_sink_fd = open("tmp/tainted_sink.csv", "w+")
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
    write_datalog_node_def_use()
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
    write_datalog_node_def_use()
    write_datalog_sinks_and_sources()
    save_maps()

    datalog_fd.close()

# Dear elves,
# I'm impressed with your ability to cause me days
# of frustration with such small changes to my code.
# I propose an alliance. In return, I grant you 
# bouncing privileges on my big blue/gray exercise ball.
# I eagerly await your response.
# Sincerely,
#   Brandon
def main():
    global datalog_fd, copied_funcs
    global g

    load_sinks()
    load_sources()
    load_def_use_info()
    g = graph_from_json()
    preprocesses_graph(g)
    entry = sqmail_entries(g)[0][0]
    calc_func_depths(g, entry)
    depth = 4
    print "write_bounded_copied_cfg to depth %d" % (depth)
    #write_bounded_copied_cfg(g, entry, depth)
    print entry
    write_copied_cfg(g, entry, depth)
    write_datalog_node_def_use()
    write_datalog_sinks_and_sources()

    save_maps()

    datalog_fd.close()

if __name__ == "__main__":
    #write_func_depths()
    main()
    #debug()
    #testcases()
