package miniJava.ContextualAnalyzer;

import java.util.ArrayList;
import java.util.HashMap;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;

public class IdentifyingVisitor implements Visitor<Object, Object> {

	public IdentificationTable table = new IdentificationTable();
	public boolean preVisit = true;
	public boolean staticCheck = false;
	
	public void Identify(AST ast){
	        ast.visit(this, null);
	}   
	
	public Object visitPackage(Package prog, Object arg) {
		table.openScope();

		SourcePosition sp = new SourcePosition(0);
		
		FieldDeclList flist = new FieldDeclList();
		MethodDeclList mlist = new MethodDeclList();
		StatementList slist = new StatementList();
		ParameterDeclList plist = new ParameterDeclList();
		BaseType ptype = new BaseType(TypeKind.INT, sp);
		ParameterDecl pdecl = new ParameterDecl(ptype, "n", sp);
		plist.add(pdecl);
		
		BaseType mtype = new BaseType(TypeKind.VOID, sp);
		FieldDecl fdecl = new FieldDecl(false, false, mtype, "println", sp);
		MethodDecl mdecl = new MethodDecl(fdecl, plist, slist, sp);
		mlist.add(mdecl);
		ClassDecl cdecl = new ClassDecl("_PrintStream", flist, mlist, sp);
		
		FieldDeclList flist2 = new FieldDeclList();
		MethodDeclList mlist2 = new MethodDeclList();
		Token ftoken = new Token((byte) 0, "_PrintStream", sp);
		Identifier fid = new Identifier(ftoken);
		fid.decl =  cdecl;
		ClassType ftype = new ClassType(fid, sp);
		FieldDecl fdecl2 = new FieldDecl(false, true, ftype, "out", sp);
		flist2.add(fdecl2);
		ClassDecl cdecl2 = new ClassDecl("System", flist2, mlist2, sp);
		
		FieldDeclList flist3 = new FieldDeclList();
		MethodDeclList mlist3 = new MethodDeclList();
		ClassDecl cdecl3 = new ClassDecl("String", flist3, mlist3, sp);
		
		table.enterID("_PrintStream", cdecl);
		table.enterID("System", cdecl2);
		table.enterID("String", cdecl3);
		

	    ClassDeclList cl = prog.classDeclList;
	    table.openScope();
	    
	    for (ClassDecl c: prog.classDeclList)
	    {
	    	table.enterID(c.name, c);
	    }
	    
	    for (ClassDecl c: prog.classDeclList)
	    {
	    	table.openScope();
	    	table.enterID("this", c);
	    	c.visit(this, null);
	    	table.closeScope();
	    }
	    
	    
	    for (ClassDecl c: prog.classDeclList)
	    {
	    	table.openScope();
	    	table.enterID("this", c);
	    	c.visit(this, null);
		    preVisit = false;
		    c.visit(this, null);
	    	table.closeScope();
	    	preVisit = true;
	    }
	    
	    table.closeScope();
	    table.closeScope();
	    boolean mainPresent = false;
	    for (ClassDecl c: prog.classDeclList)
	    {
	    	for (MethodDecl m: c.methodDeclList)
	    	{
	    		if(m.name.equals("main") && m.isStatic && !(m.isPrivate) && m.type.typeKind == TypeKind.VOID && m.parameterDeclList.size() == 1)
	    		{
	    			TypeDenoter type = m.parameterDeclList.get(0).type;
	    			if(type instanceof ArrayType)
	    			{
	    				TypeDenoter etype = ((ArrayType) type).eltType;
	    				if(etype instanceof ClassType)
	    				{
	    					if(((ClassType) etype).className.spelling.equals("String"))
	    					{
	    						if(mainPresent)
	    						{
	    							System.out.println("*** " + c.posn.toString() + " There must be only one main method ***");
	    							System.exit(4);
	    						}
	    						else
	    						{
	    							mainPresent = true;
	    						}
	    					}
	    				}
	    			}
	    		}
	    	}
	    }
	    
	    if(!mainPresent)
	    {
	    	System.out.println("*** " + prog.posn.toString() + " There must be one main method ***");
			System.exit(4);
	    }
	    return null;
	}
	    
    
	///////////////////////////////////////////////////////////////////////////////
	//
	// DECLARATIONS
	//
	///////////////////////////////////////////////////////////////////////////////
    
    public Object visitClassDecl(ClassDecl clas, Object arg){
        if(preVisit) 
        {
        	for (FieldDecl f: clas.fieldDeclList)
            	f.visit(this, null);
            for (MethodDecl m: clas.methodDeclList)
            	m.visit(this, null); 	
        }
    	
        else
        {
        	for (FieldDecl f: clas.fieldDeclList)
            	f.visit(this, null);
        	for (MethodDecl m: clas.methodDeclList)
             	m.visit(this, null);
        }
         
        return null;
    }
    
    public Object visitFieldDecl(FieldDecl f, Object arg){ 
    	if(preVisit)
    	{
	    	f.type.visit(this, null);
	    	table.enterID(f.name, f);
    	}
        return null;
    }
    
    public Object visitMethodDecl(MethodDecl m, Object arg){
    	if(preVisit)
    	{
    		m.type.visit(this, null);
            table.enterID(m.name, m);
            return null;
    	}
    	
        if(m.isStatic)
        {
        	staticCheck = true;
        }
    	
    	table.openScope();
    	ParameterDeclList pdl = m.parameterDeclList;
        for (ParameterDecl pd: pdl) {
            pd.visit(this, null);
        }
        table.openScope();
        StatementList sl = m.statementList;
        
        if((sl.size() == 0 || !(sl.get(sl.size()-1) instanceof ReturnStmt)) && m.type.typeKind != TypeKind.VOID)
        {
        	System.out.println("*** " + m.posn.toString() + " Non-void method must have a final return statement ***");
			System.exit(4);
        }
        
        else if ((sl.size() == 0 || !(sl.get(sl.size()-1) instanceof ReturnStmt)) && m.type.typeKind == TypeKind.VOID)
        {
        	sl.add(new ReturnStmt(null, null));
        }
        
        for (Statement s: sl) {
            s.visit(this, null);
        }
        table.closeScope();
        table.closeScope();
        staticCheck = false;
        return null;
    }
    
    public Object visitParameterDecl(ParameterDecl pd, Object arg){
        pd.type.visit(this, null);
        table.enterID(pd.name, pd);
        return null;
    } 
    
    public Object visitVarDecl(VarDecl vd, Object arg){
        vd.type.visit(this, null);
        table.enterID(vd.name, vd);
        return null;
    }
 
	
	///////////////////////////////////////////////////////////////////////////////
	//
	// TYPES
	//
	///////////////////////////////////////////////////////////////////////////////
    
    public Object visitBaseType(BaseType type, Object arg){
        return null;
    }
    
    public Object visitClassType(ClassType ct, Object arg){
    	if (arg instanceof String)
    	{
    		if (ct.className.spelling.equals((String) arg))
    		{
    			System.out.println("*** " + ct.posn.toString() + " Identifier cannot be referenced in initializing expression for that identifier ***");
    			System.exit(4);
    		}
    	}
    	HashMap<String, Declaration> classScope1 = table.idTable.get(1);
    	if((classScope1.containsKey(ct.className.spelling)))
    	{
    		ct.className.decl = classScope1.get(ct.className.spelling);
    		return null;
    	}
    	
    	HashMap<String, Declaration> classScope0 = table.idTable.get(0);
    	if((classScope0.containsKey(ct.className.spelling)))
    	{
    		ct.className.decl = classScope0.get(ct.className.spelling);
    		return null;
    	}
    	
    	else
    	{
    		System.out.println("*** " + ct.posn.toString() + " Class not found ***");
			System.exit(4);
    	}
			
    	return null;
        
    }
    
    public Object visitArrayType(ArrayType type, Object arg){
        if (arg instanceof String)
        {
        	type.eltType.visit(this, arg);
        }
        else
        {
        	type.eltType.visit(this, null);
        }
        return null;
    }
    
	
	///////////////////////////////////////////////////////////////////////////////
	//
	// STATEMENTS
	//
	///////////////////////////////////////////////////////////////////////////////

    public Object visitBlockStmt(BlockStmt stmt, Object arg){
    	table.openScope();
        StatementList sl = stmt.sl;
        for (Statement s: sl) {
        	s.visit(this, null);
        }
        table.closeScope();
        return null;
    }
    
    public Object visitVardeclStmt(VarDeclStmt stmt, Object arg){
        stmt.varDecl.visit(this, null);	
        String vdname = stmt.varDecl.name;
        stmt.initExp.visit(this, vdname);
        return null;
    }
    
    public Object visitAssignStmt(AssignStmt stmt, Object arg){
        stmt.ref.visit(this, null);
        stmt.val.visit(this, null);
        return null;
    }
        
    public Object visitCallStmt(CallStmt stmt, Object arg){
        stmt.methodRef.visit(this, null);
        ExprList al = stmt.argList;
        for (Expression e: al) {
            e.visit(this, null);
        }
        return null;
    }
    
    public Object visitReturnStmt(ReturnStmt stmt, Object arg){
         if (stmt.returnExpr != null)
            stmt.returnExpr.visit(this, null);
        return null;
    }
    
    public Object visitIfStmt(IfStmt stmt, Object arg){
        stmt.cond.visit(this, null);
        if (stmt.thenStmt instanceof VarDeclStmt)
        {
        	System.out.println("*** " + stmt.thenStmt.posn.toString() +  " A variable declaration cannot be the solitary statement in a branch of a conditional statement ***");
        	System.exit(4);
        }
        
        if (stmt.elseStmt instanceof VarDeclStmt)
        {
        	System.out.println("*** " + stmt.elseStmt.posn.toString() +  " A variable declaration cannot be the solitary statement in a branch of a conditional statement ***");
        	System.exit(4);
        }
        stmt.thenStmt.visit(this, null);
        if (stmt.elseStmt != null)
            stmt.elseStmt.visit(this, null);
        return null;
    }
    
    public Object visitWhileStmt(WhileStmt stmt, Object arg){
        stmt.cond.visit(this, null);
        stmt.body.visit(this, null);
        if (stmt.body instanceof VarDeclStmt)
        {
        	System.out.println("*** " + stmt.body.posn.toString() +  " A variable declaration cannot be the solitary statement in a branch of a conditional statement ***");
        	System.exit(4);
        }
        return null;
    }
    

	///////////////////////////////////////////////////////////////////////////////
	//
	// EXPRESSIONS
	//
	///////////////////////////////////////////////////////////////////////////////

    public Object visitUnaryExpr(UnaryExpr expr, Object arg){
		expr.operator.visit(this, arg);

    	if (arg instanceof String)
    	{
            expr.expr.visit(this, arg);
    	}
    	
    	else
    	{
            expr.expr.visit(this, null);
    	}
    	
        return null;
    }
    
    public Object visitBinaryExpr(BinaryExpr expr, Object arg){
        expr.operator.visit(this, null);
        
        if (arg instanceof String)
    	{
            expr.left.visit(this, arg);
            expr.right.visit(this, arg);
    	}
    	
    	else
    	{
    		 expr.left.visit(this, null);
    	     expr.right.visit(this, null);    	
    	}
       
        return null;
    }
    
    public Object visitRefExpr(RefExpr expr, Object arg){
        expr.ref.visit(this, arg);
        return null;
        
    }
    
    public Object visitCallExpr(CallExpr expr, Object arg){
        expr.functionRef.visit(this, null);
        ExprList al = expr.argList;
        if (arg instanceof String)
        {
        	for (Expression e: al) {
                e.visit(this, arg);
            }
        }
        
        else
        {
        	for (Expression e: al) {
                e.visit(this, null);
            }
        }
        	
   
        return null;
    }
    
    public Object visitLiteralExpr(LiteralExpr expr, Object arg){
        expr.lit.visit(this, null);
        return null;
    }
 
    public Object visitNewArrayExpr(NewArrayExpr expr, Object arg){
        expr.eltType.visit(this, arg);
        if (arg instanceof String)
        {
            expr.sizeExpr.visit(this, arg);
        }
        
        else
        {
            expr.sizeExpr.visit(this, null);
        }
        return null;
    }
    
    public Object visitNewObjectExpr(NewObjectExpr expr, Object arg){
        expr.classtype.visit(this, arg);
        return null;
    }
    

	///////////////////////////////////////////////////////////////////////////////
	//
	// REFERENCES
	//
	///////////////////////////////////////////////////////////////////////////////
	
    public Object visitThisRef(ThisRef ref, Object arg) {
    	if(staticCheck)
    	{
    		System.out.println("*** " + ref.posn.toString() + " Cannot refer to an instance in a static context ***");
			System.exit(4);
    	}
    	
    	Declaration decl = table.getDecl("this", false);
    	ref.decl = decl;
    	return decl;
    }
    
    public Object visitIdRef(IdRef ref, Object arg) {
    	Declaration decl = (Declaration) ref.id.visit(this, null);
    	if (arg instanceof String)
        {
        	if (decl != null)
        	{
        		if (decl.name.equals((String) arg))
        		{
        			System.out.println("*** " + ref.posn.toString() + " Identifier cannot be referenced in initializing expression for that identifier ***");
        			System.exit(4);
        		}
        	}
        }
    	ref.decl = decl;
    	return decl;
    }
    
    public Object visitIxIdRef(IxIdRef ir, Object arg) {
    	Declaration decl = (Declaration) ir.id.visit(this, null);
    	if (arg instanceof String)
        {
        	if (decl != null)
        	{
        		if (decl.name.equals((String) arg))
        		{
        			System.out.println("*** " + ir.posn.toString() + " Identifier cannot be referenced in initializing expression for that identifier ***");
        			System.exit(4);
        		}
        	}
        }

    	if (arg instanceof String)
    	{
        	ir.indexExpr.visit(this, arg);

    	}
    	else
    	{
        	ir.indexExpr.visit(this, null);

    	}
    	
    	ir.decl = decl;
    	return decl;
    }
    
    public Object visitQRef(QRef qr, Object arg) {
    	Declaration declL = null;
		Declaration declR = null;
    	if (arg instanceof String) 
    	{
    		declL = (Declaration) qr.ref.visit(this, arg);
    	}
    	
    	else
    	{
        	declL = (Declaration) qr.ref.visit(this, null);
    	}
    	
    	if (declL instanceof ClassDecl)
    	{
    		FieldDeclList flist= ((ClassDecl) declL).fieldDeclList;
    		MethodDeclList mlist= ((ClassDecl) declL).methodDeclList;

    		boolean found = false;
    		for(int i = 0; i < flist.size(); i++)
    		{
    			if(qr.id.spelling.equals(flist.get(i).name))
    			{
    				found = true;
    				qr.id.decl = flist.get(i);
    				declR = (FieldDecl) qr.id.decl;
    				qr.decl = qr.id.decl;
    				FieldDecl declR2 = (FieldDecl) declR;
    				if(declR2.isPrivate && !(table.getDecl("this", false).name.equals(declL.name)))
    				{
    					System.out.println("*** " + qr.posn.toString() + " Identifier not visible ***");
    					System.exit(4);
    				}
    				
    				if(!(declR2.isStatic) && !(qr.ref instanceof ThisRef))
    				{
    					System.out.println("*** " + qr.posn.toString() + " Identifier not accessible ***");
    					System.exit(4);
    				}
 
    				break;
    			}
    		}
    		if (!found)
    		{
    			for(int i = 0; i < mlist.size(); i++)
    			{
    				if(qr.id.spelling.equals(mlist.get(i).name))
    				{
    					found = true;
    					qr.id.decl = mlist.get(i);
    					declR = (MethodDecl) qr.id.decl;
    					qr.decl = qr.id.decl;
    					MethodDecl declR2 = (MethodDecl) declR;
    					
    					if(declR2.isPrivate && !(table.getDecl("this", false).name.equals(declL.name)))
    					{
    						System.out.println("*** " + qr.posn.toString() + " Identifier not visible ***");
    						System.exit(4);
    					}
    					
    					if(!(declR2.isStatic) && !(qr.ref instanceof ThisRef))
        				{
        					System.out.println("*** " + qr.posn.toString() + " Identifier not accessible ***");
        					System.exit(4);
        				}
    					
    					break;
    				}
    			}
    		}
    		
    		if (!found)
    		{
    			System.out.println("*** " + qr.posn.toString() + " Reference has no such member ***");
    			System.exit(4);
    		}
    		
    	}
    	
    	else if(declL instanceof VarDecl || declL instanceof ParameterDecl || declL instanceof FieldDecl)
    	{
    		TypeDenoter type = declL.type;
    		ClassType ctype = null;
    		if ((type instanceof ClassType))
    		{
    			ctype = (ClassType) declL.type;
    			
    		}
    		
    		else if(type instanceof ArrayType)
    		{
    			if(qr.id.spelling.equals("length") && !(qr.ref instanceof IxIdRef || qr.ref instanceof IxQRef))
    			{
    				return null;
    			}
    			if(!(((ArrayType) type).eltType instanceof ClassType))
    			{
    				System.out.println("*** " + qr.posn.toString() + " Reference has no such member ***");
    				System.exit(4);
    			}
    			
    			else
    			{
    				ctype = (ClassType) ((ArrayType) type).eltType;
    			}
    		}
    		

    		
    		else
    		{
    			System.out.println("*** " + qr.posn.toString() + " Reference has no such member ***");
				System.exit(4);
    		}
    		
    		
    		HashMap<String, Declaration> classScope1 = table.idTable.get(1);
    		boolean bad = true;
    		if((classScope1.containsKey(ctype.className.spelling)))
        	{
        		ctype.className.decl = classScope1.get(ctype.className.spelling);
        		bad = false;
        	}
        	
        	HashMap<String, Declaration> classScope0 = table.idTable.get(0);
        	if((classScope0.containsKey(ctype.className.spelling)) && bad)
        	{
        		ctype.className.decl = classScope0.get(ctype.className.spelling);
        		bad = false;
        	}
        	
        	if(bad)
        	{
        		System.out.println("*** " + ctype.posn.toString() + " Class not found ***");
    			System.exit(4);
        	}    		
        	FieldDeclList flist= ((ClassDecl)ctype.className.decl).fieldDeclList;
    		MethodDeclList mlist= ((ClassDecl)ctype.className.decl).methodDeclList;

    		boolean found = false;
    		for(int i = 0; i < flist.size(); i++)
    		{
    			if(qr.id.spelling.equals(flist.get(i).name))
    			{
    				found = true;
    				qr.id.decl = flist.get(i);
    				qr.decl = qr.id.decl;
    				declR = (FieldDecl) qr.id.decl;
    				FieldDecl declR2 = (FieldDecl) declR;
    				if(declR2.isPrivate && !(table.getDecl("this", false).name.equals(ctype.className.spelling)))
    				{
    					System.out.println("*** " + qr.posn.toString() + " Identifier not visible ***");
    					System.exit(4);
    				}
 
    				break;
    			}
    		}
    		if (!found)
    		{
    			for(int i = 0; i < mlist.size(); i++)
    			{
    				if(qr.id.spelling.equals(mlist.get(i).name))
    				{
    					found = true;
    					qr.id.decl = mlist.get(i);
    					declR = (MethodDecl) qr.id.decl;
    					qr.decl = qr.id.decl;
    					MethodDecl declR2 = (MethodDecl) declR;
    					
    					if(declR2.isPrivate && !(table.getDecl("this", false).name.equals(ctype.className.spelling)))
    					{
    						System.out.println("*** " + qr.posn.toString() + " Identifier not visible ***");
    						System.exit(4);
    					}
    					
    					break;
    				}
    			}
    		}
    		
    		if (!found)
    		{
    			System.out.println("*** " + qr.posn.toString() + " Reference has no such member ***");
    			System.exit(4);
    		}
    		
    	}
    	
    	else
    	{
    		System.out.println("*** " + qr.posn.toString() + " Reference has no such member ***");
			System.exit(4);
    	}
			
		return declR;
	}
    
    public Object visitIxQRef(IxQRef qr, Object arg) {
    	Declaration declL = null;
		Declaration declR = null;
    	if (arg instanceof String) 
    	{
    		declL = (Declaration) qr.ref.visit(this, arg);
    	}
    	
    	else
    	{
        	declL = (Declaration) qr.ref.visit(this, null);
    	}
    	
    	if (declL instanceof ClassDecl)
    	{
    		FieldDeclList flist= ((ClassDecl) declL).fieldDeclList;
    		MethodDeclList mlist= ((ClassDecl) declL).methodDeclList;

    		boolean found = false;
    		for(int i = 0; i < flist.size(); i++)
    		{
    			if(qr.id.spelling.equals(flist.get(i).name))
    			{
    				found = true;
    				qr.id.decl = flist.get(i);
    				declR = (FieldDecl) qr.id.decl;
    				qr.decl = qr.id.decl;
    				FieldDecl declR2 = (FieldDecl) declR;
    				if(declR2.isPrivate && !(table.getDecl("this", false).name.equals(declL.name)))
    				{
    					System.out.println("*** " + qr.posn.toString() + " Identifier not visible ***");
    					System.exit(4);
    				}
    				
    				if(!(declR2.isStatic) && !(qr.ref instanceof ThisRef))
    				{
    					System.out.println("*** " + qr.posn.toString() + " Identifier not accessible ***");
    					System.exit(4);
    				}
 
    				break;
    			}
    		}
    		if (!found)
    		{
    			for(int i = 0; i < mlist.size(); i++)
    			{
    				if(qr.id.spelling.equals(mlist.get(i).name))
    				{
    					found = true;
    					qr.id.decl = flist.get(i);
    					declR = (MethodDecl) qr.id.decl;
    					qr.decl = qr.id.decl;
    					MethodDecl declR2 = (MethodDecl) declR;
    					
    					if(declR2.isPrivate && !(table.getDecl("this", false).name.equals(declL.name)))
    					{
    						System.out.println("*** " + qr.posn.toString() + " Identifier not visible ***");
    						System.exit(4);
    					}
    					
    					if(!(declR2.isStatic))
        				{
        					System.out.println("*** " + qr.posn.toString() + " Identifier not accessible ***");
        					System.exit(4);
        				}
    					
    					break;
    				}
    			}
    		}
    		
    		if (!found)
    		{
    			System.out.println("*** " + qr.posn.toString() + " Reference has no such member ***");
    			System.exit(4);
    		}
    		
    	}
    	
    	else if(declL instanceof VarDecl || declL instanceof ParameterDecl || declL instanceof FieldDecl || declL instanceof MethodDecl)
    	{
    		TypeDenoter type = declL.type;
    		ClassType ctype = null;
    		if ((type instanceof ClassType))
    		{
    			ctype = (ClassType) declL.type;
    			
    		}
    		else if(type instanceof ArrayType)
    		{
    			if(qr.id.spelling.equals("length") && !(qr.ref instanceof IxIdRef || qr.ref instanceof IxQRef))
    			{
    				System.out.println("*** " + qr.posn.toString() + " Cannot index non-array type ***");
    				System.exit(4);
    			}
    			
    			if(!(((ArrayType) type).eltType instanceof ClassType))
    			{
    				System.out.println("*** " + qr.posn.toString() + " Reference has no such member ***");
    				System.exit(4);
    			}
    			
    			else
    			{
    				ctype = (ClassType) ((ArrayType) type).eltType;
    			}
    		}
    		
    		else
    		{
    			System.out.println("*** " + qr.posn.toString() + " Reference has no such member ***");
				System.exit(4);
    		}
    		HashMap<String, Declaration> classScope1 = table.idTable.get(1);
    		boolean bad = true;
    		if((classScope1.containsKey(ctype.className.spelling)))
        	{
        		ctype.className.decl = classScope1.get(ctype.className.spelling);
        		bad = false;
        	}
        	
        	HashMap<String, Declaration> classScope0 = table.idTable.get(0);
        	if((classScope0.containsKey(ctype.className.spelling)) && bad)
        	{
        		ctype.className.decl = classScope0.get(ctype.className.spelling);
        		bad = false;
        	}
        	
        	if(bad)
        	{
        		System.out.println("*** " + ctype.posn.toString() + " Class not found ***");
    			System.exit(4);
        	}   
        	FieldDeclList flist= ((ClassDecl)ctype.className.decl).fieldDeclList;
    		MethodDeclList mlist= ((ClassDecl)ctype.className.decl).methodDeclList;


    		boolean found = false;
    		for(int i = 0; i < flist.size(); i++)
    		{
    			if(qr.id.spelling.equals(flist.get(i).name))
    			{
    				found = true;
    				qr.id.decl = flist.get(i);
    				declR = (FieldDecl) qr.id.decl;
    				qr.decl = qr.id.decl;
    				FieldDecl declR2 = (FieldDecl) declR;
    				if(declR2.isPrivate && !(table.getDecl("this", false).name.equals(ctype.className.spelling)))
    				{
    					System.out.println("*** " + qr.posn.toString() + " Identifier not visible ***");
    					System.exit(4);
    				}
 
    				break;
    			}
    		}
    		if (!found)
    		{
    			for(int i = 0; i < mlist.size(); i++)
    			{
    				if(qr.id.spelling.equals(mlist.get(i).name))
    				{
    					found = true;
    					qr.id.decl = mlist.get(i);
    					declR = (MethodDecl) qr.id.decl;
    					qr.decl = qr.id.decl;
    					MethodDecl declR2 = (MethodDecl) declR;
    					
    					if(declR2.isPrivate && !(table.getDecl("this", false).name.equals(ctype.className.spelling)))
    					{
    						System.out.println("*** " + qr.posn.toString() + " Identifier not visible ***");
    						System.exit(4);
    					}
    					
    					break;
    				}
    			}
    		}
    		
    		if (!found)
    		{
    			System.out.println("*** " + qr.posn.toString() + " Reference has no such member ***");
    			System.exit(4);
    		}
    		
    	}
    	
    	else
    	{
    		System.out.println("*** " + qr.posn.toString() + " Reference has no such member ***");
			System.exit(4);
    	}
		if (arg instanceof String)
		{
	    	qr.ixExpr.visit(this, arg);

		}
		else
		{
			qr.ixExpr.visit(this, null);
		}
		return declR;
    }
    
    
   

    
    
	///////////////////////////////////////////////////////////////////////////////
	//
	// TERMINALS
	//
	///////////////////////////////////////////////////////////////////////////////
    public boolean errorPresent = false;
    public Object visitIdentifier(Identifier id, Object arg){
    	Declaration decl = table.getDecl(id.spelling, staticCheck);
    	
    	if (decl != null)
    	{
    		if(staticCheck)
    		{
    			if(decl.name == "ThisIsAStaticReferenceErrorInAStaticMethod")
        		{
        			System.out.println("*** " + id.posn.toString() +" Static method cannot make reference to this non-static enclosing class member *** ");
    				System.exit(4);
        		}
    		}
    		
    		id.decl = decl;
    	}
    	else
    	{
    		System.out.println("*** " + id.posn.toString() + " Identifier not found ***");
    		System.exit(4);
    	}
    	
        return decl;
    }
    
    public Object visitOperator(Operator op, Object arg){
        return null;
    }
    
    public Object visitIntLiteral(IntLiteral num, Object arg){
        return null;
    }
    
    public Object visitBooleanLiteral(BooleanLiteral bool, Object arg){
        return null;
    }
    
    public Object visitNullLiteral(NullLiteral lit, Object arg){
        return null;
    }
}
