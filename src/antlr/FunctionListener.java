// Generated from src/antlr/Function.g4 by ANTLR 4.0.1-SNAPSHOT

	package antlr;


  import java.util.Stack;

import org.antlr.v4.runtime.tree.*;
import org.antlr.v4.runtime.Token;

public interface FunctionListener extends ParseTreeListener<Token> {
	void enterPre_else(FunctionParser.Pre_elseContext ctx);
	void exitPre_else(FunctionParser.Pre_elseContext ctx);
	void enterDeclarator(FunctionParser.DeclaratorContext ctx);
	void exitDeclarator(FunctionParser.DeclaratorContext ctx);
	void enterTemplate_decl_start(FunctionParser.Template_decl_startContext ctx);
	void exitTemplate_decl_start(FunctionParser.Template_decl_startContext ctx);
	void enterFunction_argument_list(FunctionParser.Function_argument_listContext ctx);
	void exitFunction_argument_list(FunctionParser.Function_argument_listContext ctx);
	void enterType_suffix(FunctionParser.Type_suffixContext ctx);
	void exitType_suffix(FunctionParser.Type_suffixContext ctx);
	void enterNo_squares_or_semicolon(FunctionParser.No_squares_or_semicolonContext ctx);
	void exitNo_squares_or_semicolon(FunctionParser.No_squares_or_semicolonContext ctx);
	void enterFor_statement(FunctionParser.For_statementContext ctx);
	void exitFor_statement(FunctionParser.For_statementContext ctx);
	void enterCast_expression(FunctionParser.Cast_expressionContext ctx);
	void exitCast_expression(FunctionParser.Cast_expressionContext ctx);
	void enterNo_angle_brackets_or_brackets(FunctionParser.No_angle_brackets_or_bracketsContext ctx);
	void exitNo_angle_brackets_or_brackets(FunctionParser.No_angle_brackets_or_bracketsContext ctx);
	void enterEquality_expression(FunctionParser.Equality_expressionContext ctx);
	void exitEquality_expression(FunctionParser.Equality_expressionContext ctx);
	void enterNo_comma_or_semicolon(FunctionParser.No_comma_or_semicolonContext ctx);
	void exitNo_comma_or_semicolon(FunctionParser.No_comma_or_semicolonContext ctx);
	void enterTry_statement(FunctionParser.Try_statementContext ctx);
	void exitTry_statement(FunctionParser.Try_statementContext ctx);
	void enterNumber(FunctionParser.NumberContext ctx);
	void exitNumber(FunctionParser.NumberContext ctx);
	void enterBase_type(FunctionParser.Base_typeContext ctx);
	void exitBase_type(FunctionParser.Base_typeContext ctx);
	void enterPtrMemberAccess(FunctionParser.PtrMemberAccessContext ctx);
	void exitPtrMemberAccess(FunctionParser.PtrMemberAccessContext ctx);
	void enterPre_opener(FunctionParser.Pre_openerContext ctx);
	void exitPre_opener(FunctionParser.Pre_openerContext ctx);
	void enterNo_squares(FunctionParser.No_squaresContext ctx);
	void exitNo_squares(FunctionParser.No_squaresContext ctx);
	void enterShift_expression(FunctionParser.Shift_expressionContext ctx);
	void exitShift_expression(FunctionParser.Shift_expressionContext ctx);
	void enterDeclByClass(FunctionParser.DeclByClassContext ctx);
	void exitDeclByClass(FunctionParser.DeclByClassContext ctx);
	void enterType_name(FunctionParser.Type_nameContext ctx);
	void exitType_name(FunctionParser.Type_nameContext ctx);
	void enterBreakStatement(FunctionParser.BreakStatementContext ctx);
	void exitBreakStatement(FunctionParser.BreakStatementContext ctx);
	void enterFuncCall(FunctionParser.FuncCallContext ctx);
	void exitFuncCall(FunctionParser.FuncCallContext ctx);
	void enterInclusive_or_expression(FunctionParser.Inclusive_or_expressionContext ctx);
	void exitInclusive_or_expression(FunctionParser.Inclusive_or_expressionContext ctx);
	void enterBase_classes(FunctionParser.Base_classesContext ctx);
	void exitBase_classes(FunctionParser.Base_classesContext ctx);
	void enterIncDecOp(FunctionParser.IncDecOpContext ctx);
	void exitIncDecOp(FunctionParser.IncDecOpContext ctx);
	void enterPre_closer(FunctionParser.Pre_closerContext ctx);
	void exitPre_closer(FunctionParser.Pre_closerContext ctx);
	void enterRelational_expression(FunctionParser.Relational_expressionContext ctx);
	void exitRelational_expression(FunctionParser.Relational_expressionContext ctx);
	void enterClass_name(FunctionParser.Class_nameContext ctx);
	void exitClass_name(FunctionParser.Class_nameContext ctx);
	void enterParam_decl_specifiers(FunctionParser.Param_decl_specifiersContext ctx);
	void exitParam_decl_specifiers(FunctionParser.Param_decl_specifiersContext ctx);
	void enterParam_type(FunctionParser.Param_typeContext ctx);
	void exitParam_type(FunctionParser.Param_typeContext ctx);
	void enterFunction_argument(FunctionParser.Function_argumentContext ctx);
	void exitFunction_argument(FunctionParser.Function_argumentContext ctx);
	void enterIf_statement(FunctionParser.If_statementContext ctx);
	void exitIf_statement(FunctionParser.If_statementContext ctx);
	void enterWater(FunctionParser.WaterContext ctx);
	void exitWater(FunctionParser.WaterContext ctx);
	void enterClosing_curly(FunctionParser.Closing_curlyContext ctx);
	void exitClosing_curly(FunctionParser.Closing_curlyContext ctx);
	void enterFor_init_statement(FunctionParser.For_init_statementContext ctx);
	void exitFor_init_statement(FunctionParser.For_init_statementContext ctx);
	void enterOr_expression(FunctionParser.Or_expressionContext ctx);
	void exitOr_expression(FunctionParser.Or_expressionContext ctx);
	void enterRelational_operator(FunctionParser.Relational_operatorContext ctx);
	void exitRelational_operator(FunctionParser.Relational_operatorContext ctx);
	void enterDo_statement1(FunctionParser.Do_statement1Context ctx);
	void exitDo_statement1(FunctionParser.Do_statement1Context ctx);
	void enterOperator(FunctionParser.OperatorContext ctx);
	void exitOperator(FunctionParser.OperatorContext ctx);
	void enterInc_dec(FunctionParser.Inc_decContext ctx);
	void exitInc_dec(FunctionParser.Inc_decContext ctx);
	void enterConditional_expression(FunctionParser.Conditional_expressionContext ctx);
	void exitConditional_expression(FunctionParser.Conditional_expressionContext ctx);
	void enterInitDeclSimple(FunctionParser.InitDeclSimpleContext ctx);
	void exitInitDeclSimple(FunctionParser.InitDeclSimpleContext ctx);
	void enterOpening_curly(FunctionParser.Opening_curlyContext ctx);
	void exitOpening_curly(FunctionParser.Opening_curlyContext ctx);
	void enterPrimary_expression(FunctionParser.Primary_expressionContext ctx);
	void exitPrimary_expression(FunctionParser.Primary_expressionContext ctx);
	void enterGotoStatement(FunctionParser.GotoStatementContext ctx);
	void exitGotoStatement(FunctionParser.GotoStatementContext ctx);
	void enterNo_brackets(FunctionParser.No_bracketsContext ctx);
	void exitNo_brackets(FunctionParser.No_bracketsContext ctx);
	void enterBit_and_expression(FunctionParser.Bit_and_expressionContext ctx);
	void exitBit_and_expression(FunctionParser.Bit_and_expressionContext ctx);
	void enterAssign_water(FunctionParser.Assign_waterContext ctx);
	void exitAssign_water(FunctionParser.Assign_waterContext ctx);
	void enterCast_target(FunctionParser.Cast_targetContext ctx);
	void exitCast_target(FunctionParser.Cast_targetContext ctx);
	void enterInitializer(FunctionParser.InitializerContext ctx);
	void exitInitializer(FunctionParser.InitializerContext ctx);
	void enterFunction_decl_specifiers(FunctionParser.Function_decl_specifiersContext ctx);
	void exitFunction_decl_specifiers(FunctionParser.Function_decl_specifiersContext ctx);
	void enterReturnStatement(FunctionParser.ReturnStatementContext ctx);
	void exitReturnStatement(FunctionParser.ReturnStatementContext ctx);
	void enterMultiplicative_expression(FunctionParser.Multiplicative_expressionContext ctx);
	void exitMultiplicative_expression(FunctionParser.Multiplicative_expressionContext ctx);
	void enterAssign_expr(FunctionParser.Assign_exprContext ctx);
	void exitAssign_expr(FunctionParser.Assign_exprContext ctx);
	void enterExpr_statement(FunctionParser.Expr_statementContext ctx);
	void exitExpr_statement(FunctionParser.Expr_statementContext ctx);
	void enterMemberAccess(FunctionParser.MemberAccessContext ctx);
	void exitMemberAccess(FunctionParser.MemberAccessContext ctx);
	void enterBase_class(FunctionParser.Base_classContext ctx);
	void exitBase_class(FunctionParser.Base_classContext ctx);
	void enterParameter_name(FunctionParser.Parameter_nameContext ctx);
	void exitParameter_name(FunctionParser.Parameter_nameContext ctx);
	void enterAccess_specifier(FunctionParser.Access_specifierContext ctx);
	void exitAccess_specifier(FunctionParser.Access_specifierContext ctx);
	void enterAssign_water_l2(FunctionParser.Assign_water_l2Context ctx);
	void exitAssign_water_l2(FunctionParser.Assign_water_l2Context ctx);
	void enterInit_declarator_list(FunctionParser.Init_declarator_listContext ctx);
	void exitInit_declarator_list(FunctionParser.Init_declarator_listContext ctx);
	void enterCondition(FunctionParser.ConditionContext ctx);
	void exitCondition(FunctionParser.ConditionContext ctx);
	void enterArrayIndexing(FunctionParser.ArrayIndexingContext ctx);
	void exitArrayIndexing(FunctionParser.ArrayIndexingContext ctx);
	void enterCatch_statement(FunctionParser.Catch_statementContext ctx);
	void exitCatch_statement(FunctionParser.Catch_statementContext ctx);
	void enterLabel(FunctionParser.LabelContext ctx);
	void exitLabel(FunctionParser.LabelContext ctx);
	void enterExclusive_or_expression(FunctionParser.Exclusive_or_expressionContext ctx);
	void exitExclusive_or_expression(FunctionParser.Exclusive_or_expressionContext ctx);
	void enterStatement(FunctionParser.StatementContext ctx);
	void exitStatement(FunctionParser.StatementContext ctx);
	void enterInitDeclWithCall(FunctionParser.InitDeclWithCallContext ctx);
	void exitInitDeclWithCall(FunctionParser.InitDeclWithCallContext ctx);
	void enterParam_type_id(FunctionParser.Param_type_idContext ctx);
	void exitParam_type_id(FunctionParser.Param_type_idContext ctx);
	void enterAdditive_expression(FunctionParser.Additive_expressionContext ctx);
	void exitAdditive_expression(FunctionParser.Additive_expressionContext ctx);
	void enterInitializer_list(FunctionParser.Initializer_listContext ctx);
	void exitInitializer_list(FunctionParser.Initializer_listContext ctx);
	void enterUnary_operator(FunctionParser.Unary_operatorContext ctx);
	void exitUnary_operator(FunctionParser.Unary_operatorContext ctx);
	void enterElse_statement(FunctionParser.Else_statementContext ctx);
	void exitElse_statement(FunctionParser.Else_statementContext ctx);
	void enterSwitch_statement(FunctionParser.Switch_statementContext ctx);
	void exitSwitch_statement(FunctionParser.Switch_statementContext ctx);
	void enterDo_statement(FunctionParser.Do_statementContext ctx);
	void exitDo_statement(FunctionParser.Do_statementContext ctx);
	void enterNo_brackets_curlies_or_squares(FunctionParser.No_brackets_curlies_or_squaresContext ctx);
	void exitNo_brackets_curlies_or_squares(FunctionParser.No_brackets_curlies_or_squaresContext ctx);
	void enterWhile_statement(FunctionParser.While_statementContext ctx);
	void exitWhile_statement(FunctionParser.While_statementContext ctx);
	void enterIdentifier(FunctionParser.IdentifierContext ctx);
	void exitIdentifier(FunctionParser.IdentifierContext ctx);
	void enterPrimaryOnly(FunctionParser.PrimaryOnlyContext ctx);
	void exitPrimaryOnly(FunctionParser.PrimaryOnlyContext ctx);
	void enterNo_brackets_or_semicolon(FunctionParser.No_brackets_or_semicolonContext ctx);
	void exitNo_brackets_or_semicolon(FunctionParser.No_brackets_or_semicolonContext ctx);
	void enterNo_curlies(FunctionParser.No_curliesContext ctx);
	void exitNo_curlies(FunctionParser.No_curliesContext ctx);
	void enterEquality_operator(FunctionParser.Equality_operatorContext ctx);
	void exitEquality_operator(FunctionParser.Equality_operatorContext ctx);
	void enterExpr(FunctionParser.ExprContext ctx);
	void exitExpr(FunctionParser.ExprContext ctx);
	void enterParam_type_list(FunctionParser.Param_type_listContext ctx);
	void exitParam_type_list(FunctionParser.Param_type_listContext ctx);
	void enterSimple_decl(FunctionParser.Simple_declContext ctx);
	void exitSimple_decl(FunctionParser.Simple_declContext ctx);
	void enterBlock_starter(FunctionParser.Block_starterContext ctx);
	void exitBlock_starter(FunctionParser.Block_starterContext ctx);
	void enterAssignment_operator(FunctionParser.Assignment_operatorContext ctx);
	void exitAssignment_operator(FunctionParser.Assignment_operatorContext ctx);
	void enterStatements(FunctionParser.StatementsContext ctx);
	void exitStatements(FunctionParser.StatementsContext ctx);
	void enterUnary_expression(FunctionParser.Unary_expressionContext ctx);
	void exitUnary_expression(FunctionParser.Unary_expressionContext ctx);
	void enterPtrs(FunctionParser.PtrsContext ctx);
	void exitPtrs(FunctionParser.PtrsContext ctx);
	void enterInitDeclWithAssign(FunctionParser.InitDeclWithAssignContext ctx);
	void exitInitDeclWithAssign(FunctionParser.InitDeclWithAssignContext ctx);
	void enterConstant(FunctionParser.ConstantContext ctx);
	void exitConstant(FunctionParser.ConstantContext ctx);
	void enterContinueStatement(FunctionParser.ContinueStatementContext ctx);
	void exitContinueStatement(FunctionParser.ContinueStatementContext ctx);
	void enterPtr_operator(FunctionParser.Ptr_operatorContext ctx);
	void exitPtr_operator(FunctionParser.Ptr_operatorContext ctx);
	void enterClass_def(FunctionParser.Class_defContext ctx);
	void exitClass_def(FunctionParser.Class_defContext ctx);
	void enterAnd_expression(FunctionParser.And_expressionContext ctx);
	void exitAnd_expression(FunctionParser.And_expressionContext ctx);
	void enterTemplate_param_list(FunctionParser.Template_param_listContext ctx);
	void exitTemplate_param_list(FunctionParser.Template_param_listContext ctx);
	void enterDeclByType(FunctionParser.DeclByTypeContext ctx);
	void exitDeclByType(FunctionParser.DeclByTypeContext ctx);
}