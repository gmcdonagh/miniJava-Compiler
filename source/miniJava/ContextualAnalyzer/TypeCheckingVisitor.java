package miniJava.ContextualAnalyzer;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.Token;

/* A number of changes were made to the AST classes to accommodate pa3 requirements.
 * For null, a concrete class extending Terminal called NullLiteral was made. 'null' was thus added to
 * the list of keywords and a null Token is formed whenever the string null is found during parsing.
 * Thus, every appearance of null in the AST is as a LiteralExpression containing a NullLiteral containing a null Token.
 * 
 * A new abstract class extending TypeDenoter called OperatorType was also added. Extending this is a number of concrete classes,
 * one for each specific operator. These all share binaryOperand1, binaryOperand2, unaryOperand, binaryResult, and unaryResult lists.
 * The members of these lists are specific to each operator and are created during construction. They specify which operands are allowable
 * for each operator and if allowable what resultant type would be created. These classes are utilized during expression type checking.
 * 
 * SourcePosition was also finally created during pa3. Both tokens and AST nodes have a source position so 3 integers are shared by every SourcePosition
 * object - lineNum, lineStart, and lineFinish. A token will have the same number for all three integers specifying it's line in the program. AST nodes
 * can span multiple lines and thus use the lineNums from their constituent nodes/tokens to create a lineStart and lineFinish which may be the same or may be
 * different. A node's lineNum is always 0 as it is never used, unlike tokens. The toString method for SourcePosition outputs a string of the form 
 * 'lineStart - lineFinish' and is used in Contextual Analysis error messages.
 *  
 *  The Visitor class was updated to account for NullLiteral, but not any of the OperatorTypes as they are not found in the AST 
 *  and are only used for type checking.
 */

public class TypeCheckingVisitor implements Visitor<TypeDenoter, Object> {
	
	public boolean errorPresent = false;
	public boolean checkIndex = false;
	public boolean stringMask;
	
	public TypeCheckingVisitor(boolean stringMaskArg)
	{
		stringMask = stringMaskArg;
	}
	public boolean typeCompare(TypeDenoter t1, TypeDenoter t2, boolean nullAllow)
	{
		if(t1 == null || t2 == null)
		{
			return false;
		}
		if(t1.typeKind == TypeKind.UNSUPPORTED || t2.typeKind == TypeKind.UNSUPPORTED)
		{
			return false;
		}
		if (t1.typeKind == TypeKind.ERROR || t2.typeKind == TypeKind.ERROR)
		{
			return true;
		}
		
		if (t1 instanceof BaseType)
		{
			if (t2 instanceof BaseType)
			{
				if (t1.typeKind == t2.typeKind)
				{
					return true;
				}
				
				else
				{
					return false;
				}
			}
			
			else
			{
				return false;
			}
		}
		
		else if (t1 instanceof ClassType)
		{
			if (t2 instanceof ClassType)
			{
				if (((ClassType) t1).className.decl.name.equals(((ClassType) t2).className.decl.name))
				{
					return true;
				}
				
				else
				{
					return false;
				}
			}
			
			else
			{
				if(t2.typeKind == TypeKind.NULL && nullAllow)
				{
					return true;
				}
				return false;
			}
		}
		
		else if (t1 instanceof ArrayType)
		{
			if(t2.typeKind == TypeKind.NULL && nullAllow)
			{
				return true;
			}
			
			if (t2 instanceof ArrayType)
			{
				return typeCompare(((ArrayType) t1).eltType, ((ArrayType) t2).eltType, nullAllow);
			}
			
			else
			{
				return false;
			}
		}
		
		return false;
	}
	
	public void typeCheck(AST ast){
        ast.visit(this, null);
	}
	
	public TypeDenoter visitPackage(Package prog, TypeDenoter arg) {
		for (ClassDecl c: prog.classDeclList)
	    {
	    	c.visit(this, null);
	    }
		return null;
	}
	
	///////////////////////////////////////////////////////////////////////////////
	//
	// DECLARATIONS
	//
	///////////////////////////////////////////////////////////////////////////////
	
	public TypeDenoter visitClassDecl(ClassDecl cd, TypeDenoter arg) {
		for (FieldDecl f: cd.fieldDeclList)
        	f.visit(this, null);
        for (MethodDecl m: cd.methodDeclList)
        	m.visit(this, null);
		return null;
	}

	
	public TypeDenoter visitFieldDecl(FieldDecl fd, TypeDenoter arg) {
		TypeDenoter type = (TypeDenoter) fd.type.visit(this, null);
		while ((type instanceof ArrayType))
		{
			type = ((ArrayType) type).eltType;
		}
		
		if(type instanceof ClassType)
		{
			if(((ClassType) type).className.decl instanceof MethodDecl)
			{
				System.out.println("*** " + fd.posn.toString() + " Invalid field type ***");
				errorPresent = true;
				return null;
			}
		}
		
		if(type.typeKind == TypeKind.UNSUPPORTED)
		{
			System.out.println("*** " + fd.posn.toString() + " Unsupported type ***");
			errorPresent = true;

			return null;
		}
		return null;
	}

	public TypeDenoter visitMethodDecl(MethodDecl md, TypeDenoter arg) {
		TypeDenoter type = (TypeDenoter) md.type.visit(this, null);
		while ((type instanceof ArrayType))
		{
			type = ((ArrayType) type).eltType;
		}
		
		if(type instanceof ClassType)
		{
			if(((ClassType) type).className.decl instanceof MethodDecl)
			{
				System.out.println("*** " + md.posn.toString() + " Invalid method type ***");
				errorPresent = true;
				return null;
			}
		}
		
		if(type.typeKind == TypeKind.UNSUPPORTED)
		{
			System.out.println("*** " + md.posn.toString() + " Unsupported type ***");
			errorPresent = true;
			return null;
		}
		
		ParameterDeclList pdl = md.parameterDeclList;
        for (ParameterDecl pd: pdl) {
            pd.visit(this, null);
        }
        StatementList sl = md.statementList;

        for (Statement s: sl) {
            s.visit(this, md.type);
        }
		return null;
	}

	public TypeDenoter visitParameterDecl(ParameterDecl pd, TypeDenoter arg) {
		TypeDenoter type = (TypeDenoter) pd.type.visit(this, null);
		while ((type instanceof ArrayType))
		{
			type = ((ArrayType) type).eltType;
		}
		
		if(type instanceof ClassType)
		{
			if(((ClassType) type).className.decl instanceof MethodDecl)
			{
				System.out.println("*** " + pd.posn.toString() + " Invalid parameter type ***");
				errorPresent = true;
				return null;
			}
		}
		
		type = (TypeDenoter) pd.type.visit(this, null);
		if(type.typeKind == TypeKind.UNSUPPORTED)
		{
			System.out.println("*** " + pd.posn.toString() + " Unsupported type ***");
			errorPresent = true;
			return null;
		}
		return null;
	}

	public TypeDenoter visitVarDecl(VarDecl decl, TypeDenoter arg) {
		TypeDenoter type = (TypeDenoter) decl.type.visit(this, null);
		while ((type instanceof ArrayType))
		{
			type = ((ArrayType) type).eltType;
		}
		
		if(type instanceof ClassType)
		{
			if(((ClassType) type).className.decl instanceof MethodDecl)
			{
				System.out.println("*** " + decl.posn.toString() + " Invalid variable type ***");
				errorPresent = true;
				return null;
			}
		}
		
		if(type.typeKind == TypeKind.UNSUPPORTED)
		{
			System.out.println("*** " + decl.posn.toString() + " Unsupported type ***");
			errorPresent = true;
			return null;
		}
		return (TypeDenoter) decl.type.visit(this, null);
	}

	
	///////////////////////////////////////////////////////////////////////////////
	//
	// TYPES
	//
	///////////////////////////////////////////////////////////////////////////////

	
	public TypeDenoter visitBaseType(BaseType type, TypeDenoter arg) {
		return type;
	}

	public TypeDenoter visitClassType(ClassType type, TypeDenoter arg) {
		return type;
	}

	public TypeDenoter visitArrayType(ArrayType type, TypeDenoter arg) {
		return type;
	}

	
	///////////////////////////////////////////////////////////////////////////////
	//
	// STATEMENTS
	//
	///////////////////////////////////////////////////////////////////////////////
	
	public TypeDenoter visitBlockStmt(BlockStmt stmt, TypeDenoter arg) {
		StatementList sl = stmt.sl;
        for (Statement s: sl) {
        	s.visit(this, arg);
        }
		return null;
	}

	public TypeDenoter visitVardeclStmt(VarDeclStmt stmt, TypeDenoter arg) {
		
		if(stmt.initExp instanceof RefExpr)
		{
			if(((RefExpr) stmt.initExp).ref.decl instanceof ClassDecl && !(((RefExpr) stmt.initExp).ref instanceof ThisRef) || ((RefExpr) stmt.initExp).ref.decl instanceof MethodDecl)
			{
				System.out.println("*** " + stmt.posn.toString() + " Not a valid initalizing expression ***");
				errorPresent = true;
				return null;
				
			}
		}
		TypeDenoter typeL = (TypeDenoter) stmt.varDecl.visit(this, null);	
		TypeDenoter typeR = (TypeDenoter) stmt.initExp.visit(this, null);
		if(typeCompare(typeL, typeR, true))
		{
			return null;
		}
		else
		{
			System.out.println("*** " + stmt.posn.toString() + " VarDeclStmt type mismatch ***");
			errorPresent = true;
			return null;
		}
		

	}

	public TypeDenoter visitAssignStmt(AssignStmt stmt, TypeDenoter arg) {
		
		if(stmt.ref instanceof QRef)
		{
			if(((QRef) stmt.ref).ref.decl.type instanceof ArrayType && ((QRef) stmt.ref).id.spelling.equals("length"))
			{		
				System.out.println("*** " + stmt.posn.toString() + " Cannot assign to an array length ***");
				errorPresent = true;
				return null;
			}
		}
		
		if(stmt.ref instanceof ThisRef) 
		{
			System.out.println("*** " + stmt.posn.toString() + " Not a valid assignment expression ***");
			errorPresent = true;
			return null;
		}
		if(stmt.ref.decl instanceof ClassDecl || stmt.ref.decl instanceof MethodDecl)
		{
			System.out.println("*** " + stmt.posn.toString() + " Not a valid assignment expression ***");
			errorPresent = true;
			return null;
		}
		
		if (stmt.val instanceof RefExpr)
		{
			if(((RefExpr) stmt.val).ref.decl instanceof ClassDecl && !(((RefExpr) stmt.val).ref instanceof ThisRef) || ((RefExpr) stmt.val).ref.decl instanceof MethodDecl)
			{
				System.out.println("*** " + stmt.posn.toString() + " Not a valid initalizing expression ***");
				errorPresent = true;
				return null;
				
			}
		}
		
		TypeDenoter typeL = (TypeDenoter) stmt.ref.visit(this, null);
		TypeDenoter typeR = (TypeDenoter) stmt.val.visit(this, null);
		if(typeCompare(typeL, typeR, true))
		{
			return null;

		}
		
		else
		{
			System.out.println("*** " + stmt.posn.toString() + " AssignStmt type mismatch ***");
			errorPresent = true;
			return null;
		}
	}

	public TypeDenoter visitCallStmt(CallStmt stmt, TypeDenoter arg) {
		if (!(stmt.methodRef.decl instanceof MethodDecl))
		{
			System.out.println("*** " + stmt.posn.toString() + " No such method exists ***");
			errorPresent = true;
			return null;
		}
		
		MethodDecl methodDecl = (MethodDecl) stmt.methodRef.decl;
		ParameterDeclList plist = methodDecl.parameterDeclList;
		ExprList alist = stmt.argList;
		if(alist.size() != plist.size())
		{
			System.out.println("*** " + stmt.posn.toString() + " Mismatched arguments and parameters ***");
			errorPresent = true;
			return null;
		}
		
		for (int i = 0; i < alist.size(); i++) {
            
            if(alist.get(i) instanceof RefExpr)
    		{
    			if(((RefExpr) alist.get(i)).ref.decl instanceof ClassDecl && !(((RefExpr) alist.get(i)).ref instanceof ThisRef) || ((RefExpr) alist.get(i)).ref.decl instanceof MethodDecl)
    			{
    				System.out.println("*** " + stmt.posn.toString() + " Not a valid argument ***");
    				errorPresent = true;
    				return null;
    				
    			}
    		}
            TypeDenoter type1 = (TypeDenoter) alist.get(i).visit(this, null);
            TypeDenoter type2 = plist.get(i).type;
            if(typeCompare(type2, type1, true))
            {
            	continue;
            }
            
            else
            {
            	System.out.println("*** " + stmt.posn.toString() + " Argument and parameter types don't match ***");
            	errorPresent = true;
    			return null;
            }
            
        }
		
		return null;
	}
	
	public TypeDenoter visitReturnStmt(ReturnStmt stmt, TypeDenoter arg) {
		if (arg.typeKind == TypeKind.VOID)
		{
			if (stmt.returnExpr != null)
			{
				System.out.println("*** " + stmt.posn.toString() + " Method is void type, cannot return a value ***");
				errorPresent = true;
				return null;
			}

		}
		else if (arg instanceof TypeDenoter)
		{
            if(stmt.returnExpr == null)
            {
            	System.out.println("*** " + stmt.posn.toString() + " Return expression must return a value mathcing method type ***");
				errorPresent = true;
				return null;
            }
			if(stmt.returnExpr instanceof RefExpr)
    		{
    			if(((RefExpr) stmt.returnExpr).ref.decl instanceof ClassDecl && !(((RefExpr) stmt.returnExpr).ref instanceof ThisRef)|| ((RefExpr) stmt.returnExpr).ref.decl instanceof MethodDecl)
    			{
    				System.out.println("*** " + stmt.posn.toString() + " Not a valid returning expression ***");
    				errorPresent = true;
    				return null;
    				
    			}
    		}
            TypeDenoter returnType = (TypeDenoter) stmt.returnExpr.visit(this, null);

            if(typeCompare(arg, returnType, true))
    		{
    			return null;
    		}
            else
            {
            	System.out.println("*** " + stmt.posn.toString() + " Returned type does not match method type  ***");
            	errorPresent = true;
				return null;
            }
		}
		
		return null;
		
		
	}

	
	public TypeDenoter visitIfStmt(IfStmt stmt, TypeDenoter arg) {
        if(stmt.cond instanceof RefExpr)
		{
			if(((RefExpr) stmt.cond).ref.decl instanceof ClassDecl || ((RefExpr) stmt.cond).ref.decl instanceof MethodDecl)
			{
				System.out.println("*** " + stmt.posn.toString() + " Not a valid condition ***");
				errorPresent = true;
				return null;
				
			}
		}
        TypeDenoter condType = (TypeDenoter) stmt.cond.visit(this, null);

        if (condType.typeKind != TypeKind.BOOLEAN && condType.typeKind != TypeKind.ERROR)
        {
        	System.out.println("*** " + stmt.posn.toString() + " Condition must have boolean type  ***");
        	errorPresent = true;
			return null;
        }
        stmt.thenStmt.visit(this, arg);
        if (stmt.elseStmt != null)
            stmt.elseStmt.visit(this, arg);
		return null;
	}

	
	public TypeDenoter visitWhileStmt(WhileStmt stmt, TypeDenoter arg) {
		TypeDenoter condType = (TypeDenoter) stmt.cond.visit(this, null);
		if(stmt.cond instanceof RefExpr)
		{
			if(((RefExpr) stmt.cond).ref.decl instanceof ClassDecl || ((RefExpr) stmt.cond).ref.decl instanceof MethodDecl)
			{
				System.out.println("*** " + stmt.posn.toString() + " Not a valid condition***");
				errorPresent = true;
				return null;
				
			}
		}
        if (condType.typeKind != TypeKind.BOOLEAN && condType.typeKind != TypeKind.ERROR)
        {
        	System.out.println("*** " + stmt.posn.toString() + " Condition must have boolean type  ***");
        	errorPresent = true;
			return null;
        }
        stmt.body.visit(this, arg);
		return null;	
	}

	
	///////////////////////////////////////////////////////////////////////////////
	//
	// EXPRESSIONS
	//
	///////////////////////////////////////////////////////////////////////////////
	
	public TypeDenoter visitUnaryExpr(UnaryExpr expr, TypeDenoter arg) {
		TypeDenoter opType = (TypeDenoter) expr.operator.visit(this, null);
		TypeDenoter unaryType = (TypeDenoter) expr.expr.visit(this, null);
		if(unaryType.typeKind == TypeKind.UNSUPPORTED)
		{
			expr.exprType = new BaseType(TypeKind.UNSUPPORTED, null);
			return expr.exprType; 
		}
		
		if(unaryType.typeKind == TypeKind.ERROR)
		{
			expr.exprType = new BaseType(TypeKind.ERROR, null);
			return expr.exprType; 
		}
		
		if(expr.expr instanceof RefExpr)
		{
			if(((RefExpr) expr.expr).ref.decl instanceof ClassDecl && !(((RefExpr) expr.expr).ref instanceof ThisRef)|| ((RefExpr) expr.expr).ref.decl instanceof MethodDecl)
			{
				System.out.println("*** " + expr.posn.toString() + " This is not a valid variable for this expression ***");
				errorPresent = true;
				expr.exprType = new BaseType(TypeKind.ERROR, null);
				return expr.exprType;
			}
		}
		for (int i = 0; i < ((OperatorType) opType).unaryOperand.length; i++)
		{
			if (unaryType.typeKind == (((OperatorType) opType).unaryOperand[i]))
			{
				TypeKind resultKind = ((OperatorType) opType).unaryResult[i];
				BaseType resultType = new BaseType(resultKind, null);
				expr.exprType = resultType;
				return resultType;
			}
		}
		
		System.out.println("*** " + expr.posn.toString() + " This type and this operator are incompatible ***");
		errorPresent = true;
		expr.exprType = new BaseType(TypeKind.ERROR, null);
		return expr.exprType;
	}

	
	public TypeDenoter visitBinaryExpr(BinaryExpr expr, TypeDenoter arg) {
		TypeDenoter opType = (TypeDenoter) expr.operator.visit(this, null);
		TypeDenoter binaryTypeL = (TypeDenoter) expr.left.visit(this, null);
		TypeDenoter binaryTypeR = (TypeDenoter) expr.right.visit(this, null);
		if(binaryTypeR.typeKind == TypeKind.UNSUPPORTED || binaryTypeL.typeKind == TypeKind.UNSUPPORTED)
		{
			System.out.println("*** " + expr.posn.toString() + " Unsupported type in binary expression ***");
			errorPresent = true;
			expr.exprType = new BaseType(TypeKind.UNSUPPORTED, null);
			return expr.exprType; 
		}
		
		if(binaryTypeR.typeKind == TypeKind.ERROR || binaryTypeL.typeKind == TypeKind.ERROR)
		{
			expr.exprType = new BaseType(TypeKind.ERROR, null);
			return expr.exprType; 
		}
		
		if(expr.left instanceof RefExpr)
		{
			if(((RefExpr) expr.left).ref.decl instanceof ClassDecl && !(((RefExpr) expr.left).ref instanceof ThisRef)|| ((RefExpr) expr.left).ref.decl instanceof MethodDecl && !(((RefExpr) expr.left).ref instanceof ThisRef))
			{
				System.out.println("*** " + expr.posn.toString() + " This is not a valid variable for this expression ***");
				errorPresent = true;
				expr.exprType = new BaseType(TypeKind.ERROR, null);
				return expr.exprType;
			}
		}
		
		if(expr.right instanceof RefExpr)
		{
			if(((RefExpr) expr.right).ref.decl instanceof ClassDecl && !(((RefExpr) expr.right).ref instanceof ThisRef)|| ((RefExpr) expr.right).ref.decl instanceof MethodDecl && !(((RefExpr) expr.right).ref instanceof ThisRef))
			{
				System.out.println("*** " + expr.posn.toString() + " This is not a valid variable for this expression ***");
				errorPresent = true;
				expr.exprType = new BaseType(TypeKind.ERROR, null);
				return expr.exprType;
			}
		}

		for (int i = 0; i < ((OperatorType) opType).binaryOperand1.length; i++)
		{
			if (binaryTypeL.typeKind == (((OperatorType) opType).binaryOperand1[i]))
			{
				if(binaryTypeR.typeKind == (((OperatorType) opType).binaryOperand2[i]))	
				{
					if(binaryTypeL.typeKind == TypeKind.CLASS)
					{
						if(binaryTypeR.typeKind == TypeKind.CLASS)
						{
							if (!((ClassType) binaryTypeL).className.decl.name.equals(((ClassType) binaryTypeR).className.decl.name))
							{
								System.out.println("*** " + expr.posn.toString() + " These types and this operator are incompatible ***");
								errorPresent = true;
								expr.exprType = new BaseType(TypeKind.ERROR, null);
								return expr.exprType; 
							}
						}
					}
					TypeKind resultKind = ((OperatorType) opType).binaryResult[i];
					BaseType resultType = new BaseType(resultKind, null);
					expr.exprType = resultType;
					return resultType;
				}
			}
		}
		
		System.out.println("*** " + expr.posn.toString() + " These types and this operator are incompatible ***");
		errorPresent = true;
		expr.exprType = new BaseType(TypeKind.ERROR, null);
		return expr.exprType; 
	}

	public TypeDenoter visitRefExpr(RefExpr expr, TypeDenoter arg) {
        TypeDenoter type = (TypeDenoter) expr.ref.visit(this, null);
        if(type.typeKind == TypeKind.UNSUPPORTED)
		{
			System.out.println("*** " + expr.posn.toString() + " Unsupported type in reference ***");
			errorPresent = true;
        	expr.exprType = new BaseType(TypeKind.UNSUPPORTED, null);
			return expr.exprType; 
		}
		
		if(type.typeKind == TypeKind.ERROR)
		{
			expr.exprType = new BaseType(TypeKind.ERROR, null);
			return expr.exprType; 
		}
		
		expr.exprType = type;
        return expr.exprType;
	}

	
	public TypeDenoter visitCallExpr(CallExpr expr, TypeDenoter arg) {
		if (!(expr.functionRef.decl instanceof MethodDecl))
		{
			System.out.println("*** " + expr.posn.toString() + " No such method exists ***");
			errorPresent = true;
			expr.exprType = new BaseType(TypeKind.ERROR, null);
			return expr.exprType;
		}
		
		MethodDecl methodDecl = (MethodDecl) expr.functionRef.decl;
		ParameterDeclList plist = methodDecl.parameterDeclList;
		ExprList alist = expr.argList;
		if(alist.size() != plist.size())
		{
			System.out.println("*** " + expr.posn.toString() + " Mismatched arguments and parameters ***");
			errorPresent = true;
			expr.exprType = new BaseType(TypeKind.ERROR, null);
			return expr.exprType;
		}
		
		for (int i = 0; i < alist.size(); i++) {
           
            
            if(alist.get(i) instanceof RefExpr)
    		{
    			if(((RefExpr) alist.get(i)).ref.decl instanceof ClassDecl && !((((RefExpr) alist.get(i)).ref) instanceof ThisRef)|| ((RefExpr) alist.get(i)).ref.decl instanceof MethodDecl)
    			{
    				System.out.println("*** " + expr.posn.toString() + " Not a valid argument ***");
    				errorPresent = true;
    				expr.exprType = new BaseType(TypeKind.ERROR, null);
    				return expr.exprType;    				
    			}
    		}
            
            TypeDenoter type1 = (TypeDenoter) alist.get(i).visit(this, null);
            TypeDenoter type2 = plist.get(i).type;
            
            if(typeCompare(type2, type1, true))
            {
            	if(type1.typeKind == TypeKind.ERROR || type2.typeKind == TypeKind.ERROR)
            	{
                	expr.exprType = new BaseType(TypeKind.ERROR, null);
    				return expr.exprType;
            	}
            	continue;
            }
            
            else
            {
            	if(type1.typeKind == TypeKind.UNSUPPORTED || type2.typeKind == TypeKind.UNSUPPORTED)
            	{
            		System.out.println("*** " + expr.posn.toString() + " Unsupported parameter type ***");
                	errorPresent = true;
                	expr.exprType = new BaseType(TypeKind.UNSUPPORTED, null);
    				return expr.exprType;
            	}
            	
            	System.out.println("*** " + expr.posn.toString() + " Argument and parameter types don't match ***");
            	errorPresent = true;
            	expr.exprType = new BaseType(TypeKind.ERROR, null);
				return expr.exprType;
            }
            
        }
		
		expr.exprType = expr.functionRef.decl.type;
    	return expr.functionRef.decl.type;
	}

	
	public TypeDenoter visitLiteralExpr(LiteralExpr expr, TypeDenoter arg) {
		TypeDenoter type = (TypeDenoter) expr.lit.visit(this, null);
		if(type.typeKind == TypeKind.UNSUPPORTED)
		{
			expr.exprType = new BaseType(TypeKind.UNSUPPORTED, null);
			return expr.exprType; 
		}
		
		if(type.typeKind == TypeKind.ERROR)
		{
			expr.exprType = new BaseType(TypeKind.ERROR, null);
			return expr.exprType; 
		}
		expr.exprType = type;
		return type;
	}

	
	public TypeDenoter visitNewObjectExpr(NewObjectExpr expr, TypeDenoter arg) {
		if(!(expr.classtype.className.decl instanceof ClassDecl))
		{
			System.out.println("*** " + expr.posn.toString() + " Identifier referenced not a class  ***");
			errorPresent = true;
			expr.exprType = new BaseType(TypeKind.ERROR, null);
			return expr.exprType;
		}
		expr.exprType = expr.classtype;
		return expr.classtype;
	}

	
	public TypeDenoter visitNewArrayExpr(NewArrayExpr expr, TypeDenoter arg) {
		TypeDenoter sizeType = (TypeDenoter) expr.sizeExpr.visit(this, null);
		if(sizeType.typeKind == TypeKind.UNSUPPORTED)
		{
			System.out.println("*** " + expr.posn.toString() + " Unsupported size type  ***");
			errorPresent = true;
			expr.exprType = new BaseType(TypeKind.UNSUPPORTED, null);
			return expr.exprType; 
		}
		
		if(sizeType.typeKind == TypeKind.ERROR)
		{
			expr.exprType = new BaseType(TypeKind.ERROR, null);
			return expr.exprType; 
		}

		if(expr.sizeExpr instanceof RefExpr)
		{
			if(((RefExpr) expr.sizeExpr).ref.decl instanceof ClassDecl || ((RefExpr) expr.sizeExpr).ref.decl instanceof MethodDecl)
			{
				System.out.println("*** " + expr.posn.toString() + " Not a valid size expression ***");
				errorPresent = true;
				expr.exprType = new BaseType(TypeKind.ERROR, null);
				return expr.exprType;
				
			}
		}
		
		if(sizeType.typeKind != TypeKind.INT)
		{
			System.out.println("*** " + expr.posn.toString() + " Size of array must be an integer  ***");
			errorPresent = true;
			expr.exprType = new BaseType(TypeKind.ERROR, null);
			return expr.exprType;
		}
		
		if(expr.eltType instanceof BaseType)
		{
				ArrayType type = new ArrayType(expr.eltType, null);
				expr.exprType = type;
				return type;
		}
		
		else if (expr.eltType instanceof ClassType)
		{
			if(!(((ClassType) expr.eltType).className.decl instanceof ClassDecl))
			{
				System.out.println("*** " + expr.posn.toString() + " Identifier referenced not a valid type for array  ***");
				errorPresent = true;
				expr.exprType = new BaseType(TypeKind.ERROR, null);
				return expr.exprType;
			}
			
			else
			{
				ArrayType type = new ArrayType(expr.eltType, null);
				expr.exprType = type;
				return type;
			}
		
		
		}
		
		ArrayType type = new ArrayType(expr.eltType, null);
		expr.exprType = type;
		return type;
	}

	///////////////////////////////////////////////////////////////////////////////
	//
	// REFERENCES
	//
	///////////////////////////////////////////////////////////////////////////////
	
	public TypeDenoter visitThisRef(ThisRef ref, TypeDenoter arg) {
		Token t = new Token((byte) 0,ref.decl.name, null);
		Identifier i = new Identifier(t);
		i.decl = ref.decl;
		return new ClassType(i,null);
	}

	
	public TypeDenoter visitIdRef(IdRef ref, TypeDenoter arg) {
		TypeDenoter type = (TypeDenoter) ref.id.visit(this, null);
		if(type != null)
		{
			if(type.typeKind == TypeKind.UNSUPPORTED)
			{
				System.out.println("*** " + ref.posn.toString() + " Unsupported type in reference  ***");
				errorPresent = true;
				return new BaseType(TypeKind.UNSUPPORTED, null);
			}
			
			if(type.typeKind == TypeKind.ERROR)
			{
				return new BaseType(TypeKind.ERROR, null);
			}
		}
		
		if(type == null)
		{
			return ref.decl.type;	
		}
		
		else
		{
			return type;
		}
		
	}

	
	public TypeDenoter visitIxIdRef(IxIdRef ref, TypeDenoter arg) {
		TypeDenoter typeI = (TypeDenoter) ref.id.visit(this, null);
		if(typeI != null)
		{
			if(typeI.typeKind == TypeKind.UNSUPPORTED)
			{
				System.out.println("*** " + ref.posn.toString() + " Unsupported type in reference  ***");
				errorPresent = true;
				return new BaseType(TypeKind.UNSUPPORTED, null);
			}
			
			if(typeI.typeKind == TypeKind.ERROR)
			{
				return new BaseType(TypeKind.ERROR, null);
			}		
		}	
		
		TypeDenoter indexType = (TypeDenoter) ref.indexExpr.visit(this, null);
		if(indexType.typeKind == TypeKind.UNSUPPORTED)
		{
			System.out.println("*** " + ref.posn.toString() + " Unsupported type in index  ***");
			errorPresent = true;
			return new BaseType(TypeKind.UNSUPPORTED, null);
		}
		
		if(indexType.typeKind == TypeKind.ERROR)
		{
			return new BaseType(TypeKind.ERROR, null);
		}
		
		if(ref.indexExpr instanceof RefExpr)
		{
			if(((RefExpr) ref.indexExpr).ref.decl instanceof ClassDecl || ((RefExpr) ref.indexExpr).ref.decl instanceof MethodDecl)
			{
				System.out.println("*** " + ref.posn.toString() + " Not a valid indexing expression ***");
				errorPresent = true;
				return new BaseType(TypeKind.ERROR, null);
				
			}
		}
		
		if(indexType.typeKind != TypeKind.INT)
		{
			System.out.println("*** " + ref.posn.toString() + " Not a valid indexing expression  ***");
			errorPresent = true;
			return new BaseType(TypeKind.ERROR, null);	
		}
		
		TypeDenoter type = ref.decl.type;

		if(ref.decl instanceof MethodDecl || ref.decl instanceof ClassDecl)
		{
			System.out.println("*** " + ref.posn.toString() + " Not a valid array ***");
			errorPresent = true;
			return new BaseType(TypeKind.ERROR, null);
		}
		if (!(type instanceof ArrayType))
		{
			System.out.println("*** " + ref.posn.toString() + " Cannot index a non-array typed identifier ***");
			errorPresent = true;
			return new BaseType(TypeKind.ERROR, null);
		}
		ArrayType type2 = (ArrayType) type;
		
		TypeDenoter type3 = type2.eltType;
		
		return type3;
	}

	
	public TypeDenoter visitQRef(QRef ref, TypeDenoter arg) {	
		if(ref.ref.decl.type instanceof ArrayType)
		{
			if(ref.id.spelling.equals("length"))
			{
				return new BaseType(TypeKind.INT, null);
			}
		}
		TypeDenoter type = (TypeDenoter) ref.id.visit(this, null);
		if(type != null)
		{
			if(type.typeKind == TypeKind.UNSUPPORTED)
			{
				System.out.println("*** " + ref.posn.toString() + " Unsupported type in reference  ***");
				errorPresent = true;
				return new BaseType(TypeKind.UNSUPPORTED, null);
			}
			
			if(type.typeKind == TypeKind.ERROR)
			{
				return new BaseType(TypeKind.ERROR, null);
			}
		}
		if(checkIndex)
		{
			if(ref.id.decl.type instanceof ArrayType)
			{
				System.out.println("*** " + ref.posn.toString() + " Must index array in this context ***");
				errorPresent = true;
				return new BaseType(TypeKind.ERROR, null);
			}
		}
		if(ref.ref instanceof QRef)
		{
			checkIndex = true;
		}
		else
		{
			checkIndex = false;
		}
		ref.ref.visit(this, arg);
		if (ref.ref instanceof IdRef)
		{
			if(ref.ref.decl.type instanceof ArrayType)
			{
				System.out.println("*** " + ref.posn.toString() + " Must index array in this context ***");
				errorPresent = true;
				return new BaseType(TypeKind.ERROR, null);
			}
		}
		return ref.decl.type;	
		}

	
	public TypeDenoter visitIxQRef(IxQRef ref, TypeDenoter arg) {
		TypeDenoter typeI = (TypeDenoter) ref.id.visit(this, null);
		if (typeI != null)
		{
			if(typeI.typeKind == TypeKind.UNSUPPORTED)
			{
				System.out.println("*** " + ref.posn.toString() + " Unsupported type in reference ***");
				errorPresent = true;
				return new BaseType(TypeKind.UNSUPPORTED, null);
			}
			
			if(typeI.typeKind == TypeKind.ERROR)
			{
				return new BaseType(TypeKind.ERROR, null);
			}
		}
		
		if(ref.ref instanceof QRef)
		{
			checkIndex = true;
		}
		else
		{
			checkIndex = false;
		}
		ref.ref.visit(this, arg);
		if (ref.ref instanceof IdRef)
		{
			if(ref.ref.decl.type instanceof ArrayType)
			{
				System.out.println("*** " + ref.posn.toString() + " Must index array in this context ***");
				errorPresent = true;
				return new BaseType(TypeKind.ERROR, null);
			}
		}
		TypeDenoter indexType = (TypeDenoter) ref.ixExpr.visit(this, null);
		
		if(indexType.typeKind == TypeKind.UNSUPPORTED)
		{
			System.out.println("*** " + ref.posn.toString() + " Unsupported type in index  ***");
			errorPresent = true;
			return new BaseType(TypeKind.UNSUPPORTED, null);
		}
		
		if(indexType.typeKind == TypeKind.ERROR)
		{
			return new BaseType(TypeKind.ERROR, null);
		}
		
		if(ref.ixExpr instanceof RefExpr)
		{
			if(((RefExpr) ref.ixExpr).ref.decl instanceof ClassDecl || ((RefExpr) ref.ixExpr).ref.decl instanceof MethodDecl)
			{
				System.out.println("*** " + ref.posn.toString() + " Not a valid indexing expression ***");
				errorPresent = true;
				return new BaseType(TypeKind.ERROR, null);
				
			}
		}
		
		if(indexType.typeKind != TypeKind.INT)
		{
			System.out.println("*** " + ref.posn.toString() + " Not a valid indexing expression  ***");
			errorPresent = true;
			return new BaseType(TypeKind.ERROR, null);
				
		}
		
		TypeDenoter type = ref.decl.type;
		if(ref.decl instanceof MethodDecl || ref.decl instanceof ClassDecl)
		{
			System.out.println("*** " + ref.posn.toString() + " Not a valid array ***");
			errorPresent = true;
			return new BaseType(TypeKind.ERROR, null);
		}
		
		if (!(type instanceof ArrayType))
		{
			System.out.println("*** " + ref.posn.toString() + " Cannot index a non-array typed identifier");
			errorPresent = true;
			return new BaseType(TypeKind.ERROR, null);
		}
		ArrayType type2 = (ArrayType) type;
		TypeDenoter type3 = type2.eltType;
		while(type3 instanceof ArrayType)
		{
			type3 = ((ArrayType) type3).eltType;
		}
		return type3;		}

	///////////////////////////////////////////////////////////////////////////////
	//
	// TERMINALS
	//
	///////////////////////////////////////////////////////////////////////////////
	
	public TypeDenoter visitIdentifier(Identifier id, TypeDenoter arg) {
		if (id.decl.type instanceof ClassType)
		{
			if (((ClassType) id.decl.type).className.decl.name.equals("String") && !(stringMask))
			{
				System.out.println("*** " + id.posn.toString() + " Unsupported type ***");
				errorPresent = true;
				return new BaseType(TypeKind.UNSUPPORTED, null);
			}		
		}
		
		if(id.decl.type instanceof ArrayType)
		{
			if (((ArrayType) id.decl.type).eltType instanceof ClassType)
			{
				TypeDenoter type = ((ArrayType) id.decl.type).eltType;
				if (((ClassType) type).className.decl.name.equals("String") && !(stringMask))
				{
					System.out.println("*** " + id.posn.toString() + " Unsupported type ***");
					errorPresent = true;
					return new BaseType(TypeKind.UNSUPPORTED, null);
				}
			}
		}
		
		return null;
 	}

	
	public TypeDenoter visitOperator(Operator op, TypeDenoter arg) {
		if (op.spelling.equals("+"))
			return new AdditionOperator(null, null);
		if (op.spelling.equals("-"))
			return new SubtractionOperator(null, null);
		if (op.spelling.equals("/"))
			return new DivisionOperator(null, null);
		if (op.spelling.equals("*"))
			return new MultiplicationOperator(null, null);
		if (op.spelling.equals(">"))
			return new GreaterThanOperator(null, null);
		if (op.spelling.equals("<"))
			return new LessThanOperator(null, null);
		if (op.spelling.equals(">="))
			return new GreaterThanOrEqualOperator(null, null);
		if (op.spelling.equals("<="))
			return new LessThanOrEqualOperator(null, null);
		if (op.spelling.equals("=="))
			return new EqualOperator(null, null);
		if (op.spelling.equals("!="))
			return new NotEqualOperator(null, null);
		if (op.spelling.equals("!"))
			return new ComplementOperator(null, null);
		if (op.spelling.equals("&&"))
			return new AndOperator(null, null);
		if (op.spelling.equals("||"))
			return new OrOperator(null, null);
		return null;
	}

	
	public TypeDenoter visitIntLiteral(IntLiteral num, TypeDenoter arg) {
		return new BaseType(TypeKind.INT, null);
	}

	
	public TypeDenoter visitBooleanLiteral(BooleanLiteral bool, TypeDenoter arg) {
		return new BaseType(TypeKind.BOOLEAN, null);
	}

	
	public TypeDenoter visitNullLiteral(NullLiteral lit, TypeDenoter arg) {
		return new BaseType(TypeKind.NULL, null);
	}
}
