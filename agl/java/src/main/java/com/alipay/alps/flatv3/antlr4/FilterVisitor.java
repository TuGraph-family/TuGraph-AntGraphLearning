package com.alipay.alps.flatv3.antlr4;

// Generated from Filter.g4 by ANTLR 4.5.3
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link FilterParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface FilterVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link FilterParser#start}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStart(FilterParser.StartContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LiteralExp}
	 * labeled alternative in {@link FilterParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteralExp(FilterParser.LiteralExpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PlusExp}
	 * labeled alternative in {@link FilterParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPlusExp(FilterParser.PlusExpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ColumnExp}
	 * labeled alternative in {@link FilterParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumnExp(FilterParser.ColumnExpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LtExp}
	 * labeled alternative in {@link FilterParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLtExp(FilterParser.LtExpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code EqExp}
	 * labeled alternative in {@link FilterParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEqExp(FilterParser.EqExpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code AndExp}
	 * labeled alternative in {@link FilterParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAndExp(FilterParser.AndExpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ParExp}
	 * labeled alternative in {@link FilterParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParExp(FilterParser.ParExpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code OrExp}
	 * labeled alternative in {@link FilterParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrExp(FilterParser.OrExpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code UnaryExp}
	 * labeled alternative in {@link FilterParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryExp(FilterParser.UnaryExpContext ctx);
	/**
	 * Visit a parse tree produced by {@link FilterParser#keyword}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKeyword(FilterParser.KeywordContext ctx);
	/**
	 * Visit a parse tree produced by {@link FilterParser#any_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAny_name(FilterParser.Any_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link FilterParser#column_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumn_name(FilterParser.Column_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link FilterParser#unary_operator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnary_operator(FilterParser.Unary_operatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link FilterParser#literal_value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteral_value(FilterParser.Literal_valueContext ctx);
}