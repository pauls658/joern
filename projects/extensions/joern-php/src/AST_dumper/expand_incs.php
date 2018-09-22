<?php

require 'AstReverter.php';
include '/home/brandon/php-ast/util.php';

//$code = <<<'end'
//<?php
//if (($j = strpos($str, 'y')) || ($j = strpos($str, 'e'))) {}
//end;

$in_dir = "/home/brandon/php_apps/originals/addressbook";

function endsWith( $str, $sub ) {
    return ( substr( $str, strlen( $str ) - strlen( $sub ) ) == $sub );
}

function getDirContents($dir, &$results = array()){
    $files = scandir($dir);

    foreach($files as $key => $value){
        $path = realpath($dir.DIRECTORY_SEPARATOR.$value);
        if(!is_dir($path)) {
			if (endsWith($path, ".php")) {
				$results[] = $path;
			}
        } else if($value != "." && $value != "..") {
            getDirContents($path, $results);
            //$results[] = $path;
        }
    }

    return $results;
}

function getAst($path) {
	global $expandedAsts;
	if (!in_array($path, $expandedAsts)) {
		global $asts;
		expandIncs($asts[$path]);
		$expandedAsts[$path] = $asts[$path];
	}
	return $expandedAsts[$path];
}

function findAst($inc_expr) {
	global $asts;
	$matches = array();
	foreach ($asts as $path => $ast) {
		if (preg_match($inc_expr, $path))
			$matches []= $path;
	}
	if (count($matches) == 1) {
		return getAst($matches[0]);
	} else {
		return null;
	}
}

function getIncExpr($node) {
	if (gettype($node) == "string") {
		$pieces = explode("/", $node);
		$fname = end($pieces);
		return "/.*" . preg_quote($fname) . "/";
	} else {
		return null;
	}
}


function getIncAst($node) {
	$inc_expr = getIncExpr($node->children["expr"]);
	if ($inc_expr != null) {
		// inc_expr is a regex of the included file
		// maybe i want to change this later
		return findAst($inc_expr);
	} else {
		// we don't know
		return null;
	}
}

function expandIncs($node) {
	if (!isset($node->children)) return;

	foreach ($node->children as $i => $c) {
		if (!is_object($c) || !is_a($c, "ast\Node")) continue;
		if ($c->kind == ast\AST_INCLUDE_OR_EVAL &&
			((ast\flags\EXEC_INCLUDE == $c->flags) ||
			(ast\flags\EXEC_REQUIRE == $c->flags))) {
			$ast = getIncAst($c);
			if ($ast != null) {
				$node->children[$i] = $ast;
			}
		} else {
			expandIncs($c);
		}
	}
}

$asts = array();
$expandedAsts = array();
foreach (getDirContents($in_dir) as $f) {
	$asts[$f] = ast\parse_file($f, $version=40);
}
expandIncs($asts["$in_dir/index.php"]);


echo (new AstReverter\AstReverter)->getCode($asts["$in_dir/index.php"]);

?>
