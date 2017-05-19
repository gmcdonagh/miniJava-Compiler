package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class SubtractionOperator extends OperatorType {

	public SubtractionOperator(TypeKind type, SourcePosition posn) {
		super(null, null);
		super.binaryOperand1[0] = TypeKind.INT;
		super.binaryOperand2[0] = TypeKind.INT;
		super.binaryResult[0] = TypeKind.INT;
		super.unaryOperand[0] = TypeKind.INT;
		super.unaryResult[0] = TypeKind.INT;
	}
}
