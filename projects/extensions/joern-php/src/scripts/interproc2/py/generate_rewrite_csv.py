from collections import defaultdict
import os
SCRIPT_DIR = os.path.dirname(os.path.realpath(__file__))

def output_work_csv(name_ids, id_globals):
    id_prefix = {}
    for name, ids in name_ids.iteritems():
        prefix_num = 0
        for i in ids:
            id_prefix[i] = name + "_" + str(prefix_num) + "_"
            prefix_num += 1

    with open(SCRIPT_DIR + "/../csv/rewrite_params.csv", "wb+") as out_csv:
        out_csv.write("func_id,prefix,globals\n")
        for i, params in id_globals.iteritems():
            out_csv.write(str(i) + ',' + id_prefix[i] + ',' +\
                            ';'.join(id_globals[i]) + '\n')

def organize_data(line_data):
    name_ids = defaultdict(set)
    id_globals = defaultdict(set)
    for datas in line_data:
        name_ids[datas[0]].add(datas[1])
        id_globals[datas[1]].add(datas[2])

    return name_ids, id_globals

def read_csv():
    line_data = []
    with open(SCRIPT_DIR + "/../csv/decl_info.csv", "rb") as csv_file:
        file_iter = iter(csv_file)
        file_iter.next() # skip header
        for line in file_iter:
            datas = line.split(",")
            datas = map(lambda d: d.replace('"', '').strip(), datas)
            datas[1] = int(datas[1])
            if datas[2] == "NULL":
                datas[2] = ""
            line_data.append(datas)

    return line_data

def main():
    line_data = read_csv()
    name_ids, id_globals = organize_data(line_data)
    output_work_csv(name_ids, id_globals)
    
if __name__ == "__main__":
    main()
