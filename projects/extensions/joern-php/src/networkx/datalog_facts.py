import json
import networkx as nx

def graph_from_json():
    g = nx.MultiDiGraph()

    rows = json.load(open("nodes.json", "rb"))["results"][0]["data"]
    for row in rows:
        row = row["row"][0]
        i = int(row["id"])
        g.add_node(i, **row) 

    rows = json.load(open("flows_to.json", "rb"))["results"][0]["data"]
    for row in rows:
        row = row["row"]
        g.add_edge(int(row[0]), int(row[1]), label="FLOWS_TO")

    rows = json.load(open("interproc.json", "rb"))["results"][0]["data"]
    for row in rows:
        row = row["row"]
        rel_props = row[2]
        rel_props["label"] = "INTERPROC"
        g.add_edge(int(row[0]), int(row[1]), **rel_props)

    return g

stmt_defs = {}
stmt_uses = {}
var_map = {}
def store_load_info():
    global stmt_defs
    global stmt_uses
    global var_map
    distinct_vars = set()
    # defs/uses for stmts by stmt_id
    rows = json.load(open("store_load.json", "rb"))["results"][0]["data"]
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

echos = set()
def load_echos():
    global echos
    echos.update(json.load(open("echos.json", "rb"))["results"][0]["data"][0]["row"][0])

tainted = set()
def load_tainted():
    global tainted
    tainted.update(json.load(open("tainted.json", "rb"))["results"][0]["data"][0]["row"][0])

def profile_memory():
    from memory_profiler import memory_usage
    mem_usage = memory_usage(graph_from_json)
    print('Maximum memory usage: %s' % max(mem_usage))

def print_datalog_edge(start, end):
    global dl_facts_fd
    dl_facts_fd("(rule po(#x%d, #x%d))\n" % (start, end))

def print_datalog_write(stmt_id, var_id):
    global dl_facts_fd
    dl_facts_fd.write("(rule write(#x%d, #x%d))\n" % (stmt_id, var_id))

def print_datalog_read(stmt_id, var_id):
    global dl_facts_fd
    dl_facts_fd.write("(rule read(#x%d, #x%d))\n" % (stmt_id, var_id))

def print_datalog_variable(name, var_id):
    global dl_facts_fd
    dl_facts_fd.write("(rule variable(#x%d)) ;; %s\n" % (var_id, name))

def print_datalog_echo(i):
    global dl_facts_fd
    dl_facts_fd.write("(rule echo(#x%d))" % (i))

def print_datalog_tainted(i):
    global dl_facts_fd
    dl_facts_fd.write("(rule tainted(#x%d))" % (i))

def cfg_node_store_load():
    global idc
    global stmt_defs
    global stmt_uses
    global var_map
    global id_map
    for i in range(0, idc): # idc itself has not been used yet
        orig_id = id_map[i]
        for d in stmt_defs.get(orig_id, []):
            print_datalog_write(var_map[d], i)
        for u in stmt_uses.get(orig_id, []):
            print_datalog_read(var_map[d], i)

def copy_cfg(g, func_entry):
    global idc
    # orig id -> new id
    id_translation = {func_entry : idc}
    id_map[idc] = func_entry
    idc += 1

    func_exit = int(g.nodes[func_entry]["exit_id"])
    id_translation[func_exit] = idc
    id_map[idc] = func_exit
    idc += 1

    work = [func_entry]
    while work:
        cur = work.pop()
        for s in g.adj[cur]:
            if s == func_exit: continue

            e = g.get_edge_data(cur, s)[0]

            if e["label"] == "INTERPROC":
                arg_exit = e["exit_id"]

                #assert e["type"] == "entry" and \
                #        arg_exit not in id_translation
                if arg_exit in id_translation:
                    import pdb; pdb.set_trace()

                id_translation[arg_exit] = idc
                id_map[idc] = arg_exit
                idc += 1

                if s in id_translation: 
                    # recursion
                    assert s == func_entry
                    print_datalog_edge(id_translation[cur], id_translation[s])
                    print_datalog_edge(id_translation[func_exit], id_translation[arg_exit])
                else:
                    call_entry, call_exit = copy_cfg(g, s)
                    print_datalog_edge(id_translation[cur], call_entry)
                    print_datalog_edge(call_exit, id_translation[arg_exit])
                    work.append(arg_exit)

            elif s not in id_translation:
                id_translation[s] = idc
                id_map[idc] = s
                idc += 1
                work.append(s)
                print_datalog_edge(id_translation[cur], id_translation[s])

            else:
                print_datalog_edge(id_translation[cur], id_translation[s])

    return id_translation[func_entry], id_translation[func_exit]

def datalog_variables():
    global var_map
    for name, i in var_map.iteritems():
        print_datalog_variable(name, i)

def datalog_echos_and_tainted():
    global echos
    global tainted
    global idc
    global id_map
    for i in range(0, idc):
        if id_map[i] in echos:
            print_datalog_echo(i)
        elif id_map[i] in 

def save_maps():
    global idc
    global id_map
    out_fd = open("id_map.csv", "w+")
    for i in range(0, idc):
        out_fd.write(str(i) + "," + str(id_map[i]) + "\n")
    out_fd.close()

    # dont need this actually
    global var_map
    out_fd = open("var_map.csv", "w+")
    for name, i in var_map.iteritems():
        out_fd.write(str(i) + "," + name)
    out_fd.close()


idc = 0
# copied id -> orig id
id_map = {}
dl_facts_fd = open("facts.smt2", "w+")
def main():
    global dl_facts_fd

    g = graph_from_json()
    datalog_variables()
    copy_cfg(g, 3)
    cfg_node_store_load()
    datalog_echos_and_tainted()
    save_maps()

    dl_facts_fd.close()

if __name__ == "__main__":
    main()
