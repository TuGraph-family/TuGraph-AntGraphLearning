package com.alipay.alps.flatv3.antlr4;

// Generated from Filter.g4 by ANTLR 4.5.3
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link FilterParser}.
 */
public interface FilterListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link FilterParser#start}.
	 * @param ctx the parse tree
	 */
	void enterStart(FilterParser.StartContext ctx);
	/**
	 * Exit a parse tree produced by {@link FilterParser#start}.
	 * @param ctx the parse tree
	 */
	void exitStart(FilterParser.StartContext ctx);
	/**
	 * Enter a parse tree produced by the {@code LiteralExp}
	 * labeled alternative in {@link FilterParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterLiteralExp(FilterParser.LiteralExpContext ctx);
	/**
	 * Exit a parse tree produced by the {@code LiteralExp}
	 * labeled alternative in {@link FilterParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitLiteralExp(FilterParser.LiteralExpContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ColumnExp}
	 * labeled alternative in {@link FilterParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterColumnExp(FilterParser.ColumnExpContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ColumnExp}
	 * labeled alternative in {@link FilterParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitColumnExp(FilterParser.ColumnExpContext ctx);
	/**
	 * Enter a parse tree produced by the {@code AndExp}
	 * labeled alternative in {@link FilterParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterAndExp(FilterParser.AndExpContext ctx);
	/**
	 * Exit a parse tree produced by the {@code AndExp}
	 * labeled alternative in {@link FilterParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitAndExp(FilterParser.AndExpContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CompareExp}
	 * labeled alternative in {@link FilterParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterCompareExp(FilterParser.CompareExpContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CompareExp}
	 * labeled alternative in {@link FilterParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitCompareExp(FilterParser.CompareExpContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ParExp}
	 * labeled alternative in {@link FilterParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterParExp(FilterParser.ParExpContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ParExp}
	 * labeled alternative in {@link FilterParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitParExp(FilterParser.ParExpContext ctx);
	/**
	 * Enter a parse tree produced by the {@code OrExp}
	 * labeled alternative in {@link FilterParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterOrExp(FilterParser.OrExpContext ctx);
	/**
	 * Exit a parse tree produced by the {@code OrExp}
	 * labeled alternative in {@link FilterParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitOrExp(FilterParser.OrExpContext ctx);
	/**
	 * Enter a parse tree produced by the {@code PlusMinusExp}
	 * labeled alternative in {@link FilterParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterPlusMinusExp(FilterParser.PlusMinusExpContext ctx);
	/**
	 * Exit a parse tree produced by the {@code PlusMinusExp}
	 * labeled alternative in {@link FilterParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitPlusMinusExp(FilterParser.PlusMinusExpContext ctx);
	/**
	 * Enter a parse tree produced by the {@code UnaryExp}
	 * labeled alternative in {@link FilterParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterUnaryExp(FilterParser.UnaryExpContext ctx);
	/**
	 * Exit a parse tree produced by the {@code UnaryExp}
	 * labeled alternative in {@link FilterParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitUnaryExp(FilterParser.UnaryExpContext ctx);
	/**
	 * Enter a parse tree produced by the {@code StarDivExp}
	 * labeled alternative in {@link FilterParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterStarDivExp(FilterParser.StarDivExpContext ctx);
	/**
	 * Exit a parse tree produced by the {@code StarDivExp}
	 * labeled alternative in {@link FilterParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitStarDivExp(FilterParser.StarDivExpContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CategoryExp}
	 * labeled alternative in {@link FilterParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterCategoryExp(FilterParser.CategoryExpContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CategoryExp}
	 * labeled alternative in {@link FilterParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitCategoryExp(FilterParser.CategoryExpContext ctx);
	/**
	 * Enter a parse tree produced by {@link FilterParser#keyword}.
	 * @param ctx the parse tree
	 */
	void enterKeyword(FilterParser.KeywordContext ctx);
	/**
	 * Exit a parse tree produced by {@link FilterParser#keyword}.
	 * @param ctx the parse tree
	 */
	void exitKeyword(FilterParser.KeywordContext ctx);
	/**
	 * Enter a parse tree produced by {@link FilterParser#any_name}.
	 * @param ctx the parse tree
	 */
	void enterAny_name(FilterParser.Any_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link FilterParser#any_name}.
	 * @param ctx the parse tree
	 */
	void exitAny_name(FilterParser.Any_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link FilterParser#column_name}.
	 * @param ctx the parse tree
	 */
	void enterColumn_name(FilterParser.Column_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link FilterParser#column_name}.
	 * @param ctx the parse tree
	 */
	void exitColumn_name(FilterParser.Column_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link FilterParser#unary_operator}.
	 * @param ctx the parse tree
	 */
	void enterUnary_operator(FilterParser.Unary_operatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link FilterParser#unary_operator}.
	 * @param ctx the parse tree
	 */
	void exitUnary_operator(FilterParser.Unary_operatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link FilterParser#literal_value}.
	 * @param ctx the parse tree
	 */
	void enterLiteral_value(FilterParser.Literal_valueContext ctx);
	/**
	 * Exit a parse tree produced by {@link FilterParser#literal_value}.
	 * @param ctx the parse tree
	 */
	void exitLiteral_value(FilterParser.Literal_valueContext ctx);
}