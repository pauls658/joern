<?php

require __DIR__ . "/vendor/autoload.php";
include '/home/brandon/php-ast/util.php';

$code = <<<'end'
<?php

/**
 * My testing class
 */
class BrandonsClass extends AnotherClass implements AnInterface
{
    /**
     * Some property
     */
    private $prop = 0;

    const TEST = 'string';

    use TraitA, TraitB {
        TraitA::func1 insteadof TraitB;
        TraitB::func1 as protected func2;
    }

    /**
     * Some useless constructor
     */
    function __construct(int $arg = 1)
    {
        $this->prop = $arg;
		$this->prop = 2;
    }
}
end;

$code = <<<'end'
<?php

$dependency_definitions = array(
    'url_query' => &$url_query
);

end;

$ast = ast\parse_code($code, $version=40);

echo ast_dump($ast), "\n";

//echo (new AstReverter\AstReverter)->getCode($ast);

?>
