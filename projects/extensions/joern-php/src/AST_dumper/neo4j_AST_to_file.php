<?php
require __DIR__ . "/vendor/autoload.php";

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

function Node_from_json($node) {
	global $md_by_name;
	if (strtolower($node['type']) == "null") return null;
	else if (!array_key_exists($node['type'], $md_by_name)) {
		return $node['code'];
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
		$refs[$start_id]->children['name'] = $end['code'];
	} else {
		$end_id = (int)$end['id'];
		if (!array_key_exists($end_id, $refs))
			$refs[$end_id] = Node_from_json($end);
		$refs[$start_id]->children[$reltype] = $refs[$end_id];
	}
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
		ksort($node_refs[$id]->children);

	return $node_refs;
}

$json_str = file_get_contents("relations.json");
$json = json_decode($json_str, true);
$refs = json_to_php_ast($json["results"][0]);
echo (new AstReverter\AstReverter)->getCode($refs[(int)$argv[1]]);
?>
