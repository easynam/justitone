// Generated from JI.g4 by ANTLR 4.7.2
package justitone.antlr;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link JIParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface JIVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link JIParser#integer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInteger(JIParser.IntegerContext ctx);
	/**
	 * Visit a parse tree produced by {@link JIParser#fraction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFraction(JIParser.FractionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code note}
	 * labeled alternative in {@link JIParser#event}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNote(JIParser.NoteContext ctx);
	/**
	 * Visit a parse tree produced by the {@code modulation}
	 * labeled alternative in {@link JIParser#event}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModulation(JIParser.ModulationContext ctx);
	/**
	 * Visit a parse tree produced by {@link JIParser#sequence}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSequence(JIParser.SequenceContext ctx);
}