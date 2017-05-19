package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ComplementOperator extends OperatorType {

	public ComplementOperator(TypeKind type, SourcePosition posn) {
		super(null, null);
		super.unaryOperand[0] = TypeKind.BOOLEAN;
		super.unaryResult[0] = TypeKind.BOOLEAN;
	}
	

}
