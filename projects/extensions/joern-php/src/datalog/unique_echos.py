import json

def load_echos():
    echos = set()
    echos.update(json.load(open("echos.json", "rb"))["results"][0]["data"][0]["row"][0])
    return echos

def load_id_map():
    fd = open("id_map.csv", "r")
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

def main():
    id_map = load_id_map()
    reachables = set(map(lambda x: id_map[x], read_dl_results()))
    echos = load_echos()
    print len(reachables & echos)

if __name__ == "__main__":
    main()
