package miniJava.CodeGenerator;

import java.util.ArrayList;

import mJAM.Machine;
import mJAM.Machine.Op;
import mJAM.Machine.Prim;
import mJAM.Machine.Reg;
import miniJava.AbstractSyntaxTrees.AST;
import miniJava.AbstractSyntaxTrees.ArrayType;
import miniJava.AbstractSyntaxTrees.AssignStmt;
import miniJava.AbstractSyntaxTrees.BaseType;
import miniJava.AbstractSyntaxTrees.BinaryExpr;
import miniJava.AbstractSyntaxTrees.BlockStmt;
import miniJava.AbstractSyntaxTrees.BooleanLiteral;
import miniJava.AbstractSyntaxTrees.CallExpr;
import miniJava.AbstractSyntaxTrees.CallStmt;
import miniJava.AbstractSyntaxTrees.ClassDecl;
import miniJava.AbstractSyntaxTrees.ClassType;
import miniJava.AbstractSyntaxTrees.Declaration;
import miniJava.AbstractSyntaxTrees.ExprList;
import miniJava.AbstractSyntaxTrees.Expression;
import miniJava.AbstractSyntaxTrees.FieldDecl;
import miniJava.AbstractSyntaxTrees.IdRef;
import miniJava.AbstractSyntaxTrees.Identifier;
import miniJava.AbstractSyntaxTrees.IfStmt;
import miniJava.AbstractSyntaxTrees.IntLiteral;
import miniJava.AbstractSyntaxTrees.IxIdRef;
import miniJava.AbstractSyntaxTrees.IxQRef;
import miniJava.AbstractSyntaxTrees.LiteralExpr;
import miniJava.AbstractSyntaxTrees.MethodDecl;
import miniJava.AbstractSyntaxTrees.NewArrayExpr;
import miniJava.AbstractSyntaxTrees.NewObjectExpr;
import miniJava.AbstractSyntaxTrees.NullLiteral;
import miniJava.AbstractSyntaxTrees.Operator;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.ParameterDecl;
import miniJava.AbstractSyntaxTrees.QRef;
import miniJava.AbstractSyntaxTrees.RefExpr;
import miniJava.AbstractSyntaxTrees.ReturnStmt;
import miniJava.AbstractSyntaxTrees.Statement;
import miniJava.AbstractSyntaxTrees.StatementList;
import miniJava.AbstractSyntaxTrees.ThisRef;
import miniJava.AbstractSyntaxTrees.TypeDenoter;
import miniJava.AbstractSyntaxTrees.TypeKind;
import miniJava.AbstractSyntaxTrees.UnaryExpr;
import miniJava.AbstractSyntaxTrees.VarDecl;
import miniJava.AbstractSyntaxTrees.VarDeclStmt;
import miniJava.AbstractSyntaxTrees.Visitor;
import miniJava.AbstractSyntaxTrees.WhileStmt;
import miniJava.SyntacticAnalyzer.Token;
	// A number of arguments and return values are used for references. A passed argument of 0 means that the reference is one that will be assigned to.
	// A passed argument of 1 means it is appearing as its value in an expression. A passed argument of 2 means the reference is for a method.
	// A return value from a reference of -1 means that a static value was found for it, 0 otherwise.
public class EmittingVisitor implements Visitor<Object, Object> {
    
	public boolean preVisit = true;
	public int stack_base_space = 0;
	public int patchAddr_Call_main = -1;
	
	public int ref_level = 1;
	public MethodDecl curr_method;
	Declaration[] callMap = new Declaration[1024];
    
	public void emitInstructions(AST ast){
		Machine.initCodeGen();
		ast.visit(this, null);
	}
	
	public Object visitPackage(Package prog, Object arg) { 
		
		for (ClassDecl c: prog.classDeclList)
	    {
	    	c.visit(this, null);
	    }
		
		preVisit = false;
		
		Machine.emit(Op.LOADL,0);            
		Machine.emit(Prim.newarr);           
		patchAddr_Call_main = Machine.nextInstrAddr();
		Machine.emit(Op.CALL,Reg.CB,-1); 
		Machine.emit(Op.HALT,0,0,0);
	
		for (ClassDecl c: prog.classDeclList)
	    {
	    	c.visit(this, null);
	    }
		
		for(int i = 0; i < 1024; i++)
		{
			if(callMap[i] != null)
			{
				Machine.patch(i, ((UnknownValue) callMap[i].entity).address);
			}
		}
		return null;
	}

	///////////////////////////////////////////////////////////////////////////////
	//
	// DECLARATIONS
	//
	///////////////////////////////////////////////////////////////////////////////
	
	public Object visitClassDecl(ClassDecl cd, Object arg) {
		if (preVisit)
		{
			int field_sum = 0;
			for (int i = 0; i < cd.fieldDeclList.size(); i++)
			{
				if(!(cd.fieldDeclList.get(i).isStatic))
				{
					field_sum++;
				}
			}
			cd.entity = new UnknownValue(field_sum, "", -1);
			int ob_space = 0;
			for (FieldDecl f: cd.fieldDeclList){
            	ob_space = (int) f.visit(this, ob_space);
			}
		}
		
		else
		{
			for (MethodDecl m: cd.methodDeclList)
            	m.visit(this, null); 
		}
		return null;
	}

	public Object visitFieldDecl(FieldDecl fd, Object arg) {
		int newspace = (int) arg;
		
		if(fd.isStatic)
		{
			if(fd.type instanceof BaseType)
			{
				Machine.emit(Machine.Op.LOADL, 0);
				fd.entity = new UnknownValue(1, "sb", stack_base_space);
				stack_base_space++;
			}
			else
			{
				Machine.emit(Machine.Op.PUSH, 1);
				fd.entity = new UnknownValue(1, "sb", stack_base_space);
				stack_base_space++;
			}
		}
		
		else
		{
			fd.entity = new UnknownValue(1, "ob", (int) arg);
			newspace = newspace + 1;
		}
		return newspace;
	}

	
	public Object visitMethodDecl(MethodDecl md, Object arg) {	
		curr_method = md;
		int local_space = -1;
		if(md.name.equals("main") && md.isStatic && !(md.isPrivate) && md.type.typeKind == TypeKind.VOID && md.parameterDeclList.size() == 1)
		{
			TypeDenoter type = md.parameterDeclList.get(0).type;
			if(type instanceof ArrayType)
			{
				TypeDenoter etype = ((ArrayType) type).eltType;
				if(etype instanceof ClassType)
				{
					if(((ClassType) etype).className.spelling.equals("String"))
					{
						md.entity = new UnknownValue(-1, "", Machine.nextInstrAddr());
						md.parameterDeclList.get(0).entity = new UnknownValue(1, "sb", stack_base_space);
						stack_base_space++;
						callMap[patchAddr_Call_main] = md;
						
						local_space = 3;
						for (Statement s: md.statementList) {
				            local_space = (int) s.visit(this, local_space);
				            
				        }
					}
				}
			}
		}
		
		else
		{
			md.entity = new UnknownValue(-1, "", Machine.nextInstrAddr());
			int pnum = md.parameterDeclList.size();
			int j = 0;
			for (int i = -pnum; i < 0; i++)
			{
				md.parameterDeclList.get(j).entity = new UnknownValue(1, "lb", i);
				j++;
			}
			
			local_space = 3;

			for (Statement s: md.statementList) {
	            local_space = (int) s.visit(this, local_space);
	            
	        }
		}
	
		return null;
	}

	
	public Object visitParameterDecl(ParameterDecl pd, Object arg) {
		// empty
		return null;
	}

	
	public Object visitVarDecl(VarDecl decl, Object arg) {
		int local_space = (int) arg;
		if(decl.type instanceof BaseType)
		{
			Machine.emit(Machine.Op.LOADL, 0);
			decl.entity = new UnknownValue(1, "lb", local_space);
			local_space++;
		}
		else
		{
			Machine.emit(Machine.Op.PUSH, 1);
			decl.entity = new UnknownValue(1, "lb", local_space);
			local_space++;
		}
		
		return local_space;
	}

	///////////////////////////////////////////////////////////////////////////////
	//
	// TYPES
	//
	///////////////////////////////////////////////////////////////////////////////

	public Object visitBaseType(BaseType type, Object arg) {
		return null;
	}

	
	public Object visitClassType(ClassType type, Object arg) {
		return null;
	}

	
	public Object visitArrayType(ArrayType type, Object arg) {
		return null;
	}
	
	///////////////////////////////////////////////////////////////////////////////
	//
	// STATEMENTS
	//
	///////////////////////////////////////////////////////////////////////////////

	public Object visitBlockStmt(BlockStmt stmt, Object arg) {
		int starting_space = (int) arg;
		int local_space = (int) arg;
		StatementList sl = stmt.sl;
        for (Statement s: sl) {
        	local_space = (int) s.visit(this, local_space);
        }
        Machine.emit(Machine.Op.POP, local_space - starting_space);
		return starting_space;
	}

	public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		int local_space = (int) arg;
		local_space = (int) stmt.varDecl.visit(this, local_space);
		stmt.initExp.visit(this, null);
		Machine.emit(Machine.Op.STORE, Machine.Reg.LB, ((UnknownValue) stmt.varDecl.entity).address);
		return local_space;
	}

	public Object visitAssignStmt(AssignStmt stmt, Object arg) {
		int local_space = (int) arg;
		Declaration refDecl = (Declaration) stmt.ref.decl;
		if (stmt.ref instanceof IdRef && refDecl instanceof FieldDecl)
		{
			if(((FieldDecl) refDecl).isStatic)
			{
				stmt.val.visit(this, null);
				int d = ((UnknownValue) refDecl.entity).address;
				Machine.emit(Machine.Op.STORE, 1, Machine.Reg.SB, d);
			}
			
			else
			{
				stmt.val.visit(this, null);
				int d = ((UnknownValue) refDecl.entity).address;
				Machine.emit(Machine.Op.STORE, 1, Machine.Reg.OB, d);
			}
		}
		
		else if (stmt.ref instanceof IdRef && refDecl instanceof VarDecl)
		{
			stmt.val.visit(this, null);
			int d = ((UnknownValue) refDecl.entity).address;
			Machine.emit(Machine.Op.STORE, 1, Machine.Reg.LB, d);
		}
		
		else if (stmt.ref instanceof IdRef && refDecl instanceof ParameterDecl)
		{
			stmt.val.visit(this, null);
			int d = ((UnknownValue) refDecl.entity).address;
			Machine.emit(Machine.Op.STORE, 1, Machine.Reg.LB, d);
		}
		
		if (stmt.ref instanceof IxIdRef && refDecl instanceof FieldDecl)
		{
			if(((FieldDecl) refDecl).isStatic)
			{
				int d = ((UnknownValue) refDecl.entity).address;
				Machine.emit(Machine.Op.LOAD, 1, Machine.Reg.SB, d);
				((IxIdRef) stmt.ref).indexExpr.visit(this, null);
				stmt.val.visit(this, null);
				Machine.emit(Machine.Prim.arrayupd);
			}
			
			else
			{
				int d = ((UnknownValue) refDecl.entity).address;
				Machine.emit(Machine.Op.LOAD, 1, Machine.Reg.OB, d);
				((IxIdRef) stmt.ref).indexExpr.visit(this, null);
				stmt.val.visit(this, null);
				Machine.emit(Machine.Prim.arrayupd);
			}
		}
		
		else if (stmt.ref instanceof IxIdRef && refDecl instanceof VarDecl)
		{
			int d = ((UnknownValue) refDecl.entity).address;
			Machine.emit(Machine.Op.LOAD, 1, Machine.Reg.LB, d);
			((IxIdRef) stmt.ref).indexExpr.visit(this, null);
			stmt.val.visit(this, null);
			Machine.emit(Machine.Prim.arrayupd);
		}
		
		else if (stmt.ref instanceof IxIdRef && refDecl instanceof ParameterDecl)
		{
			int d = ((UnknownValue) refDecl.entity).address;
			Machine.emit(Machine.Op.LOAD, 1, Machine.Reg.LB, d);
			((IxIdRef) stmt.ref).indexExpr.visit(this, null);
			stmt.val.visit(this, null);
			Machine.emit(Machine.Prim.arrayupd);
		}
				
		if (stmt.ref instanceof QRef)
		{
			Declaration lDecl = ((QRef) stmt.ref).ref.decl;
			if (lDecl instanceof ClassDecl && !(((QRef) stmt.ref).ref instanceof ThisRef))
			{
				int d = ((UnknownValue) ((QRef) stmt.ref).decl.entity).address;
				stmt.val.visit(this, null);
				Machine.emit(Machine.Op.STORE, 1, Machine.Reg.SB, d);
				return local_space;
			}
		
			ref_level = 1;
			stmt.ref.visit(this, 0);
			stmt.val.visit(this, null);
			Machine.emit(Machine.Prim.fieldupd);
		}
		
		else if (stmt.ref instanceof IxQRef)
		{
			Declaration lDecl = ((IxQRef) stmt.ref).ref.decl;
			if (lDecl instanceof ClassDecl)
			{
				int d = ((UnknownValue) ((IxQRef) stmt.ref).decl.entity).address;
				Machine.emit(Machine.Op.LOAD, 1, Machine.Reg.SB, d);
				((IxQRef) stmt.ref).ixExpr.visit(this, null);
				stmt.val.visit(this, null);
				Machine.emit(Machine.Prim.arrayupd);
				return local_space;
			}
			
			ref_level = 1;
			stmt.ref.visit(this, 0);
			((IxQRef) stmt.ref).ixExpr.visit(this, null);
			stmt.val.visit(this, null);
			Machine.emit(Machine.Prim.arrayupd);			
		}
		
		return local_space;
	}

	@Override
	public Object visitCallStmt(CallStmt stmt, Object arg) {
		int local_space = (int) arg;
		if(stmt.methodRef instanceof QRef)
		{
			QRef ref1 = (QRef) stmt.methodRef;
			if(ref1.ref instanceof QRef)
			{
				QRef ref2 = (QRef) ref1.ref;
				if (ref2.ref instanceof IdRef)
				{
					IdRef ref3 = (IdRef) ref2.ref;
					if (ref3.id.spelling.equals("System") && ref3.id.decl instanceof ClassDecl && ref3.id.decl.name.equals("System"))
					{
						if (ref2.id.spelling.equals("out") && ref2.id.decl instanceof FieldDecl)
						{
							if(ref1.id.spelling.equals("println"))
							{
								stmt.argList.get(0).visit(this, null);
								Machine.emit(Machine.Prim.putintnl);
								return local_space;
							}
						}
					}
				}
			}
			
		}
		if(((MethodDecl) stmt.methodRef.decl).isStatic)
		{
			ExprList al = stmt.argList;
	        for (Expression e: al) {
	                e.visit(this, null);
	            }
	        
	        callMap[Machine.nextInstrAddr()] = stmt.methodRef.decl;
	        Machine.emit(Machine.Op.CALL, -1);
		}
		
		else
		{
			ExprList al = stmt.argList;
			for (Expression e: al) {
	                e.visit(this, null);
	            }
			
			if(stmt.methodRef instanceof IdRef)
			{
				Machine.emit(Machine.Op.LOAD, 1, Machine.Reg.OB, 0);
			}
			
			else
			{
				stmt.methodRef.visit(this, 2);
			}
			
			callMap[Machine.nextInstrAddr()] = stmt.methodRef.decl;
			Machine.emit(Machine.Op.CALLI, -1);
		}
		
		if(stmt.methodRef.decl.type.typeKind != TypeKind.VOID)
		{
			Machine.emit(Machine.Op.POP, 1);
		}
		
		return local_space;
	}

	@Override
	public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
		int local_space = (int) arg;
		if (curr_method.type.typeKind == TypeKind.VOID)
		{
			Machine.emit(Machine.Op.RETURN, 0, Machine.Reg.LB, curr_method.parameterDeclList.size());
		}
		
		else
		{
			stmt.returnExpr.visit(this, null);
			Machine.emit(Machine.Op.RETURN, 1, Machine.Reg.LB, curr_method.parameterDeclList.size());
		}
		
		return local_space;
	}

	@Override
	public Object visitIfStmt(IfStmt stmt, Object arg) {
		int local_space = (int) arg;
		if(stmt.elseStmt == null)
		{
			stmt.cond.visit(this, null);
			int i = Machine.nextInstrAddr();
			Machine.emit(Machine.Op.JUMPIF, 0, Machine.Reg.CB, -1);
			stmt.thenStmt.visit(this, local_space);
			int j = Machine.nextInstrAddr();
			Machine.patch(i, j);
		}
		
		else
		{
			stmt.cond.visit(this, null);
			int i = Machine.nextInstrAddr();
			Machine.emit(Machine.Op.JUMPIF, 0, Machine.Reg.CB, -1);
			local_space = (int) stmt.thenStmt.visit(this, local_space);
			int j = Machine.nextInstrAddr();
			Machine.emit(Machine.Op.JUMP, 0, Machine.Reg.CB, -1);
			int g = Machine.nextInstrAddr();
			Machine.patch(i, g);
			local_space = (int) stmt.elseStmt.visit(this, local_space);
			Machine.patch(j, Machine.nextInstrAddr());
		}
		
		
		return local_space;
	}

	@Override
	public Object visitWhileStmt(WhileStmt stmt, Object arg) {
		int local_space = (int) arg;
		int j = Machine.nextInstrAddr();
		Machine.emit(Machine.Op.JUMP, 0, Machine.Reg.CB, -1);
		int g = Machine.nextInstrAddr();
		local_space = (int) stmt.body.visit(this, local_space);
		int h = Machine.nextInstrAddr();
		Machine.patch(j, h);
		stmt.cond.visit(this, null);
		Machine.emit(Machine.Op.JUMPIF, 1, Machine.Reg.CB, g);		
		return local_space;
	}
	
	///////////////////////////////////////////////////////////////////////////////
	//
	// EXPRESSIONS
	//
	///////////////////////////////////////////////////////////////////////////////

	@Override
	public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
		expr.expr.visit(this, null);
		switch (expr.operator.kind)
		{
			case Token.MINUS:
			{
				Machine.emit(Machine.Prim.neg);
				break;
			}
			
			case Token.COMPLEMENT:
			{
				Machine.emit(Machine.Prim.not);
				break;
			}
		}
		
		return null;
	}

	@Override
	public Object visitBinaryExpr(BinaryExpr expr, Object arg) {	
		expr.left.visit(this, null);
		expr.right.visit(this, null);

		switch (expr.operator.kind)
		{
			case Token.GT:
			{
				Machine.emit(Machine.Prim.gt);
				break;
			}
			
			case Token.LT:
			{
				Machine.emit(Machine.Prim.lt);
				break;

			}
			
			case Token.ECOMPARISON:
			{
				Machine.emit(Machine.Prim.eq);
				break;

			}
			
			case Token.LTE:
			{
				Machine.emit(Machine.Prim.le);
				break;

			}
			
			case Token.GTE:
			{
				Machine.emit(Machine.Prim.ge);
				break;
			}
			
			case Token.NECOMPARISON:
			{
				Machine.emit(Machine.Prim.ne);
				break;
			}
			
			case Token.AND:
			{
				Machine.emit(Machine.Prim.and);
				break;
			}
			
			case Token.OR:
			{
				Machine.emit(Machine.Prim.or);
				break;
			}
			
			case Token.PLUS:
			{
				Machine.emit(Machine.Prim.add);
				break;
			}
			
			case Token.TIMES:
			{
				Machine.emit(Machine.Prim.mult);
				break;
			}
			
			case Token.DIVIDE:
			{
				Machine.emit(Machine.Prim.div);
				break;
			}
			
			case Token.MINUS:
			{
				Machine.emit(Machine.Prim.sub);
				break;
			}
		}
		
		return null;
	}

	
	public Object visitRefExpr(RefExpr expr, Object arg) {
		expr.ref.visit(this, 1);
		return null;
	}

	
	public Object visitCallExpr(CallExpr expr, Object arg) {
		if(expr.functionRef instanceof QRef)
		{
			QRef ref1 = (QRef) expr.functionRef;
			if(ref1.ref instanceof QRef)
			{
				QRef ref2 = (QRef) ref1.ref;
				if (ref2.ref instanceof IdRef)
				{
					IdRef ref3 = (IdRef) ref2.ref;
					if (ref3.id.spelling.equals("System"))
					{
						if (ref2.id.spelling.equals("out"))
						{
							if(ref1.id.spelling.equals("println"))
							{
								expr.argList.get(0).visit(this, null);
								Machine.emit(Machine.Prim.putintnl);
								return null;
							}
						}
					}
				}
			}
			
		}
		if(((MethodDecl) expr.functionRef.decl).isStatic)
		{
			ExprList al = expr.argList;
	        for (Expression e: al) {
	                e.visit(this, null);
	            }
	        
	        callMap[Machine.nextInstrAddr()] = expr.functionRef.decl;
	        Machine.emit(Machine.Op.CALL, -1);
		}
		
		else
		{
			ExprList al = expr.argList;
			for (Expression e: al) {
	                e.visit(this, null);
	            }
			
			if(expr.functionRef instanceof IdRef)
			{
				Machine.emit(Machine.Op.LOADA, 1, Machine.Reg.OB, 0);
			}
			
			else
			{
				expr.functionRef.visit(this, 2);
			}
			
			callMap[Machine.nextInstrAddr()] = expr.functionRef.decl;
			Machine.emit(Machine.Op.CALLI, -1);
		}
		
		
		return null;
	}

	
	public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
		if (expr.lit instanceof IntLiteral)
		{
			Machine.emit(Machine.Op.LOADL, Integer.parseInt(expr.lit.spelling));
		}
		
		if (expr.lit instanceof BooleanLiteral)
		{
			if(expr.lit.spelling.equals("false"))
			{
				Machine.emit(Machine.Op.LOADL, 0);
			}
			
			else if(expr.lit.spelling.equals("true"))
			{
				Machine.emit(Machine.Op.LOADL, 1);
			}
		}
		
		if (expr.lit instanceof NullLiteral)
		{
			Machine.emit(Machine.Op.LOADL, 0);
		}
		
		return null;
	}

	@Override
	public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
		ClassDecl cdecl = (ClassDecl) expr.classtype.className.decl;
		int csize = cdecl.entity.size;
		Machine.emit(Machine.Op.LOADL, -1);
		Machine.emit(Machine.Op.LOADL, csize);
		Machine.emit(Machine.Prim.newobj);
		return null;
	}

	
	public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
		expr.sizeExpr.visit(this, null);
		Machine.emit(Machine.Prim.newarr);
		return null;
	}

		
	///////////////////////////////////////////////////////////////////////////////
	//
	// REFERENCES
	//
	///////////////////////////////////////////////////////////////////////////////

	public Object visitThisRef(ThisRef ref, Object arg) {
		Machine.emit(Machine.Op.LOADA, 1, Machine.Reg.OB, 0);
		return null;
	}

	public Object visitIdRef(IdRef ref, Object arg) {
		if (ref.decl instanceof FieldDecl)
		{
			if(((FieldDecl) ref.decl).isStatic)
			{
				int d = ((UnknownValue) ref.decl.entity).address;
				Machine.emit(Machine.Op.LOAD, 1, Machine.Reg.SB, d);
			}
			
			else
			{
				int d = ((UnknownValue) ref.decl.entity).address;
				Machine.emit(Machine.Op.LOAD, 1, Machine.Reg.OB, d);
			}
		}
		
		else if (ref.decl instanceof VarDecl)
		{
			int d = ((UnknownValue) ref.decl.entity).address;
			Machine.emit(Machine.Op.LOAD, 1, Machine.Reg.LB, d);
		}
		
		else if (ref.decl instanceof ParameterDecl)
		{
			int d = ((UnknownValue) ref.decl.entity).address;
			Machine.emit(Machine.Op.LOAD, 1, Machine.Reg.LB, d);
		}
		
		return null;
	}

	public Object visitIxIdRef(IxIdRef ref, Object arg) {
	
		if (ref.decl instanceof FieldDecl)
		{
			if(((FieldDecl) ref.decl).isStatic)
			{
				int d = ((UnknownValue) ref.decl.entity).address;
				Machine.emit(Machine.Op.LOAD, 1, Machine.Reg.SB, d);
				((IxIdRef) ref).indexExpr.visit(this, null);
				Machine.emit(Machine.Prim.arrayref);
			}
			
			else
			{
				int d = ((UnknownValue) ref.decl.entity).address;
				Machine.emit(Machine.Op.LOAD, 1, Machine.Reg.OB, d);
				((IxIdRef) ref).indexExpr.visit(this, null);
				Machine.emit(Machine.Prim.arrayref);
			}
		}
		
		else if (ref.decl instanceof VarDecl)
		{
			int d = ((UnknownValue) ref.decl.entity).address;
			Machine.emit(Machine.Op.LOAD, 1, Machine.Reg.LB, d);
			((IxIdRef) ref).indexExpr.visit(this, null);
			Machine.emit(Machine.Prim.arrayref);
		}
		
		else if (ref.decl instanceof ParameterDecl)
		{
			int d = ((UnknownValue) ref.decl.entity).address;
			Machine.emit(Machine.Op.LOAD, 1, Machine.Reg.LB, d);
			((IxIdRef) ref).indexExpr.visit(this, null);
			Machine.emit(Machine.Prim.arrayref);
		}
		
		
		return null;
	}

	
	public Object visitQRef(QRef ref, Object arg) {
		if(((int) arg == 1 || (int) arg == 2) && ref_level == 1)
		{
			Declaration lDecl = ref.ref.decl;
			if (lDecl instanceof ClassDecl)
			{
				if((int) arg == 2 && ref.ref instanceof ThisRef)
				{
					Machine.emit(Machine.Op.LOADA, 1, Machine.Reg.OB, 0);
					return 0;
				}
				
				if((int) arg == 1 && ref.ref instanceof ThisRef)
				{
					int d = ((UnknownValue) ref.decl.entity).address;
					Machine.emit(Machine.Op.LOAD, 1, Machine.Reg.OB, d);
					return 0;
				}
				
				int d = ((UnknownValue) ref.decl.entity).address;
				Machine.emit(Machine.Op.LOAD, 1, Machine.Reg.SB, d);
				return -1;
			}
			
			else
			{				
				if(ref.decl instanceof FieldDecl)
				{
					if(ref.id.spelling.equals("length") && ref.ref.decl.type instanceof ArrayType)
					{
						ref_level++;
						ref.ref.visit(this,arg);
						ref_level--;
						Machine.emit(Machine.Prim.arraylen);
						return 0;
					}
					
					if(((FieldDecl) ref.decl).isStatic)
					{
						int d = ((UnknownValue) ref.decl.entity).address;
						Machine.emit(Machine.Op.LOAD, 1, Machine.Reg.SB, d);
						return -1;
					}
					
					else
					{
						if((int) arg == 2)
						{
							ref_level++;
							ref.ref.visit(this,arg);
							ref_level--;
							return 0;
						}
						
						ref_level++;
						ref.ref.visit(this,arg);
						ref_level--;
						int d = ((UnknownValue) ref.decl.entity).address;
						Machine.emit(Machine.Op.LOADL, d);
						Machine.emit(Machine.Prim.fieldref);
					}
				}
				
				else
				{
					if(ref.id.spelling.equals("length") && ref.ref.decl.type instanceof ArrayType)
					{
						ref_level++;
						ref.ref.visit(this,arg);
						ref_level--;
						Machine.emit(Machine.Prim.arraylen);
						return 0;
					}
					
					if((int) arg == 2)
					{
						ref_level++;
						ref.ref.visit(this,arg);
						ref_level--;
						return 0;
					}
					
					ref_level++;
					ref.ref.visit(this,arg);
					ref_level--;
					int d = ((UnknownValue) ref.decl.entity).address;
					Machine.emit(Machine.Op.LOADL, d);
					Machine.emit(Machine.Prim.fieldref);
					return 0;
				}
				
			}
		}
		
		else if ((int) arg == 0)
		{
			Declaration lDecl = ref.ref.decl;
			if (lDecl instanceof ClassDecl && !(ref.ref instanceof ThisRef))
			{
				int d = ((UnknownValue) ref.decl.entity).address;
				Machine.emit(Machine.Op.LOAD, 1, Machine.Reg.SB, d);
				return -1;
			}
			
			else
			{				
				if(ref.decl instanceof FieldDecl)
				{
					if(((FieldDecl) ref.decl).isStatic)
					{
						int d = ((UnknownValue) ref.decl.entity).address;
						Machine.emit(Machine.Op.LOAD, 1, Machine.Reg.SB, d);
						return -1;
					}
					
					
					else
					{
						if(ref.ref instanceof ThisRef)
						{
							Machine.emit(Machine.Op.LOADA, 1, Machine.Reg.OB, 0);
						}
						
						else
						{
							ref_level++;
							ref.ref.visit(this,arg);
							ref_level--;
						}
						
						if(ref_level > 1)
						{
							int d = ((UnknownValue) ref.decl.entity).address;
							Machine.emit(Machine.Op.LOADL, d);
							Machine.emit(Machine.Prim.fieldref);
						}
						
						else
						{
							int d = ((UnknownValue) ref.decl.entity).address;
							Machine.emit(Machine.Op.LOADL, d);
						}
						return 0;
					}
				}
				
				else
				{
					ref_level++;
					ref.ref.visit(this,arg);
					ref_level--;
					if(ref_level > 1)
					{
						int d = ((UnknownValue) ref.decl.entity).address;
						Machine.emit(Machine.Op.LOADL, d);
						Machine.emit(Machine.Prim.fieldref);
					}
					
					else
					{
						int d = ((UnknownValue) ref.decl.entity).address;
						Machine.emit(Machine.Op.LOADL, d);
					}
					return 0;
				}
				
			}
		}
		
		else if ((int) arg == 1 || (int) arg == 2)
		{
			Declaration lDecl = ref.ref.decl;
			if (lDecl instanceof ClassDecl)
			{
				if((int) arg == 2 && ref.ref instanceof ThisRef)
				{
					int d = ((UnknownValue) ref.decl.entity).address;
					Machine.emit(Machine.Op.LOAD, 1, Machine.Reg.OB, d);
					return 0;
				}
				
				if((int) arg == 1 && ref.ref instanceof ThisRef)
				{
					int d = ((UnknownValue) ref.decl.entity).address;
					Machine.emit(Machine.Op.LOAD, 1, Machine.Reg.OB, d);
					return 0;
				}
				
				int d = ((UnknownValue) ref.decl.entity).address;
				Machine.emit(Machine.Op.LOAD, 1, Machine.Reg.SB, d);
				return -1;
			}
			
			else
			{				
				if(ref.decl instanceof FieldDecl)
				{
					
					if(((FieldDecl) ref.decl).isStatic)
					{
						int d = ((UnknownValue) ref.decl.entity).address;
						Machine.emit(Machine.Op.LOAD, 1, Machine.Reg.SB, d);
						return -1;
					}
					
					
					else
					{
						ref_level++;
						ref.ref.visit(this,arg);
						ref_level--;
						int d = ((UnknownValue) ref.decl.entity).address;
						Machine.emit(Machine.Op.LOADL, d);
						Machine.emit(Machine.Prim.fieldref);
						return 0;
					}
				}
				
				else
				{
					ref_level++;
					ref.ref.visit(this,arg);
					ref_level--;
					int d = ((UnknownValue) ref.decl.entity).address;
					Machine.emit(Machine.Op.LOADL, d);
					Machine.emit(Machine.Prim.fieldref);
					return 0;
				}
				
			}
		}
		return null;
	}

	
	public Object visitIxQRef(IxQRef ref, Object arg) {
		if((int) arg == 1 && ref_level == 1)
		{
			Declaration lDecl = ref.ref.decl;
			if (lDecl instanceof ClassDecl)
			{
				int d = ((UnknownValue) ref.decl.entity).address;
				Machine.emit(Machine.Op.LOAD, 1, Machine.Reg.SB, d);
				ref.ixExpr.visit(this,null);
				Machine.emit(Machine.Prim.arrayref);
				return -1;
			}
			
			else
			{				
				if(ref.decl instanceof FieldDecl)
				{
					if(((FieldDecl) ref.decl).isStatic)
					{
						int d = ((UnknownValue) ref.decl.entity).address;
						Machine.emit(Machine.Op.LOAD, 1, Machine.Reg.SB, d);
						ref.ixExpr.visit(this,null);
						Machine.emit(Machine.Prim.arrayref);
						return -1;
					}
					
					else
					{
						ref_level++;
						ref.ref.visit(this,arg);
						ref_level--;
						int d = ((UnknownValue) ref.decl.entity).address;
						Machine.emit(Machine.Op.LOADL, d);
						Machine.emit(Machine.Prim.fieldref);
						ref.ixExpr.visit(this,null);
						Machine.emit(Machine.Prim.arrayref);
						return 0;
					}
				}
				
				else
				{
					ref_level++;
					ref.ref.visit(this,arg);
					ref_level--;
					int d = ((UnknownValue) ref.decl.entity).address;
					Machine.emit(Machine.Op.LOADL, d);
					Machine.emit(Machine.Prim.fieldref);
					ref.ixExpr.visit(this,null);
					Machine.emit(Machine.Prim.arrayref);
					return 0;
				}
				
			}
		}
		
		else if ((int) arg == 0)
		{
			Declaration lDecl = ref.ref.decl;
			if (lDecl instanceof ClassDecl)
			{
				int d = ((UnknownValue) ref.decl.entity).address;
				Machine.emit(Machine.Op.LOAD, 1, Machine.Reg.SB, d);
				ref.ixExpr.visit(this, null);
				Machine.emit(Machine.Prim.arrayref);
				return -1;
			}
			
			else
			{				
				if(ref.decl instanceof FieldDecl)
				{
					if(((FieldDecl) ref.decl).isStatic)
					{
						int d = ((UnknownValue) ref.decl.entity).address;
						Machine.emit(Machine.Op.LOAD, 1, Machine.Reg.SB, d);
						ref.ixExpr.visit(this, null);
						Machine.emit(Machine.Prim.arrayref);
						return -1;
					}
					
					
					else
					{
						ref_level++;
						ref.ref.visit(this,arg);
						ref_level--;
						if(ref_level > 1)
						{
							int d = ((UnknownValue) ref.decl.entity).address;
							Machine.emit(Machine.Op.LOADL, d);
							Machine.emit(Machine.Prim.fieldref);
							ref.ixExpr.visit(this, null);
							Machine.emit(Machine.Prim.arrayref);
						}
						
						else
						{
							int d = ((UnknownValue) ref.decl.entity).address;
							Machine.emit(Machine.Op.LOADL, d);
							Machine.emit(Machine.Prim.fieldref);
						}
						
						return 0;
					}
				}
				
				else
				{
					ref_level++;
					ref.ref.visit(this,arg);
					ref_level--;
					if(ref_level > 1)
					{
						int d = ((UnknownValue) ref.decl.entity).address;
						Machine.emit(Machine.Op.LOADL, d);
						Machine.emit(Machine.Prim.fieldref);
						ref.ixExpr.visit(this, null);
						Machine.emit(Machine.Prim.arrayref);
					}
					
					else
					{
						int d = ((UnknownValue) ref.decl.entity).address;
						Machine.emit(Machine.Op.LOADL, d);
						Machine.emit(Machine.Prim.fieldref);
					}
					
					return 0;
				}
				
			}
		}
		
		else if ((int) arg == 1 || (int) arg == 2)
		{
			Declaration lDecl = ref.ref.decl;
			if (lDecl instanceof ClassDecl)
			{
				int d = ((UnknownValue) ref.decl.entity).address;
				Machine.emit(Machine.Op.LOAD, 1, Machine.Reg.SB, d);
				ref.ixExpr.visit(this,null);
				Machine.emit(Machine.Prim.arrayref);
				return -1;
			}
			
			else
			{				
				if(ref.decl instanceof FieldDecl)
				{
					if(((FieldDecl) ref.decl).isStatic)
					{
						int d = ((UnknownValue) ref.decl.entity).address;
						Machine.emit(Machine.Op.LOAD, 1, Machine.Reg.SB, d);
						ref.ixExpr.visit(this,null);
						Machine.emit(Machine.Prim.arrayref);
						return -1;
					}
					
					
					else
					{
						ref_level++;
						ref.ref.visit(this,arg);
						ref_level--;
						int d = ((UnknownValue) ref.decl.entity).address;
						Machine.emit(Machine.Op.LOADL, d);
						Machine.emit(Machine.Prim.fieldref);
						ref.ixExpr.visit(this,null);
						Machine.emit(Machine.Prim.arrayref);
						return 0;
					}
				}
				
				else
				{
					ref_level++;
					ref.ref.visit(this,arg);
					ref_level--;
					int d = ((UnknownValue) ref.decl.entity).address;
					Machine.emit(Machine.Op.LOADL, d);
					Machine.emit(Machine.Prim.fieldref);
					ref.ixExpr.visit(this,null);
					Machine.emit(Machine.Prim.arrayref);
					return 0;
				}
			}
		}
		return null;
	}

	///////////////////////////////////////////////////////////////////////////////
	//
	// TERMINALS
	//
	///////////////////////////////////////////////////////////////////////////////
	
	public Object visitIdentifier(Identifier id, Object arg) {
		return null;
	}

	public Object visitOperator(Operator op, Object arg) {
		return null;
	}

	
	public Object visitIntLiteral(IntLiteral num, Object arg) {
		return null;
	}

	public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
		return null;
	}

	public Object visitNullLiteral(NullLiteral lit, Object arg) {
		return null;
	}

}
