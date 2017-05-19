package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class EqualOperator extends OperatorType {

	public EqualOperator(TypeKind type, SourcePosition posn) {
		super(null, null);
		super.binaryOperand1[0] = TypeKind.INT;
		super.binaryOperand2[0] = TypeKind.INT;
		super.binaryResult[0] = TypeKind.BOOLEAN;
		
		super.binaryOperand1[1] = TypeKind.BOOLEAN;
		super.binaryOperand2[1] = TypeKind.BOOLEAN;
		super.binaryResult[1] = TypeKind.BOOLEAN;
		
		super.binaryOperand1[2] = TypeKind.CLASS;
		super.binaryOperand2[2] = TypeKind.CLASS;
		super.binaryResult[2] = TypeKind.BOOLEAN;

		super.binaryOperand1[3] = TypeKind.CLASS;
		super.binaryOperand2[3] = TypeKind.NULL;
		super.binaryResult[3] = TypeKind.BOOLEAN;

		super.binaryOperand1[4] = TypeKind.NULL;
		super.binaryOperand2[4] = TypeKind.CLASS;
		super.binaryResult[4] = TypeKind.BOOLEAN;

		super.binaryOperand1[5] = TypeKind.ARRAY;
		super.binaryOperand2[5] = TypeKind.ARRAY;
		super.binaryResult[5] = TypeKind.BOOLEAN;

		super.binaryOperand1[6] = TypeKind.ARRAY;
		super.binaryOperand2[6] = TypeKind.NULL;
		super.binaryResult[6] = TypeKind.BOOLEAN;

		super.binaryOperand1[7] = TypeKind.NULL;
		super.binaryOperand2[7] = TypeKind.ARRAY;
		super.binaryResult[7] = TypeKind.BOOLEAN;
	}
}
