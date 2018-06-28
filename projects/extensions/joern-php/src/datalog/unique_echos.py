import json

def load_echos():
    echos = set()
    echos.update(json.load(open("tmp/sinks.json", "rb"))["results"][0]["data"][0]["row"][0])
    return echos

def load_id_map():
    fd = open("tmp/id_map.csv", "r")
    id_map = {}
    for line in fd:
        new, orig = map(int, line.strip().split(","))
        id_map[new] = orig
    return id_map

def read3lines(fd):
    line = fd.readline()
    if not line:
        return None
    line += fd.readline()
    line += fd.readline()
    return line

def read_dl_results():
    fd = open("reachables.csv", "r")
    fact = read3lines(fd)
    reachables = set()
    while fact:
        # (var, stmt, reachable)
        fact = fact.split("\n", 2)
        fact = map(lambda x: int(x.replace("#x", "").strip(), 16), fact)
        reachables.add(fact[2])
        fact = read3lines(fd)
    return reachables

def load_souffle_res():
    s = set()
    fd = open("tainted_sink.csv", "r")
    for line in fd:
        s.add(int(line.strip()))
    return s

def main():
    id_map = load_id_map()
    mapped_echos = set(map(lambda x: id_map[x], load_souffle_res()))
    echos = load_echos()
    if mapped_echos - echos:
        print "Something is wonky"
    else:
        print len(mapped_echos)

if __name__ == "__main__":
    main()
