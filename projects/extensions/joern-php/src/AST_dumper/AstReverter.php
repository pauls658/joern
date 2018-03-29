<?php

namespace AstReverter;

use ast\Node;

ini_set('assert.exception', 1);

class AstReverter
{
    private $indentationLevel = 0;
    private $inCatchBlock = false;

    const INDENTATION_CHAR = ' ';
    const INDENTATION_SIZE = 4;

    public function __construct()
    {
        // nothing to do
    }

    private function revertAST($node) : string
    {
        if (!$node instanceof Node) {
            switch (gettype($node)) {
                case 'integer':
                case 'double':
                case 'boolean':
                    return $node;
                case 'string':
                    return '"' . $this->sanitiseString($node) . '"';
                default:
                    // an array, null, etc, should never come through here
                    assert(false, 'Unknown type ('. gettype($node) . ') found.');
            }
        }

        switch ($node->kind) {
            case \ast\AST_ARG_LIST:
                return $this->argList($node);
            case \ast\AST_ARRAY:
                return $this->array($node);
            case \ast\AST_ARRAY_ELEM:
                return $this->arrayElem($node);
            case \ast\AST_ASSIGN:
                return $this->assign($node);
            case \ast\AST_ASSIGN_OP:
                return $this->assignOp($node);
            case \ast\AST_ASSIGN_REF:
                return $this->assignRef($node);
            case \ast\AST_BINARY_OP:
                return $this->binaryOp($node);
            case \ast\AST_BREAK:
                return $this->break($node);
            case \ast\AST_CALL:
                return $this->call($node);
            case \ast\AST_CAST:
                return $this->cast($node);
            case \ast\AST_CATCH:
                return $this->catch($node);
            case \ast\AST_CATCH_LIST:
                return $this->catchList($node);
            case \ast\AST_CLASS:
                return $this->class($node);
            case \ast\AST_CLASS_CONST:
                return $this->classConst($node);
            case \ast\AST_CLASS_CONST_DECL:
                return $this->classConstDecl($node);
            case \ast\AST_CLONE:
                return $this->clone($node);
            case \ast\AST_CLOSURE:
                return $this->closure($node);
            case \ast\AST_CLOSURE_VAR:
                return $this->closureVar($node);
            case \ast\AST_COALESCE:
                return $this->coalesce($node);
            case \ast\AST_CONDITIONAL:
                return $this->conditional($node);
            case \ast\AST_CONST:
                return $this->const($node);
            case \ast\AST_CONST_DECL:
                return $this->constDecl($node);
            case \ast\AST_CONST_ELEM:
                return $this->constElem($node);
            case \ast\AST_CONTINUE:
                return $this->continue($node);
            case \ast\AST_DECLARE:
                return $this->declare($node);
            case \ast\AST_DIM:
                return $this->dim($node);
            case \ast\AST_DO_WHILE:
                return $this->doWhile($node);
            case \ast\AST_ECHO:
                return $this->echo($node);
            case \ast\AST_EMPTY:
                return $this->empty($node);
            case \ast\AST_ENCAPS_LIST:
                return $this->encapsList($node);
            case \ast\AST_EXIT:
                return $this->exit($node);
            case \ast\AST_EXPR_LIST:
                return $this->exprList($node);
            case \ast\AST_FOR:
                return $this->for($node);
            case \ast\AST_FOREACH:
                return $this->foreach($node);
            case \ast\AST_FUNC_DECL:
                return $this->funcDecl($node);
            case \ast\AST_GLOBAL:
                return $this->global($node);
            case \ast\AST_GOTO:
                return $this->goto($node);
            case \ast\AST_GROUP_USE:
                return $this->groupUse($node);
            case \ast\AST_HALT_COMPILER:
                return $this->haltCompiler($node);
            case \ast\AST_IF:
                return $this->if($node);
            case \ast\AST_IF_ELEM:
                return $this->ifElem($node);
            case \ast\AST_INCLUDE_OR_EVAL:
                return $this->includeOrEval($node);
            case \ast\AST_INSTANCEOF:
                return $this->instanceof($node);
            case \ast\AST_ISSET:
                return $this->isset($node);
            case \ast\AST_LABEL:
                return $this->label($node);
            case \ast\AST_LIST:
                return $this->list($node);
            case \ast\AST_MAGIC_CONST:
                return $this->magicConst($node);
            case \ast\AST_METHOD:
                return $this->method($node);
            case \ast\AST_METHOD_CALL:
                return $this->methodCall($node);
            case \ast\AST_METHOD_REFERENCE:
                return $this->methodReference($node);
            case \ast\AST_NAME:
                return $this->name($node);
            case \ast\AST_NAMESPACE:
                return $this->namespace($node);
            case \ast\AST_NAME_LIST:
                return $this->nameList($node);
            case \ast\AST_NEW:
                return $this->new($node);
            case \ast\AST_NULLABLE_TYPE:
                return $this->nullableType($node);
            case \ast\AST_PARAM:
                return $this->param($node);
            case \ast\AST_PARAM_LIST:
                return $this->paramList($node);
            case \ast\AST_POST_DEC:
                return $this->postDec($node);
            case \ast\AST_POST_INC:
                return $this->postInc($node);
            case \ast\AST_PRE_DEC:
                return $this->preDec($node);
            case \ast\AST_PRE_INC:
                return $this->preInc($node);
            case \ast\AST_PRINT:
                return $this->print($node);
            case \ast\AST_PROP:
                return $this->prop($node);
            case \ast\AST_PROP_DECL:
                return $this->propDecl($node);
            case \ast\AST_PROP_ELEM:
                return $this->propElem($node);
            case \ast\AST_REF:
                return $this->ref($node);
            case \ast\AST_RETURN:
                return $this->return($node);
            case \ast\AST_SHELL_EXEC:
                return $this->shellExec($node);
            case \ast\AST_STATIC:
                return $this->static($node);
            case \ast\AST_STATIC_CALL:
                return $this->staticCall($node);
            case \ast\AST_STATIC_PROP:
                return $this->staticProp($node);
            case \ast\AST_STMT_LIST:
                return $this->stmtList($node);
            case \ast\AST_SWITCH:
                return $this->switch($node);
            case \ast\AST_SWITCH_CASE:
                return $this->switchCase($node);
            case \ast\AST_SWITCH_LIST:
                return $this->switchList($node);
            case \ast\AST_THROW:
                return $this->throw($node);
            case \ast\AST_TRAIT_ADAPTATIONS:
                return $this->traitAdaptations($node);
            case \ast\AST_TRAIT_ALIAS:
                return $this->traitAlias($node);
            case \ast\AST_TRAIT_PRECEDENCE:
                return $this->traitPrecedence($node);
            case \ast\AST_TRY:
                return $this->try($node);
            case \ast\AST_TYPE:
                return $this->type($node);
            case \ast\AST_UNARY_OP:
                return $this->unaryOp($node);
            case \ast\AST_UNPACK:
                return $this->unpack($node);
            case \ast\AST_UNSET:
                return $this->unset($node);
            case \ast\AST_USE:
                return $this->use($node);
            case \ast\AST_USE_ELEM:
                return $this->useElem($node);
            case \ast\AST_USE_TRAIT:
                return $this->useTrait($node);
            case \ast\AST_VAR:
                return $this->var($node);
            case \ast\AST_WHILE:
                return $this->while($node);
            case \ast\AST_YIELD:
                return $this->yield($node);
            case \ast\AST_YIELD_FROM:
                return $this->yieldFrom($node);
            default:
                assert(false, 'Unknown AST kind (' . \ast\get_kind_name($node->kind) . ') found.');
        }
    }

    /**
     * Custom method not node-related to encapsulate common logic of
     * methodCall() and staticCall() methods.
     */
    private function abstractCall(Node $lhs, $rhs, Node $args, string $op) : string
    {
        $code = '';

        if ($lhs->kind === \ast\AST_NEW) {
            $code .= "({$this->revertAST($lhs)})";
        } else {
            $code .= $this->revertAST($lhs);
        }

        $code .= $op;

        if ($rhs instanceof Node) {
            $code .= $this->revertAST($rhs);
        } else {
            $code .= $rhs;
        }

        $code .= $this->revertAST($args);

        return $code;
    }

    /**
     * Custom method not node-related to encapsulate common logic of
     * prop() and staticProp() methods.
     */
    private function abstractProp(Node $lhs, $rhs, string $op) : string
    {
        $code = '';
        $openBrace = '';
        $closeBrace = '';

        // Maintain the right associativity
        if (isset($rhs->kind) && $rhs->kind !== \ast\AST_VAR) {
            $openBrace = '{';
            $closeBrace = '}';
        }

        $code .= $this->revertAST($lhs)
            . $op
            . $openBrace;

        if ($rhs instanceof Node) {
            $code .= $this->revertAST($rhs);
        } else {
            $code .= $rhs;
        }

        $code .= $closeBrace;

        return $code;
    }

    private function argList(Node $node) : string
    {
        return '('
            . $this->commaSeparatedValues($node, ', ')
            . ')';
    }

    private function array(Node $node) : string
    {
        switch ($node->flags) {
            case \ast\flags\ARRAY_SYNTAX_LIST:
                $code = 'list(';
                $closing = ')';
                break;
            case \ast\flags\ARRAY_SYNTAX_LONG:
                $code = 'array(';
                $closing = ')';
                break;
            case \ast\flags\ARRAY_SYNTAX_SHORT:
                $code = '[';
                $closing = ']';
                break;
            case 0:
                // nothing to do - required for PHP 7.0
                $code = '[';
                $closing = ']';
                break;
            default:
                assert(false, "Unknown flag ({$node->flags}) for AST_ARRAY found.");
        }

        if ($node->children !== []) {
            $code .= $this->commaSeparatedValues($node, ', ');
        }

        $code .= $closing;

        return $code;
    }

    private function arrayElem(Node $node) : string
    {
        $code = '';

        if ($node->children['key'] === null) {
            $code .= $this->revertAST($node->children['value']);
        } else {
            $code .= "{$this->revertAST($node->children['key'])} => ";

            if ($node->flags === \ast\flags\PARAM_REF) {
                $code .= '&';
            }

            $code .= $this->revertAST($node->children['value']);
        }

        return $code;
    }

    private function assign(Node $node) : string
    {
        return $this->revertAST($node->children['var'])
            . ' = '
            . $this->revertAST($node->children['expr']);
    }

    private function assignOp(Node $node) : string
    {
        $op = '';

        switch ($node->flags) {
            case \ast\flags\BINARY_BITWISE_OR:
                $op = '|=';
                break;
            case \ast\flags\BINARY_BITWISE_AND:
                $op = '&=';
                break;
            case \ast\flags\BINARY_BITWISE_XOR:
                $op = '^=';
                break;
            case \ast\flags\BINARY_CONCAT:
                $op = '.=';
                break;
            case \ast\flags\BINARY_ADD:
                $op = '+=';
                break;
            case \ast\flags\BINARY_SUB:
                $op = '-=';
                break;
            case \ast\flags\BINARY_MUL:
                $op = '*=';
                break;
            case \ast\flags\BINARY_DIV:
                $op = '/=';
                break;
            case \ast\flags\BINARY_MOD:
                $op = '%=';
                break;
            case \ast\flags\BINARY_POW:
                $op = '**=';
                break;
            case \ast\flags\BINARY_SHIFT_LEFT:
                $op = '<<=';
                break;
            case \ast\flags\BINARY_SHIFT_RIGHT:
                $op = '>>=';
                break;
            default:
                assert(false, "Unknown flag ({$node->flags}) for AST_ASSIGN_OP found.");
        }

        return $this->revertAST($node->children['var'])
            . ' '
            . $op
            . ' '
            . $this->revertAST($node->children['expr']);
    }

    private function assignRef(Node $node) : string
    {
        return $this->revertAST($node->children['var'])
            . ' = &'
            . $this->revertAST($node->children['expr']);
    }

    private function binaryOp(Node $node) : string
    {
        $code = '';
        $op = '';
        $enforcePrecendence = true;

        switch ($node->flags) {
            case \ast\flags\BINARY_BITWISE_OR:
                $op = '|';
                break;
            case \ast\flags\BINARY_BITWISE_AND:
                $op = '&';
                break;
            case \ast\flags\BINARY_BITWISE_XOR:
                $op = '^';
                break;
            case \ast\flags\BINARY_CONCAT:
                $enforcePrecendence = false;
                $op = '.';
                break;
            case \ast\flags\BINARY_ADD:
                $op = '+';
                break;
            case \ast\flags\BINARY_SUB:
                $op = '-';
                break;
            case \ast\flags\BINARY_MUL:
                $op = '*';
                break;
            case \ast\flags\BINARY_DIV:
                $op = '/';
                break;
            case \ast\flags\BINARY_MOD:
                $op = '%';
                break;
            case \ast\flags\BINARY_POW:
                $op = '**';
                break;
            case \ast\flags\BINARY_SHIFT_LEFT:
                $op = '<<';
                break;
            case \ast\flags\BINARY_SHIFT_RIGHT:
                $op = '>>';
                break;
            case \ast\flags\BINARY_BOOL_XOR:
                $op = 'xor';
                break;
            case \ast\flags\BINARY_BOOL_OR:
                $op = '||';
                break;
            case \ast\flags\BINARY_BOOL_AND:
                $op = '&&';
                break;
            case \ast\flags\BINARY_IS_IDENTICAL:
                $op = '===';
                break;
            case \ast\flags\BINARY_IS_NOT_IDENTICAL:
                $op = '!==';
                break;
            case \ast\flags\BINARY_IS_EQUAL:
                $op = '==';
                break;
            case \ast\flags\BINARY_IS_NOT_EQUAL:
                $op = '!=';
                break;
            case \ast\flags\BINARY_IS_SMALLER:
                $op = '<';
                break;
            case \ast\flags\BINARY_IS_SMALLER_OR_EQUAL:
                $op = '<=';
                break;
            case \ast\flags\BINARY_IS_GREATER:
                $op = '>';
                break;
            case \ast\flags\BINARY_IS_GREATER_OR_EQUAL:
                $op = '>=';
                break;
            case \ast\flags\BINARY_SPACESHIP:
                $op = '<=>';
                break;
            default:
                assert(false, "Unknown flag ({$node->flags}) for AST_BINARY_OP found.");
        }

        $buffer = [];

        foreach (['left', 'right'] as $child) {
            if (
                $enforcePrecendence
                && $node->children[$child] instanceof Node
				&& 
				($node->children[$child]->kind === \ast\AST_BINARY_OP ||
				$node->children[$child]->kind === \ast\AST_ASSIGN)
            ) {
                $buffer[] = '(' . $this->revertAST($node->children[$child]) . ')';
                continue;
            }

            $buffer[] = $this->revertAST($node->children[$child]);
        }

        return implode(' ' . $op . ' ', $buffer);
    }

    private function break(Node $node) : string
    {
        $code = 'break';

        if ($node->children['depth'] !== null) {
            $code .= " {$this->revertAST($node->children['depth'])}";
        }

        return $code;
    }

    private function call(Node $node) : string
    {
        return $this->revertAST($node->children['expr'])
            . $this->revertAST($node->children['args']);
    }

    private function cast(Node $node) : string
    {
        $code = '(';

        switch ($node->flags) {
            case \ast\flags\TYPE_NULL:
                $code .= 'unset';
                break;
            case \ast\flags\TYPE_BOOL:
                $code .= 'bool';
                break;
            case \ast\flags\TYPE_LONG:
                $code .= 'int';
                break;
            case \ast\flags\TYPE_DOUBLE:
                $code .= 'float';
                break;
            case \ast\flags\TYPE_STRING:
                $code .= 'string';
                break;
            case \ast\flags\TYPE_ARRAY:
                $code .= 'array';
                break;
            case \ast\flags\TYPE_OBJECT:
                $code .= 'object';
                break;
            default:
                assert(false, "Unknown cast type ({$node->flags}) for AST_CAST found.");
        }

        $code .= ') ' . $this->revertAST($node->children['expr']);

        return $code;
    }

    private function catch(Node $node) : string
    {
        $code = ' catch (' . $this->revertAST($node->children['class']) . ' ';
        $code .= $this->revertAST($node->children['var']);
        $code .= ') {' . PHP_EOL;

        ++$this->indentationLevel;

        $code .= $this->revertAST($node->children['stmts']);

        --$this->indentationLevel;

        $code .= $this->indent() . '}';

        return $code;
    }

    private function catchList(Node $node) : string
    {
        $code = '';

        $this->inCatchBlock = true;

        foreach ($node->children as $catch) {
            $code .= $this->revertAST($catch);
        }

        $this->inCatchBlock = false;

        return $code;
    }

    /**
     * The second argument is for anonymous classes.
     */
    private function class(Node $node, Node $args = null) : string
    {
        $code = '';

        if (isset($node->docComment)) {
            $code .= $node->docComment . PHP_EOL;
        }

        $modifier = '';
        $type = 'class';
        $extends = '';
        $implements = '';

        switch ($node->flags) {
            case \ast\flags\CLASS_ABSTRACT:
                $modifier = 'abstract ';
                break;
            case \ast\flags\CLASS_FINAL:
                $modifier = 'final ';
                break;
            case \ast\flags\CLASS_TRAIT:
                $type = 'trait';
                break;
            case \ast\flags\CLASS_INTERFACE:
                $type = 'interface';
                break;
            case 0:
                // no flag used
                break;
            case \ast\flags\CLASS_ANONYMOUS:
                // anonymous class
                break;
            default:
                assert(false, "Unknown flag ({$node->flags}) for AST_CLASS found.");
        }

        if ($node->children['extends'] !== null) {
            $extends .= ' extends ' . $this->revertAST($node->children['extends']);
        }

        if ($node->children['implements'] !== null) {
            if ($type === 'interface') {
                $implements .= ' extends ';
            } else {
                $implements .= ' implements ';
            }

            $implements .= $this->revertAST($node->children['implements']);
        }

        $code .= $modifier . $type . ' ' . $node->name;

        if ($args !== null) {
            $code .= $this->revertAST($args);
        }

        $code .= $extends . $implements;

        if ($args === null) {
            $code .= PHP_EOL . $this->indent();
        } else {
            $code .= ' ';
        }

        $code .= '{' . PHP_EOL;

        ++$this->indentationLevel;

        $code .= $this->revertAST($node->children['stmts']);

        --$this->indentationLevel;

        $code .= $this->indent() . '}';

        return $code;
    }

    private function classConst(Node $node) : string
    {
        $code = $this->revertAST($node->children['class']) . '::';

        if ($node->children['const'] instanceof Node) {
            $code .= $this->revertAST($node->children['const']);
        } else {
            $code .= $node->children['const'];
        }

        return $code;
    }

    private function classConstDecl(Node $node) : string
    {
        return $this->constDecl($node);
    }

    private function clone(Node $node) : string
    {
        return 'clone ' . $this->revertAST($node->children['expr']);
    }

    private function closure(Node $node) : string
    {
        $code = 'function ';

        if ($node->flags === \ast\flags\RETURNS_REF) {
            $code .= '&';
        }

        $code .= $this->revertAST($node->children['params']);

        if ($node->children['uses'] !== null) {
            $code .= ' use ('
                . $this->commaSeparatedValues($node->children['uses'], ', ')
                . ')';
        }

        if ($node->children['returnType'] !== null) {
            $code .= ' : ' . $this->revertAST($node->children['returnType']);
        }

        $code .= ' {' . PHP_EOL;

        ++$this->indentationLevel;

        $code .= $this->revertAST($node->children['stmts']);

        --$this->indentationLevel;

        $code .= $this->indent() . '}';

        return $code;
    }

    private function closureVar(Node $node) : string
    {
        $code = '';

        if ($node->flags === \ast\flags\PARAM_REF) {
            $code .= '&';
        }

        $code .= '$' . $node->children['name'];

        return $code;
    }

    private function coalesce(Node $node) : string
    {
        return '('
            . $this->revertAST($node->children['left'])
            . ' ?? '
            . $this->revertAST($node->children['right'])
            . ')';
    }

    /**
     * Custom method for building comma-deliniated lists.
     *
     * Sometimes, a space is not wanted after a comma because it causes
     * trailing white space in code, like after multiple property declarations.
     *
     * The NULL check is required for list(,,, $a).
     */
    private function commaSeparatedValues(Node $node, string $separator) : string
    {
        $aggregator = [];

        foreach ($node->children as $child) {
            $aggregator[] = ($child === null) ? null : $this->revertAST($child);
        }

        return implode($separator, $aggregator);
    }

    private function conditional(Node $node) : string
    {
        return '('
            . $this->revertAST($node->children['cond'])
            . ' ? '
            . $this->revertAST($node->children['true'])
            . ' : '
            . $this->revertAST($node->children['false'])
            . ')';
    }

    private function const(Node $node) : string
    {
        return $this->revertAST($node->children['name']);
    }

    private function constDecl(Node $node, $setConst = true) : string
    {
        $code = '';

        switch ($node->flags) {
            case \ast\flags\MODIFIER_PUBLIC:
                $code = 'public ';
                break;
            case \ast\flags\MODIFIER_PROTECTED:
                $code = 'protected ';
                break;
            case \ast\flags\MODIFIER_PRIVATE:
                $code = 'private ';
                break;
            case 0:
                // nothing to do - for PHP 7.0
                break;
            default:
                assert(false, "Unknown flag ({$node->flags}) for AST_CONST_DECL found.");
        }

        if ($setConst) {
            $code .= 'const ';
        }

        $code .= $this->commaSeparatedValues($node, ', ');

        return $code;
    }

    private function constElem(Node $node) : string
    {
        return $node->children['name']
            . ' = '
            . $this->revertAST($node->children['value']);
    }

    private function continue(Node $node) : string
    {
        $code = 'continue';

        if ($node->children['depth'] !== null) {
            $code .= ' ' . $this->revertAST($node->children['depth']);
        }

        return $code;
    }

    /**
     * Custom method used to wrap single statement bodies into a AST_STMT_LIST.
     *
     * Used when braces are omitted for single statement bodies, like:
     * while (1)
     *     doSomething();
     */
    private function createStmtList($node) : Node
    {
        $node2 = new Node;
        $node2->kind = \ast\AST_STMT_LIST;
        $node2->children = [$node];

        return $node2;
    }

    private function declare(Node $node) : string
    {
        $code = 'declare(' . $this->constDecl($node->children['declares'], false) . ')';

        if ($node->children['stmts'] !== null) {
            $code .= ' {' . PHP_EOL;
            ++$this->indentationLevel;
            $code .= $this->revertAST($node->children['stmts']);
            --$this->indentationLevel;
            $code .= $this->indent() . '}';
        }

        return $code;
    }

    private function dim(Node $node) : string
    {
        $code = $this->revertAST($node->children['expr']) . '[';

        if ($node->children['dim'] !== null) {
            $code .= $this->revertAST($node->children['dim']);
        }

        $code .= ']';

        return $code;
    }

    private function doWhile(Node $node) : string
    {
        $code = 'do {' . PHP_EOL;

        ++$this->indentationLevel;

        $code .= $this->revertAST($node->children['stmts']);

        --$this->indentationLevel;

        $code .= $this->indent()
            . '} while ('
            . $this->revertAST($node->children['cond'])
            . ')';

        return $code;
    }

    private function echo(Node $node) : string
    {
        return 'echo ' . $this->revertAST($node->children['expr']);
    }

    private function empty(Node $node) : string
    {
        return 'empty(' . $this->revertAST($node->children['expr']) . ')';
    }

    private function encapsList(Node $node) : string
    {
        $code = '"';

        foreach ($node->children as $child) {
            if ($child instanceof Node) {
                $code .= '{' . $this->revertAST($child) . '}';
            } else {
                $code .= $this->sanitiseString($child);
            }
        }

        $code .= '"';

        return $code;
    }

    private function exit(Node $node) : string
    {
        $code = 'die';

        if ($node->children['expr'] !== null) {
            $code .= '(' . $this->revertAST($node->children['expr']) .')';
        }

        return $code;
    }

    private function exprList(Node $node) : string
    {
        return $this->commaSeparatedValues($node, ', ');
    }

    private function for(Node $node) : string
    {
        $code = 'for (';

        if ($node->children['init'] !== null) {
            $code .= $this->revertAST($node->children['init']);
        }

        $code .= ';';

        if ($node->children['cond'] !== null) {
            $code .= ' ' . $this->revertAST($node->children['cond']);
        }

        $code .= ';';

        if ($node->children['loop'] !== null) {
            $code .= ' ' . $this->revertAST($node->children['loop']);
        }

        $code .= ')';

        $bodyNode = $node->children['stmts'];

        if ($bodyNode !== null && $bodyNode->children !== []) {
            if (
                !$bodyNode instanceof Node
                || $bodyNode->kind !== \ast\AST_STMT_LIST
            ) {
                $bodyNode = $this->createStmtList($bodyNode);
            }

            $code .=' {' . PHP_EOL;

            ++$this->indentationLevel;

            $code .= $this->revertAST($bodyNode);

            --$this->indentationLevel;

            $code .= $this->indent() . '}';
        }

        return $code;
    }

    /**
     * Custom method that terminates a statement irregardless.
     */
    private function forceTerminateStatement(string $buffer) : string
    {
        $lastChar = substr($buffer, -1);

        if ($lastChar === ';') {
            return PHP_EOL;
        }

        if ($lastChar === PHP_EOL) {
            return '';
        }

        return ';' . PHP_EOL;
    }

    private function foreach(Node $node) : string
    {
        $code = 'foreach (' . $this->revertAST($node->children['expr']) . ' as ';

        if (isset($node->children['key'])) {
            $code .= $this->revertAST($node->children['key']) . ' => ';
        }

        $code .= $this->revertAST($node->children['value']) . ')';

        $bodyNode = $node->children['stmts'];

        if (
            !$bodyNode instanceof Node
            || $bodyNode->kind !== \ast\AST_STMT_LIST
        ) {
            $bodyNode = $this->createStmtList($bodyNode);
        }

        $code .= ' {' . PHP_EOL;

        ++$this->indentationLevel;

        $code .= $this->revertAST($bodyNode);

        --$this->indentationLevel;

        $code .= $this->indent() . '}';

        return $code;
    }

    private function funcDecl(Node $node) : string
    {
        $code = '';

        if (isset($node->docComment)) {
            $code .= $node->docComment . PHP_EOL;
        }

        $code .= 'function ';

        if ($node->flags === \ast\flags\RETURNS_REF) {
            $code .= '&';
        }

        $code .= $node->name . $this->revertAST($node->children['params']);

        if ($node->children['returnType'] !== null) {
            $code .= ' : ' . $this->revertAST($node->children['returnType']);
        }

        $code .= PHP_EOL . $this->indent() . '{' . PHP_EOL;

        ++$this->indentationLevel;

        $code .= $this->revertAST($node->children['stmts']);

        --$this->indentationLevel;

        $code .= $this->indent() . '}' . PHP_EOL;

        return $code;
    }

    public function getCode(Node $node, bool $fromFile = true) : string
    {
        $code = '';

        if ($fromFile) {
            $code .= '<?php' . PHP_EOL . PHP_EOL;
        }

        $code .= $this->revertAST($node) . PHP_EOL;

        return $code;
    }

    public function global(Node $node) : string
    {
        return 'global ' . $this->revertAST($node->children['var']);
    }

    private function goto(Node $node) : string
    {
        return 'goto ' . $node->children['label'];
    }

    private function groupUse(Node $node) : string
    {
        $code = $this->useAbstract($node) . $node->children['prefix'] . '\{';

        $code .= $this->use($node->children['uses'], false); // a hack to not show 'use ' in block

        $code .= '}';

        return $code;
    }

    private function haltCompiler(Node $node) : string
    {
        return '__halt_compiler()';
    }

    private function if(Node $node) : string
    {
        $code = $this->ifElem($node->children[0], 'if ');

        $childCount = count($node->children);

        for ($i = 1; $i < $childCount; ++$i) {
            $type = ($node->children[$i]->children['cond'] !== null)
                ? ' elseif '
                : ' else';

            $code .= $this->ifElem($node->children[$i], $type);
        }

        return $code;
    }

    private function ifElem(Node $node, string $type) : string
    {
        $code = $type;

        if ($node->children['cond'] !== null) {
            $code .= '(' . $this->revertAST($node->children['cond']) . ')';
        }

        $code .= ' {' . PHP_EOL;

        ++$this->indentationLevel;

        $bodyNode = $node->children['stmts'];

        if (
            !$bodyNode instanceof Node
            || $bodyNode->kind !== \ast\AST_STMT_LIST
        ) {
            $bodyNode = $this->createStmtList($bodyNode);
        }

        $code .= $this->revertAST($bodyNode);

        --$this->indentationLevel;

        $code .= $this->indent() . '}';

        return $code;
    }

    private function includeOrEval(Node $node) : string
    {
        $code = '';
        $arg = $this->revertAST($node->children['expr']);

        switch ($node->flags) {
            case \ast\flags\EXEC_INCLUDE:
                $code .= "include {$arg}";
                break;
            case \ast\flags\EXEC_INCLUDE_ONCE:
                $code .= "include_once {$arg}";
                break;
            case \ast\flags\EXEC_REQUIRE:
                $code .= "require {$arg}";
                break;
            case \ast\flags\EXEC_REQUIRE_ONCE:
                $code .= "require_once {$arg}";
                break;
            case \ast\flags\EXEC_EVAL:
                $code .= "eval({$arg})";
                break;
            default:
                assert(false, "Unknown flag ({$node->flags}) for AST_INCLUDE_OR_EVAL found.");
        }

        return $code;
    }

    private function instanceof(Node $node) : string
    {
        return $this->revertAST($node->children['expr'])
            . ' instanceof '
            . $this->revertAST($node->children['class']);
    }

    /**
     * Custom method to indent statements
     */
    private function indent() : string
    {
        return str_repeat(
            self::INDENTATION_CHAR,
            $this->indentationLevel * self::INDENTATION_SIZE
        );
    }

    private function isset(Node $node) : string
    {
        return 'isset(' . $this->revertAST($node->children['var']) . ')';
    }

    private function label(Node $node) : string
    {
        return $node->children['name'] . ':' . PHP_EOL;
    }

    private function list(Node $node) : string
    {
        return 'list(' . $this->commaSeparatedValues($node, ', ') . ')';
    }

    private function magicConst(Node $node)
    {
        switch ($node->flags) {
            case \ast\flags\MAGIC_LINE:
                return '__LINE__';
            case \ast\flags\MAGIC_FILE:
                return '__FILE__';
            case \ast\flags\MAGIC_DIR:
                return '__DIR__';
            case \ast\flags\MAGIC_TRAIT:
                return '__TRAIT__';
            case \ast\flags\MAGIC_METHOD:
                return '__METHOD__';
            case \ast\flags\MAGIC_FUNCTION:
                return '__FUNCTION__';
            case \ast\flags\MAGIC_NAMESPACE:
                return '__NAMESPACE__';
            case \ast\flags\MAGIC_CLASS:
                return '__CLASS__';
            default:
                assert(false, "Unknown flag ({$node->flags}) for T_MAGIC_CONST found.");
        }
    }

    private function method(Node $node) : string
    {
        $code = '';
        $scope = '';
        $returnsRef = '';

        if (isset($node->docComment)) {
            $code .= $node->docComment . PHP_EOL . $this->indent();
        }

        // (abstract|final)?(public|protected|private)(static)?&?
        switch ($node->flags) {
            case \ast\flags\MODIFIER_PUBLIC:
                $scope = 'public';
                break;
            case \ast\flags\MODIFIER_PUBLIC | \ast\flags\RETURNS_REF:
                $scope = 'public';
                $returnsRef = '&';
                break;
            case \ast\flags\MODIFIER_ABSTRACT | \ast\flags\MODIFIER_PUBLIC:
                $scope = 'abstract public';
                break;
            case \ast\flags\MODIFIER_ABSTRACT | \ast\flags\MODIFIER_PUBLIC | \ast\flags\RETURNS_REF:
                $scope = 'abstract public';
                $returnsRef = '&';
                break;
            case \ast\flags\MODIFIER_FINAL | \ast\flags\MODIFIER_PUBLIC:
                $scope = 'final public';
                break;
            case \ast\flags\MODIFIER_FINAL | \ast\flags\MODIFIER_PUBLIC | \ast\flags\RETURNS_REF:
                $scope = 'final public';
                $returnsRef = '&';
                break;
            case \ast\flags\MODIFIER_PUBLIC | \ast\flags\MODIFIER_STATIC:
                $scope = 'public static';
                break;
            case \ast\flags\MODIFIER_PUBLIC | \ast\flags\MODIFIER_STATIC | \ast\flags\RETURNS_REF:
                $scope = 'public static';
                $returnsRef = '&';
                break;
            case \ast\flags\MODIFIER_ABSTRACT | \ast\flags\MODIFIER_PUBLIC | \ast\flags\MODIFIER_STATIC:
                $scope = 'abstract public static';
                break;
            case \ast\flags\MODIFIER_ABSTRACT | \ast\flags\MODIFIER_PUBLIC | \ast\flags\MODIFIER_STATIC | \ast\flags\RETURNS_REF:
                $scope = 'abstract public static';
                $returnsRef = '&';
                break;
            case \ast\flags\MODIFIER_FINAL | \ast\flags\MODIFIER_PUBLIC | \ast\flags\MODIFIER_STATIC:
                $scope = 'final public static';
                break;
            case \ast\flags\MODIFIER_FINAL | \ast\flags\MODIFIER_PUBLIC | \ast\flags\MODIFIER_STATIC | \ast\flags\RETURNS_REF:
                $scope = 'final public static';
                $returnsRef = '&';
                break;
            case \ast\flags\MODIFIER_PROTECTED:
                $scope = 'protected';
                break;
            case \ast\flags\MODIFIER_PROTECTED | \ast\flags\RETURNS_REF:
                $scope = 'protected';
                $returnsRef = '&';
                break;
            case \ast\flags\MODIFIER_ABSTRACT | \ast\flags\MODIFIER_PROTECTED:
                $scope = 'abstract protected';
                break;
            case \ast\flags\MODIFIER_ABSTRACT | \ast\flags\MODIFIER_PROTECTED | \ast\flags\RETURNS_REF:
                $scope = 'abstract protected';
                $returnsRef = '&';
                break;
            case \ast\flags\MODIFIER_FINAL | \ast\flags\MODIFIER_PROTECTED:
                $scope = 'final protected';
                break;
            case \ast\flags\MODIFIER_FINAL | \ast\flags\MODIFIER_PROTECTED | \ast\flags\RETURNS_REF:
                $scope = 'final protected';
                $returnsRef = '&';
                break;
            case \ast\flags\MODIFIER_PROTECTED | \ast\flags\MODIFIER_STATIC:
                $scope = 'protected static';
                break;
            case \ast\flags\MODIFIER_PROTECTED | \ast\flags\MODIFIER_STATIC | \ast\flags\RETURNS_REF:
                $scope = 'protected static';
                $returnsRef = '&';
                break;
            case \ast\flags\MODIFIER_ABSTRACT | \ast\flags\MODIFIER_PROTECTED | \ast\flags\MODIFIER_STATIC:
                $scope = 'abstract protected static';
                break;
            case \ast\flags\MODIFIER_ABSTRACT | \ast\flags\MODIFIER_PROTECTED | \ast\flags\MODIFIER_STATIC | \ast\flags\RETURNS_REF:
                $scope = 'abstract protected static';
                $returnsRef = '&';
                break;
            case \ast\flags\MODIFIER_FINAL | \ast\flags\MODIFIER_PROTECTED | \ast\flags\MODIFIER_STATIC:
                $scope = 'final protected static';
                break;
            case \ast\flags\MODIFIER_FINAL | \ast\flags\MODIFIER_PROTECTED | \ast\flags\MODIFIER_STATIC | \ast\flags\RETURNS_REF:
                $scope = 'final protected static';
                $returnsRef = '&';
                break;
            case \ast\flags\MODIFIER_PRIVATE:
                $scope = 'private';
                break;
            case \ast\flags\MODIFIER_PRIVATE | \ast\flags\RETURNS_REF:
                $scope = 'private';
                $returnsRef = '&';
                break;
            case \ast\flags\MODIFIER_ABSTRACT | \ast\flags\MODIFIER_PRIVATE:
                $scope = 'abstract private';
                break;
            case \ast\flags\MODIFIER_ABSTRACT | \ast\flags\MODIFIER_PRIVATE | \ast\flags\RETURNS_REF:
                $scope = 'abstract private';
                $returnsRef = '&';
                break;
            case \ast\flags\MODIFIER_FINAL | \ast\flags\MODIFIER_PRIVATE:
                $scope = 'final private';
                break;
            case \ast\flags\MODIFIER_FINAL | \ast\flags\MODIFIER_PRIVATE | \ast\flags\RETURNS_REF:
                $scope = 'final private';
                $returnsRef = '&';
                break;
            case \ast\flags\MODIFIER_PRIVATE | \ast\flags\MODIFIER_STATIC:
                $scope = 'private static';
                break;
            case \ast\flags\MODIFIER_PRIVATE | \ast\flags\MODIFIER_STATIC | \ast\flags\RETURNS_REF:
                $scope = 'private static';
                $returnsRef = '&';
                break;
            case \ast\flags\MODIFIER_ABSTRACT | \ast\flags\MODIFIER_PRIVATE | \ast\flags\MODIFIER_STATIC:
                $scope = 'abstract private static';
                break;
            case \ast\flags\MODIFIER_ABSTRACT | \ast\flags\MODIFIER_PRIVATE | \ast\flags\MODIFIER_STATIC | \ast\flags\RETURNS_REF:
                $scope = 'abstract private static';
                $returnsRef = '&';
                break;
            case \ast\flags\MODIFIER_FINAL | \ast\flags\MODIFIER_PRIVATE | \ast\flags\MODIFIER_STATIC:
                $scope = 'final private static';
                break;
            case \ast\flags\MODIFIER_FINAL | \ast\flags\MODIFIER_PRIVATE | \ast\flags\MODIFIER_STATIC | \ast\flags\RETURNS_REF:
                $scope = 'final private static';
                $returnsRef = '&';
                break;
            default:
                assert(false, "Unknown flag(s) ({$node->flags}) for AST_METHOD found.");
        }

        $code .= $scope
            . ' function '
            . $returnsRef
            . $node->name
            . $this->revertAST($node->children['params']);

        if ($node->children['returnType'] !== null) {
            $code .= ' : ' . $this->revertAST($node->children['returnType']);
        }

        if ($node->children['stmts'] !== null) {
            $code .= PHP_EOL
                . $this->indent()
                . '{'
                . PHP_EOL;

            ++$this->indentationLevel;

            $code .= $this->revertAST($node->children['stmts']);

            --$this->indentationLevel;

            $code .= $this->indent()
                . '}'
                . PHP_EOL;
        }

        return $code;
    }

    private function methodCall(Node $node) : string
    {
        return $this->abstractCall(
            $node->children['expr'],
            $node->children['method'],
            $node->children['args'],
            '->');
    }

    private function methodReference(Node $node) : string
    {
        return $this->revertAST($node->children['class'])
            . '::'
            . $node->children['method'];
    }

    private function name(Node $node) : string
    {
        $code = '';

        switch ($node->flags) {
            case \ast\flags\NAME_FQ:
                if ($node->children['name'][0] != '\\') { // @version php-ast v30 and v35
                    $code .= '\\';
                }
                break;
            case \ast\flags\NAME_NOT_FQ:
                // nothing to do
                break;
            case \ast\flags\NAME_RELATIVE:
                // nothing to do
                break;
            default:
                assert(false, "Unknown flag ({$node->flags}) for AST_PARAM found.");
        }

        $code .= $node->children['name'];

        return $code;
    }

    private function nameList(Node $node) : string
    {
        return $this->commaSeparatedValues($node, $this->inCatchBlock ? ' | ' : ', ');
    }

    private function namespace(Node $node) : string
    {
        $code = 'namespace';

        if ($node->children['name'] !== null) {
            $code .= ' ' . $node->children['name'];
        }

        if ($node->children['stmts'] !== null) {
            $code .= ' {' . PHP_EOL;

            ++$this->indentationLevel;

            $code .=  $this->revertAST($node->children['stmts']);

            --$this->indentationLevel;

            $code .= $this->indent() . '}' . PHP_EOL;
        }

        return $code;
    }

    private function new(Node $node) : string
    {
        $code = 'new ';

        if ($node->children['class']->kind === \ast\AST_CLASS) {
            $code .= $this->class($node->children['class'], $node->children['args'])
                . ';';
        } else {
            $code .= $this->revertAST($node->children['class']);

            if ($node->children['args'] !== null) {
                $code .= $this->revertAST($node->children['args']);
            }
        }

        return $code;
    }

    private function nullableType(Node $node) : string
    {
        return '?' . $this->revertAST($node->children['type']);
    }

    private function param(Node $node) : string
    {
        $code = '';

        if ($node->children['type'] !== null) {
            $code .= $this->revertAST($node->children['type']) . ' ';
        }

        if ($node->flags & \ast\flags\PARAM_REF) {
            $code .= '&';
        }
        if ($node->flags & \ast\flags\PARAM_VARIADIC) {
            $code .= '...';
        }

        $code .= '$' . $node->children['name'];

        if ($node->children['default'] !== null) {
            $code .= ' = ' . $this->revertAST($node->children['default']);
        }

        return $code;
    }

    private function paramList(Node $node) : string
    {
        return '('
            . $this->commaSeparatedValues($node, ', ')
            . ')';
    }

    private function postDec(Node $node) : string
    {
        return $this->revertAst($node->children['var']) . '--';
    }

    private function postInc(Node $node) : string
    {
        return $this->revertAst($node->children['var']) . '++';
    }

    private function preDec(Node $node) : string
    {
        return '--' . $this->revertAst($node->children['var']);
    }

    private function preInc(Node $node) : string
    {
        return '++' . $this->revertAst($node->children['var']);
    }

    private function print(Node $node) : string
    {
        return 'print ' . $this->revertAST($node->children['expr']);
    }

    private function prop(Node $node) : string
    {
        return $this->abstractProp($node->children['expr'], $node->children['prop'], '->');
    }

    private function propDecl(Node $node) : string
    {
        $code = '';
        $scope = '';

        // (public|protected|private)(static)?
        switch ($node->flags) {
            case \ast\flags\MODIFIER_STATIC:
                $scope = 'static';
                break;
            case \ast\flags\MODIFIER_PUBLIC:
                $scope = 'public';
                break;
            case \ast\flags\MODIFIER_PUBLIC | \ast\flags\MODIFIER_STATIC:
                $scope = 'public static';
                break;
            case \ast\flags\MODIFIER_PROTECTED:
                $scope = 'protected';
                break;
            case \ast\flags\MODIFIER_PROTECTED | \ast\flags\MODIFIER_STATIC:
                $scope = 'protected static';
                break;
            case \ast\flags\MODIFIER_PRIVATE:
                $scope = 'private';
                break;
            case \ast\flags\MODIFIER_PRIVATE | \ast\flags\MODIFIER_STATIC:
                $scope = 'private static';
                break;
            default:
                assert(false, "Unknown flag(s) ({$node->flags}) for AST_PROP_DECL found.");
        }

        $code .= $scope . $this->commaSeparatedValues($node, ',');

        return $code;
    }

    private function propElem(Node $node) : string
    {
        $code = '';

        if (isset($node->docComment)) {
            $code .= PHP_EOL . $this->indent() . $node->docComment . PHP_EOL . $this->indent();
        } else {
            $code .= ' ';
        }

        $code .= '$' . $node->children['name'];

        if ($node->children['default'] !== null) {
            $code .= ' = ' . $this->revertAST($node->children['default']);
        }

        return $code;
    }

    private function ref(Node $node) : string
    {
        return '&' . $this->revertAST($node->children['var']);
    }

    private function return(Node $node) : string
    {
        $code = 'return';

        if ($node->children['expr'] !== null) {
            $code .= ' ' . $this->revertAST($node->children['expr']);
        }

        return $code;
    }

    private function shellExec(Node $node) : string
    {
        // ugly hack to remove double quotes
        $expr = substr($this->revertAST($node->children['expr']), 1, -1);
        $code = '`' . $expr . '`';

        return $code;
    }

    private function sanitiseString(string $string) : string
    {
        /*return strtr(
            $string,
            ["\n" => '\n', '"' => '\"']
		);*/

        return strtr(
            $string,
            ['$' => '\\$', '\\' => '\\\\', "\n" => '\n', '"' => '\"']
		);
    }

    private function static(Node $node) : string
    {
        $code = 'static ';
        $code .= $this->revertAST($node->children['var']);

        if ($node->children['default'] !== null) {
            $code .= ' = ' . $this->revertAST($node->children['default']);
        }

        return $code;
    }

    private function staticCall(Node $node) : string
    {
        return $this->abstractCall(
            $node->children['class'],
            $node->children['method'],
            $node->children['args'],
            '::');
    }

    private function staticProp(Node $node) : string
    {
        return $this->abstractProp($node->children['class'], $node->children['prop'], '::$');
    }

    private function stmtList(Node $node) : string
    {
        $code = '';

        foreach ($node->children as $key => $child) {
            if ($child === null) {
                continue;
            }

            if (!$child instanceof Node || $child->kind !== \ast\AST_STMT_LIST) {
                $code .= $this->indent();
            }

            $code .= $this->revertAST($child);

            if (
                $child instanceof Node
                && (
                    $child->kind === \ast\AST_VAR
                    || $child->kind === \ast\AST_PROP
                    || $child->kind === \ast\AST_STATIC_PROP
                    || $child->kind === \ast\AST_GLOBAL
                    || $child->kind === \ast\AST_ASSIGN
                    || $child->kind === \ast\AST_ASSIGN_OP
                    || $child->kind === \ast\AST_CLOSURE
                    || $child->kind === \ast\AST_GROUP_USE
                )
            ) {
                $code .= $this->forceTerminateStatement($code);
            } else {
                $code .= $this->terminateStatement($code);
            }
        }

        return $code;
    }

    private function switch(Node $node) : string
    {
        $code = 'switch ('
            . $this->revertAST($node->children['cond'])
            . ') {'
            . PHP_EOL;

        ++$this->indentationLevel;

        $code .= $this->revertAST($node->children['stmts']);

        --$this->indentationLevel;

        $code .= $this->indent() . '}';

        return $code;
    }

    private function switchCase(Node $node) : string
    {
        $code = $this->indent();

        if ($node->children['cond'] === null) {
            $code .= 'default:';
        } else {
            $code .= 'case ' . $this->revertAST($node->children['cond']) . ':';
        }

        $code .= PHP_EOL;

        ++$this->indentationLevel;

        $code .= $this->revertAST($node->children['stmts']);

        --$this->indentationLevel;

        return $code;
    }

    private function switchList(Node $node) : string
    {
        $code = '';

        foreach ($node->children as $child) {
            $code .= $this->revertAST($child);
        }

        return $code;
    }

    /**
     * Custom method that ensures statements are terminated.
     */
    private function terminateStatement(string $buffer) : string
    {
        $lastChar = substr($buffer, -1);

        if ($lastChar !== '}') {
            return $this->forceTerminateStatement($buffer);
        }

        return PHP_EOL;
    }

    private function throw(Node $node) : string
    {
        return 'throw ' . $this->revertAST($node->children['expr']);
    }

    private function traitAdaptations(Node $node) : string
    {
        $code = '';

        foreach ($node->children as $child) {
            $code .= $this->indent()
                . $this->revertAST($child)
                . ';'
                . PHP_EOL;
        }

        return $code;
    }

    private function traitAlias(Node $node) : string
    {
        $code = $this->revertAST($node->children['method']) . ' as ';

        switch ($node->flags) {
            case \ast\flags\MODIFIER_PUBLIC:
                $code .= 'public ';
                break;
            case \ast\flags\MODIFIER_PROTECTED:
                $code .= 'protected ';
                break;
            case \ast\flags\MODIFIER_PRIVATE:
                $code .= 'private ';
                break;
            default:
                // if there's no flag, then the value will be 0
                // assert(false, "Unknown flag ({$node->flags}) for AST_TRAIT_ALIAS found.");
        }

        $code .= $node->children['alias'];

        return $code;
    }

    private function traitPrecedence(Node $node) : string
    {
        return $this->revertAST($node->children['method'])
            . ' insteadof '
            . $this->revertAST($node->children['insteadof']);
    }

    private function try(Node $node) : string
    {
        $code = 'try {' . PHP_EOL;

        ++$this->indentationLevel;

        $code .= $this->revertAST($node->children['try']);

        --$this->indentationLevel;

        $code .= $this->indent() . '}';

        $code .= $this->revertAST($node->children['catches']);

        if ($node->children['finally'] !== null) {
            $code .= ' finally {' . PHP_EOL;

            ++$this->indentationLevel;

            $code .= $this->revertAST($node->children['finally']);

            -- $this->indentationLevel;

            $code .= $this->indent() . '}';
        }

        return $code;
    }

    private function type(Node $node) : string
    {
        switch ($node->flags) {
            case \ast\flags\TYPE_ARRAY:
                return 'array';
            case \ast\flags\TYPE_CALLABLE:
                return 'callable';
            case \ast\flags\TYPE_LONG:
                return 'int';
            case \ast\flags\TYPE_STRING:
                return 'string';
            case \ast\flags\TYPE_BOOL:
                return 'bool';
            case \ast\flags\TYPE_ITERABLE:
                return 'iterable';
            case \ast\flags\TYPE_DOUBLE:
                return 'float';
            case \ast\flags\TYPE_VOID:
                return 'void';
            default:
                assert(false, "Unknown flag ({$node->flags}) for AST_TYPE found.");
        }
    }

    private function unaryOp(Node $node) : string
    {
        $code = '';

        switch ($node->flags) {
            case \ast\flags\UNARY_BOOL_NOT:
                $code .= '!';
                break;
            case \ast\flags\UNARY_BITWISE_NOT:
                $code .= '~';
                break;
            case \ast\flags\UNARY_SILENCE:
                $code .= '@';
                break;
            case \ast\flags\UNARY_PLUS:
                $code .= '+';
                break;
            case \ast\flags\UNARY_MINUS:
                $code .= '-';
                break;
            default:
                assert(false, "Unknown flag ({$node->flags}) for AST_UNARY_OP found.");
        }

        $code .= $this->revertAST($node->children['expr']);

        return $code;
    }

    private function unpack(Node $node) : string
    {
        return '...' . $this->revertAST($node->children['expr']);
    }

    private function unset(Node $node) : string
    {
        return 'unset('. $this->revertAST($node->children['var']) . ')';
    }

    private function use(Node $node, $setUse = true) : string
    {
        return $this->useAbstract($node, $setUse)
            . $this->commaSeparatedValues($node, ', ');
    }

    /**
     * Custom method to encapsulate common logic between use() and groupUse().
     */
    private function useAbstract(Node $node, $setUse = true) : string
    {
        $code = '';

        if ($setUse) {
            $code .= 'use ';
        }

        switch ($node->flags) {
            case \ast\flags\USE_CONST:
                $code .= 'const ';
                break;
            case \ast\flags\USE_FUNCTION:
                $code .= 'function ';
                break;
            case \ast\flags\USE_NORMAL:
                // nothing to do
                break;
            case 0:
                // denotes no flags are set
                break;
            default:
                assert(false, "Unknown flag ({$node->flags}) for AST_USE or AST_GROUP_USE found.");
        }

        return $code;
    }

    private function useElem(Node $node) : string
    {
        $code = '';

        switch ($node->flags) {
            case \ast\flags\USE_CONST:
                $code .= 'const ';
                break;
            case \ast\flags\USE_FUNCTION:
                $code .= 'function ';
                break;
            case \ast\flags\USE_NORMAL:
                // nothing to do
                break;
            case 0:
                // denotes no flags are set
                break;
            default:
                assert(false, "Unknown flag ({$node->flags}) for AST_USE_ELEM found.");
        }

        $code .= $node->children['name'];

        if ($node->children['alias'] !== null) {
            $code .= ' as ' . $node->children['alias'];
        }

        return $code;
    }

    private function useTrait(Node $node) : string
    {
        $code = 'use ';

        $code .= $this->commaSeparatedValues($node->children['traits'], ', ');

        if ($node->children['adaptations'] !== null) {
            $code .= ' {' . PHP_EOL;

            ++$this->indentationLevel;

            $code .= $this->revertAST($node->children['adaptations']);

            --$this->indentationLevel;

            $code .= $this->indent() . '}' . PHP_EOL;
        }

        return $code;
    }

    private function var(Node $node) : string
    {
        $code = '$';

        if (!$node->children['name'] instanceof Node) {
            $varName = $node->children['name'];

            if (preg_match('~^[_a-z][_a-z0-9]*$~i', $varName)) {
                return $code . $varName;
            }

            return $code . "{'" . $varName . "'}";
        }

        $code .= '{' . $this->revertAST($node->children['name']) . '}';

        // special case check for something like `${$a = $b};`
        if ($node->children['name']->kind === \ast\AST_ASSIGN) {
            $code .= $this->forceTerminateStatement($code);
        }

        return $code;
    }

    private function while(Node $node) : string
    {
        $code = 'while (' . $this->revertAST($node->children['cond']) . ')';

        $bodyNode = $node->children['stmts'];

        if ($bodyNode !== null && (!$bodyNode instanceof Node || $bodyNode->children !== [])) {
            if (
                !$bodyNode instanceof Node
                || $bodyNode->kind !== \ast\AST_STMT_LIST
            ) {
                $bodyNode = $this->createStmtList($bodyNode);
            }

            $code .= ' {' . PHP_EOL;

            ++$this->indentationLevel;

            $code .= $this->revertAST($bodyNode);

            --$this->indentationLevel;

            $code .= $this->indent() . '}';
        }

        return $code;
    }

    private function yield(Node $node) : string
    {
        $code = 'yield';
        if ($node->children['value'] !== null) {
            $code .= ' ';
            if ($node->children['key'] !== null) {
                $code .= $this->revertAST($node->children['key']) . ' => ';
            }
            $code .= $this->revertAST($node->children['value']);
        }
        return $code;
    }

    private function yieldFrom(Node $node) : string
    {
        return 'yield from ' . $this->revertAST($node->children['expr']);
    }
}
