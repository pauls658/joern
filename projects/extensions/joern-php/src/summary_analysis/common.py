from collections import defaultdict
import json

"""
Returns a list of the copied-cfg node ids that are both
tainted and sinks.
"""
def load_souffle_res():
    s = set()
    fd = open("tainted_sink.csv", "r")
    for line in fd:
        s.add(int(line.strip()))
    return s

def load_id_map():
    fd = open("tmp/id_map.csv", "r")
    id_map = {}
    reverse_id_map = defaultdict(list)
    for line in fd:
        new, orig = map(int, line.strip().split(","))
        id_map[new] = orig
        reverse_id_map[orig].append(new)
    return id_map, reverse_id_map

def load_var_map():
    fd = open("tmp/var_map.csv", "r")
    var_map = {}
    rev_var_map = {}
    for line in fd:
        i, var_name = line.split(",")
        i = int(i)
        var_name = var_name.strip()
        var_map[i] = var_name
        rev_var_map[var_name] = i
    return var_map, rev_var_map

def load_cypher_graph_nodes():
    nodes = {}
    rows = json.load(open("tmp/nodes.json", "rb"))["results"][0]["data"]
    for row in rows:
        row = row["row"][0]
        i = int(row["id"])
        nodes[i] = row
    return nodes

def load_kills():
    fd = open("tmp/kill.csv", "r")
    kills = defaultdict(list)
    for line in fd:
        n, var = map(int, line.strip().split())
        kills[n].append(var)
    fd.close()
    return kills

def load_defs():
    fd = open("tmp/def.csv", "r")
    defs = defaultdict(list)
    for line in fd:
        n, var = line.strip().split("\t")
        n = int(n)
        defs[n].append(var)
    fd.close()
    return defs

def load_uses():
    fd = open("tmp/use.csv", "r")
    uses = defaultdict(list)
    for line in fd:
        n, var = line.strip().split("\t")
        n = int(n)
        uses[n].append(var)
    fd.close()
    return uses

def load_ctrldefs():
    fd = open("tmp/ctrldef.csv", "rb")
    ctrldefs = defaultdict(list)
    for line in fd:
        n, var = line.strip().split("\t")
        n = int(n)
        ctrldefs[n].append(var)
    fd.close()
    return ctrldefs

