from collections import defaultdict

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
        var_map[i] = var_name.strip()
        rev_var_map[var_name] = i
    return var_map, rev_var_map

