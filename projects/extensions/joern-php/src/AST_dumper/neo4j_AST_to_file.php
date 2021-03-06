<?php
require 'AstReverter.php';
//include '/home/brandon/php-ast/util.php';

use ast\Node;
use ast\Metadata;
use ast\flags;

$mds = ast\get_metadata();

$md_by_name = array();
foreach ($mds as $md) {
	$md_by_name[$md->name] = $md;
}

$numeric_children = array();
$numeric_children["AST_ARG_LIST"] = 1;
$numeric_children["AST_ARRAY"] = 1;
$numeric_children["AST_CATCH_LIST"] = 1;
$numeric_children["AST_CLASS_CONST_DECL"] = 1;
$numeric_children["AST_CLOSURE_USES"] = 1;
$numeric_children["AST_CONST_DECL"] = 1;
$numeric_children["AST_ENCAPS_LIST"] = 1;
$numeric_children["AST_EXPR_LIST"] = 1;
$numeric_children["AST_IF"] = 1;
$numeric_children["AST_LIST"] = 1;
$numeric_children["AST_NAME_LIST"] = 1;
$numeric_children["AST_PARAM_LIST"] = 1;
$numeric_children["AST_PROP_DECL"] = 1;
$numeric_children["AST_STMT_LIST"] = 1;
$numeric_children["AST_SWITCH_LIST"] = 1;
$numeric_children["AST_TRAIT_ADAPTATIONS"] = 1;
$numeric_children["AST_USE"] = 1;

function get_node_code($node) {
	switch($node['type']) {
	case "integer":
		return (int)$node['code'];
	case "double":
		return (double)$node['code'];
		break;
	case "string":
		return (string)$node['code'];
	default:
		throw Exception("Fuck you I'm an exception");
	}
}

function Node_from_json($node) {
	global $md_by_name;
	if (strtolower($node['type']) == "null") return null;
	else if (!array_key_exists($node['type'], $md_by_name)) {
		return get_node_code($node);
	}

	$node_id = (int)$node['id'];
	$node_kind = $md_by_name[$node['type']]->kind;


	$node_flags = 0;
	if (array_key_exists('flags', $node)) {
		foreach ($node['flags'] as $flag) {
			// HACK
			// See the comment in Exporter.php about the BY_REFERENCE flag
			// in the phpjoern project
			if ($flag == "BY_REFERENCE")
				$node_flags |= 1;
			else
				$node_flags |= constant("\ast\\flags\\" . $flag);
		}
	}
	$new_node = new Node($node_kind, $node_flags, array(), $node['lineno']);
	if (array_key_exists('name', $node))
		$new_node->name = $node['name'];
	return $new_node;
}

function add_relation($start, $reltype, $end, &$refs) {
	$start_id = (int)$start['id'];
	if (!array_key_exists($start_id, $refs))
		$refs[$start_id] = Node_from_json($start);

	if ($reltype == 'name' && array_key_exists('code', $end)) {
		// no need to make a node object
		$refs[$start_id]->children['name'] = get_node_code($end);
	} elseif ($reltype == 'depth') {
		// break statement
		$refs[$start_id]->children['depth'] = $end['type'] == "NULL" ? null : get_node_code($end);
	} else {
		$end_id = $end['id'];
		if (!array_key_exists($end_id, $refs))
			$refs[$end_id] = Node_from_json($end);

		// If we added new statements, there is the possibility they collide with
		// other new statements. The child_rel of new statements is always a floating point
		// so we can just tack on 0's til they dont collide
		// collisions cannot happen for statements in the original AST.
		$relkey = (string)$reltype;
		while(array_key_exists($relkey, $refs[$start_id]->children))
			$relkey .= "0";

		$refs[$start_id]->children[$relkey] = $refs[$end_id];
	}
}

function child_cmp($a, $b) {
	$a = (float)$a;
	$b = (float)$b;
    if ($a == $b) {
        return 0;
    }
    return ($a < $b) ? -1 : 1;
}

function json_to_php_ast($json) {
	global $numeric_children;
	// maps node id to the node's AST object
	$node_refs = array();
	$needs_sorting = [];
	$data = $json['data'];

	foreach ($data as $i => $entry) {
		$row = $entry['row'];
		if (array_key_exists($row[0]['type'], $numeric_children))
			$needs_sorting[] = (int)$row[0]['id'];
		if (!array_key_exists('child_rel', $row[1])) {
			continue; // artificial this param
		}
		add_relation($row[0], trim($row[1]['child_rel']), $row[2], $node_refs);
	}
	foreach ($needs_sorting as $id)
		uksort($node_refs[$id]->children, 'child_cmp');

	return $node_refs;
}

$json_str = file_get_contents("relations.json");
$json = json_decode($json_str, true);
$refs = json_to_php_ast($json["results"][0]);
//echo ast_dump($refs[(int)$argv[1]]), "\n";

echo (new AstReverter\AstReverter)->getCode($refs[(int)$argv[1]]);
?>
