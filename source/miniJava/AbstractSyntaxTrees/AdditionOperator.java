package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class AdditionOperator extends OperatorType {

	public AdditionOperator(TypeKind type, SourcePosition posn) {
		super(null, null);
		super.binaryOperand1[0] = TypeKind.INT;
		super.binaryOperand2[0] = TypeKind.INT;
		super.binaryResult[0] = TypeKind.INT;
	}
	

}
