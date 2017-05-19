package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class AndOperator extends OperatorType {

	public AndOperator(TypeKind type, SourcePosition posn) {
		super(null, null);
		super.binaryOperand1[0] = TypeKind.BOOLEAN;
		super.binaryOperand2[0] = TypeKind.BOOLEAN;
		super.binaryResult[0] = TypeKind.BOOLEAN;

	}
	

}
