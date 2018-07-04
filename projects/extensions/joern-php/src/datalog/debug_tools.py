import json, sys
from collections import defaultdict

def load_sinks():
    echos = set()
    echos.update(json.load(open("tmp/sinks.json", "rb"))["results"][0]["data"][0]["row"][0])
    return echos

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
    for line in fd:
        i, var_name = line.split(",")
        i = int(i)
        var_map[i] = var_name
    return var_map

def load_livedefs(stmt):
    fd = open("livedef.csv", "r")
    stmt_livevars = defaultdict(set)
    ret = set()
    for line in fd:
        def_stmt, var_id, this_stmt = map(int, line.split("\t"))
        #stmt_livevars[stmt].add(var_id)
        if stmt == this_stmt:
            ret.add(var_id)
    return ret

def load_souffle_res():
    s = set()
    fd = open("tainted_sink.csv", "r")
    for line in fd:
        s.add(int(line.strip()))
    return s

def unique_echos():
    id_map, _ = load_id_map()
    mapped_echos = set(map(lambda x: id_map[x], load_souffle_res()))
    echos = load_sinks()
    if mapped_echos - echos:
        print "Something is wonky"
    else:
        print mapped_echos

def livevars_for_stmt(args):
    id_map, reverse_id_map = load_id_map()
    stmt = reverse_id_map[int(args[0])][0]
    var_map = load_var_map()
    print load_livedefs(stmt)

if __name__ == "__main__":
    if len(sys.argv) < 2:
        cmd = "<none>"
    else:
        cmd = sys.argv[1]
    cmds = {
            "livevars" : livevars_for_stmt,
            "echos" : unique_echos
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
