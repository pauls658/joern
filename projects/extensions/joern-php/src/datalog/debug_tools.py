import json, sys
from collections import defaultdict
from common import *

def load_sinks():
    echos = set()
    echos.update(json.load(open("tmp/sinks.json", "rb"))["results"][0]["data"][0]["row"][0])
    return echos

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


def load_data_deps():
    fd = open("datadep.csv", "r")
    datadeps = []
    for line in fd:
        datadeps.append(tuple(map(int, line.split("\t"))))
    return datadeps

"""
This is way too slow
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
"""

def unique_echos():
    id_map, _ = load_id_map()
    mapped_echos = set(map(lambda x: id_map[x], load_souffle_res()))
    echos = load_sinks()
    if mapped_echos - echos:
        print "Something is wonky"
    else:
        print "\n".join(map(str, mapped_echos))

def livevars_for_stmt(args):
    id_map, reverse_id_map = load_id_map()
    stmt = reverse_id_map[int(args[0])][0]
    var_map = load_var_map()
    print load_livedefs(stmt)

def datadeps_to_cypher():
    datadeps = load_data_deps()
    var_map, _ = load_var_map()
    id_map, _ = load_id_map()

    unique_datadeps = set()
    for def_stmt, use_stmt, var_id in datadeps:
        unique_datadeps.add((id_map[def_stmt], id_map[use_stmt], var_map[var_id]))

    fd = open("cypher_datadeps.csv", "w+")
    fd.write("def_stmt,use_stmt,var\n")
    for def_stmt, use_stmt, var_id in unique_datadeps:
        fd.write("%d,%d,%s\n" % (def_stmt, use_stmt, var_id))
    fd.close()

def defuses_to_cypher():
    fd = open("cypher_defuses.csv", "w+")
    id_map, rev_id_map = load_id_map()
    defs = load_defs()
    uses = load_uses()
    fd.write("id,defs,uses\n")
    for new, orig in id_map.iteritems():
        fd.write("%d,%s,%s\n" % (orig,
            ";".join(defs[new]),
            ";".join(uses[new])))
    fd.close()

def copied_cfg():
    id_map, rev_id_map = load_id_map()
    var_map, rev_var_map = load_var_map()
    defs = load_defs()
    uses = load_uses()
    kills = load_kills()
    fd = open("nodes.csv", "w+")
    fd.write("id,def,use,kill,orig_id\n")
    for new, orig in id_map.iteritems():
        fd.write("%d,%s,%s,%s,%d\n" % (new,
            ";".join(map(lambda vi: var_map[int(vi)], defs[new])),
            ";".join(map(lambda vi: var_map[int(vi)], uses[new])),
            ";".join(map(lambda vi: var_map[int(vi)], kills[new])),
            orig))
    fd.close()

    datadeps = load_data_deps()
    fd = open("datadeps.csv", "w+") 
    fd.write("start,end,var\n")
    for s, e, v in datadeps:
        fd.write("%d,%d,%s\n" % (s, e, var_map[v]))
    fd.close()

if __name__ == "__main__":
    if len(sys.argv) < 2:
        cmd = "<none>"
    else:
        cmd = sys.argv[1]
    cmds = {
            "livevars" : livevars_for_stmt,
            "echos" : unique_echos,
            "datadeps" : datadeps_to_cypher,
            "defuses" : defuses_to_cypher,
            "copiedcfg" : copied_cfg
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
