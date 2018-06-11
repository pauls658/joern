from collections import defaultdict
import json

pred = defaultdict(list) # AKA parent
succ = defaultdict(list) # AKA adjacent

changed = set() # this need not be global
GEN = defaultdict(list)
KILL = defaultdict(list)
IN = {}
OUT = defaultdict(set)

# adjacency list
# edges are (stmt, var)
DDG = defaultdict(list)

uses = defaultdict(set)
# var -> [stmt]
# a list of stmts that def to var
defs = defaultdict(list)

sources = []
sinks = set()

var_map = {}
id_map = {}

# debug/testing
tainted_sinks = set()
safe_sinks = set()
reverse_id_map = {}

def handle_edge(start, end):
    global pred, succ, changed
    succ[start].append(end)
    pred[end].append(start)
    changed.add(start)
    changed.add(end)

def handle_use(stmt, var):
    global uses
    uses[stmt].add(var)

def handle_def(stmt, var):
    global GEN
    GEN[stmt].append(var)
    OUT[stmt].add((stmt, var))
    defs[var].append(stmt)

def handle_source(stmt):
    global sources
    sources.append(stmt)

def handle_sink(stmt):
    global sinks
    sinks.add(stmt)

class Hell(Exception):
    pass

def load_graph_data():
    fd = open("tmp/graph.txt", "r")
    for line in fd:
        parts = line.split(' ')
        data_type = parts[0]
        data = map(lambda x: int(x.strip()), parts[1:])
        if data_type == "edge":
            handle_edge(data[0], data[1])
        elif data_type == "use":
            handle_use(data[0], data[1])
        elif data_type == "def":
            handle_def(data[0], data[1])
        elif data_type == "source":
            handle_source(data[0])
        elif data_type == "sink":
            handle_sink(data[0])
        else:
            raise Hell("undefined data type: %s!" % (data_type))
    fd.close()

def load_var_map():
    global var_map
    fd = open("tmp/var_map.csv", "r")
    for line in fd:
        i, name = map(lambda x: x.strip(), line.split(','))
        var_map[int(i)] = name
    fd.close()

def load_id_map():
    global id_map
    fd = open("tmp/id_map.csv", "r")
    for line in fd:
        new, orig = map(lambda x: int(x.strip()), line.split(','))
        id_map[new] = orig
    fd.close()

def load_test_data():
    global sinks, tainted_sinks, safe_sinks, id_map
    # warning: this only works if we did not make any copies
    # of the tainted_echo() and safe_echo()
    reverse_id_map = {v : k for k, v in id_map.iteritems()}
    tainted_sinks.update(
                json.load(open("tmp/tainted_sinks.json", "rb"))["results"][0]["data"][0]["row"][0]
            )
    safe_sinks.update(
                json.load(open("tmp/safe_sinks.json", "rb"))["results"][0]["data"][0]["row"][0]
            )
    #sinks.update(tainted_sinks)
    #sinks.update(safe_sinks)

def IN_minus_KILL(n):
    global IN, GEN
    ret = set()
    ret.update(IN[n])
    
    # KILL[n] is the set of definitions
    # that define variables defined in n
    for var in GEN[n]:
        ret.difference_update(
                map(lambda stmt: (stmt, var), defs[var])
        )

    return ret

def compute_reaching_definitions():
    global changed, IN, GEN, OUT
    # TODO: init OUT all of out
    #import pdb; pdb.set_trace()
    while changed:
        n = changed.pop()

        IN[n] = set()
        for p in pred[n]:
            IN[n] |= OUT[p]

        new_out = IN_minus_KILL(n)
        new_out.update(
                map(lambda var: (n, var), GEN[n])
        )

        if new_out != OUT[n]:
            OUT[n] = new_out
            changed.update(succ[n])

def compute_defuse_relations():
    global IN, DDG, uses

    for end_stmt, reaching_defs in IN.iteritems():
        for start_stmt, var in reaching_defs:
            #DDG[stmt2].append((stmt1, var))
            if var in uses[end_stmt]: 
                DDG[start_stmt].append(end_stmt)

# computes the reachable nodes from the give source nodes
# the reachable nodes are represented as their ids in the
# _copied_ graph, i.e. They need to be mapped back to their
# original ids in the real graph.
def compute_forward_reachability():
    global DDG, sources, sinks, id_map

    reachable_sinks = set()
    visited = set()

    stack = list(sources) # deep copy
    while stack:
        cur = stack.pop()

        if cur in visited: continue

        visited.add(cur)
        if cur in sinks:
            reachable_sinks.add(cur)
        stack.extend(DDG[cur])

    return visited, reachable_sinks

def debug():
    global tainted_sinks, safe_sinks, id_map
    load_id_map()
    load_var_map()
    load_graph_data()
    load_test_data()

    compute_reaching_definitions()
    compute_defuse_relations()
    reachable, _ = compute_forward_reachability()
    reachable = set(map(lambda x: id_map[x], reachable))
    if tainted_sinks & reachable != tainted_sinks:
        print "Test failed: tainted sinks do not match reachable sinks"
        print "Tainted sinks: " + str(tainted_sinks)
        #print "Reachable sinks: " + str(reachable_sinks)
        print "tainted - reachable: " + str(tainted_sinks - reachable_sinks)
        #print "Reachable - Tainted: " + str(reachable_sinks - tainted_sinks)
    elif (reachable & safe_sinks):
        print "Test failed: some safe sinks were reachable"
        #print "Reachable sinks: " + str(reachable_sinks)
        print "Safe sinks: " + str(safe_sinks)
        print "Reachable safe sinks: " + str(reachable_sinks & safe_sinks)
    else:
        print "Test passed! :)"

def CFGDFS(start=22114):
    global succ
    visited = set()
    stack = [start]
    import pdb; pdb.set_trace()
    while stack:
        cur = stack.pop()

        if cur in visited: continue

        visited.add(cur)
        stack.extend(succ[cur])

    return visited

def debug2():
    load_graph_data()
    return CFGDFS()

def main():
    load_id_map()
    load_var_map()
    load_graph_data()

    compute_reaching_definitions()
    compute_defuse_relations()
    print compute_forward_reachability()

if __name__ == "__main__":
    #main()
    debug2()
