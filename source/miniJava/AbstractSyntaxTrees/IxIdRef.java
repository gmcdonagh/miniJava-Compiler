/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class IxIdRef extends BaseRef {
	
	public IxIdRef(Identifier id, Expression expr, SourcePosition posn){
		super(posn);
		this.id = id;
		this.indexExpr = expr;
	}

	public <A,R> R visit(Visitor<A,R> v, A o){
		return v.visitIxIdRef(this, o);
	}
	
	public Identifier id;
	public Expression indexExpr;
}
