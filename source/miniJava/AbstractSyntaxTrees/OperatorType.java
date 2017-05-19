package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public abstract class OperatorType extends TypeDenoter {
	
	public TypeKind[] binaryOperand1 = new TypeKind[50];
	public TypeKind[] binaryOperand2 = new TypeKind[50];
	public TypeKind[] binaryResult = new TypeKind[50];
	public TypeKind[] unaryOperand = new TypeKind[50];
	public TypeKind[] unaryResult = new TypeKind[50];

	
	public OperatorType(TypeKind type, SourcePosition posn) {
		super(TypeKind.OPERATOR, null);
	}

	public <A, R> R visit(Visitor<A, R> v, A o) {
		return null;
	}

}
