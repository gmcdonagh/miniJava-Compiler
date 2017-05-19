package miniJava.SyntacticAnalyzer;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;

public class Parser 
{
	private Scanner scanner;
	private ErrorReporter reporter;
	private Token token;
	private boolean trace = true;
	private int lineStart;
	private int lineFinish;

	public Parser(Scanner scanner, ErrorReporter reporter) {
		this.scanner = scanner;
		this.reporter = reporter;
	}
	
	public AST parse()
	{
		try
		{
			token = scanner.scan();
			return parseProgram();
		}
		catch (SyntaxError e)
		{}
		return null;
	}
	
	private String parseVisibility()
	{
		switch(token.kind)
		{
			case Token.PUBLIC:
			{
				acceptIt();
				return "public";
			}
			case Token.PRIVATE:
			{
				acceptIt();
				return "private";
			}
			
			default:
			{
				return "none";
			}
		}
	}
	
	private String parseAccess()
	{
		switch(token.kind)
		{
			case Token.STATIC:
			{
				acceptIt();
				return "static";
			}
			
			default:
			{
				return "none";
			}
		}
	}
	
	private TypeDenoter parseType()
	{
		SourcePosition sp =  new SourcePosition(0);
		sp.lineStart = token.posn.lineNum;
		switch(token.kind)
		{
			case Token.BOOLEAN:
			{
				sp.lineFinish = token.posn.lineNum;
				acceptIt();
				return new BaseType(TypeKind.BOOLEAN, sp);
			}
			
			case Token.INT:
			{
				sp.lineFinish = token.posn.lineNum;
				acceptIt();
				if(token.kind == Token.LBRACKET)
				{
					acceptIt();
					sp.lineFinish = token.posn.lineNum;
					accept(Token.RBRACKET);
					BaseType type1 = new BaseType(TypeKind.INT, sp);
					ArrayType type2 = new ArrayType(type1, sp);
					return type2;
					
				}
				return new BaseType(TypeKind.INT, sp);
			}
			
			case Token.IDENTIFIER:
			{
				sp.lineFinish = token.posn.lineNum;
				Identifier id = new Identifier(token);
				acceptIt();
				if(token.kind == Token.LBRACKET)
				{
					acceptIt();
					sp.lineFinish = token.posn.lineNum;
					accept(Token.RBRACKET);
					ClassType type1 = new ClassType(id, sp);
					ArrayType type2 = new ArrayType(type1, sp);
					return type2;
				}
				return new ClassType(id, sp);
			}		
			
			default:
			{
				parseError("Invalid Term - expecting BOOLEAN or INT or IDENTIFIER but found " + token.kind);
				return new BaseType(TypeKind.ERROR, null);
			}
		}
	}
	
	private ParameterDeclList parseParameterList()
	{
		SourcePosition sp =  new SourcePosition(0);
		sp.lineStart = token.posn.lineNum;
		ParameterDeclList plist = new ParameterDeclList();
		TypeDenoter type = parseType();
		String name = "";
		if(token.kind == Token.IDENTIFIER)
		{
			name = token.spelling;
			sp.lineFinish = token.posn.lineNum;
			acceptIt();
		}
		
		else
		{
			accept(Token.IDENTIFIER);
		}
		
		ParameterDecl pd = new ParameterDecl(type,name,sp);
		plist.add(pd);
		
		while(token.kind == Token.COMMA)
		{
			SourcePosition sp2 =  new SourcePosition(0);
			sp2.lineStart = token.posn.lineNum;
			acceptIt();
			type = parseType();
			if(token.kind == Token.IDENTIFIER)
			{
				sp2.lineFinish = token.posn.lineNum;
				name = token.spelling;
				acceptIt();
			}
			
			else
			{
				accept(Token.IDENTIFIER);
			}
			
			pd = new ParameterDecl(type,name,sp2);
			plist.add(pd);
		}
		return plist;
	}
	
	private ExprList parseArgumentList()
	{
		ExprList elist =  new ExprList();
		Expression e = parseExpression();
		elist.add(e);
		while(token.kind == Token.COMMA)
		{
			acceptIt();
			e = parseExpression();
			elist.add(e);
		}
		return elist;
	}
	
	private Reference parseReference()
	{
		SourcePosition sp = new SourcePosition(0);
		sp.lineStart = token.posn.lineNum;
		Reference ref;
		switch(token.kind)
		{
			case Token.THIS:
			{
				sp.lineFinish = token.posn.lineNum;
				ref = new ThisRef(sp);
				acceptIt();
				break;
			}
			
			case Token.IDENTIFIER:
			{
				Token t = token;
				Identifier id =  new Identifier(t);
				sp.lineFinish = token.posn.lineNum;
				acceptIt();
				if(token.kind == Token.LBRACKET)
				{
					acceptIt();
					Expression e = parseExpression();
					sp.lineFinish = token.posn.lineNum;
					accept(Token.RBRACKET);
					ref = new IxIdRef(id, e, sp);
					break;
				}
				ref = new IdRef(id, sp);
				break;
			}
			
			default:
			{
				parseError("Invalid Term - expecting THIS or IDENTIFIER but found " + token.kind);
				return null;
			}
		}
		
		while(token.kind == Token.PERIOD)
		{
			acceptIt();
			Token t = null;
			if (token.kind == Token.IDENTIFIER)
			{
				sp.lineFinish = token.posn.lineNum;
				t = token;
				acceptIt();
			}
			
			else
			{
				accept(Token.IDENTIFIER);

			}
			
			Identifier id = new Identifier(t);
			
			if(token.kind == Token.LBRACKET)
			{
				acceptIt();
				Expression e = parseExpression();
				sp.lineFinish = token.posn.lineNum;
				accept(Token.RBRACKET);
				ref = new IxQRef(ref, id, e, sp);
				continue;
			}
			ref = new QRef(ref, id, sp);
		}
		
		return ref;
	}
	
	private Expression parseExpression()
	{
		SourcePosition sp =  new SourcePosition(0);
		sp.lineStart = token.posn.lineNum;
		Expression e1 = parseExpressionA();
		while (token.kind == Token.OR)
		{
			Operator o =  new Operator(token);
			acceptIt();
			Expression e2 = parseExpressionA();
			sp.lineFinish = token.posn.lineNum;
			e1 = new BinaryExpr(o,e1,e2, sp);
		}
		return e1;
	}
	
	private Expression parseExpressionA()
	{
		SourcePosition spA =  new SourcePosition(0);
		spA.lineStart = token.posn.lineNum;
		Expression Ae1 = parseExpressionB();
		while (token.kind == Token.AND)
		{
			Operator o =  new Operator(token);
			acceptIt();
			Expression Ae2 = parseExpressionB();
			spA.lineFinish = token.posn.lineNum;
			Ae1 = new BinaryExpr(o,Ae1,Ae2, spA);
		}
		return Ae1;
	}
	
	private Expression parseExpressionB()
	{
		SourcePosition spB =  new SourcePosition(0);
		spB.lineStart = token.posn.lineNum;
		Expression Be1 = parseExpressionC();
		while (token.kind == Token.ECOMPARISON || token.kind == Token.NECOMPARISON)
		{
			Operator o =  new Operator(token);
			acceptIt();
			Expression Be2 = parseExpressionC();
			spB.lineFinish = token.posn.lineNum;
			Be1 = new BinaryExpr(o,Be1,Be2, spB);
		}
		return Be1;
	}
	
	private Expression parseExpressionC()
	{
		SourcePosition spC =  new SourcePosition(0);
		spC.lineStart = token.posn.lineNum;
		Expression Ce1 = parseExpressionD();
		while (token.kind == Token.LT || token.kind == Token.LTE || token.kind == Token.GTE || token.kind == Token.GT)
		{
			Operator o =  new Operator(token);
			acceptIt();
			Expression Ce2 = parseExpressionD();
			spC.lineFinish = token.posn.lineNum;
			Ce1 = new BinaryExpr(o,Ce1,Ce2, spC);
		}
		return Ce1;
	}
	
	private Expression parseExpressionD()
	{
		SourcePosition spD =  new SourcePosition(0);
		spD.lineStart = token.posn.lineNum;
		Expression De1 = parseExpressionF();
		while (token.kind == Token.PLUS|| token.kind == Token.MINUS)
		{
			Operator o =  new Operator(token);
			acceptIt();
			Expression De2 = parseExpressionF();
			spD.lineFinish = token.posn.lineNum;
			De1 = new BinaryExpr(o,De1,De2, spD);
		}
		return De1;
	}
	
	private Expression parseExpressionF()
	{
		SourcePosition spF =  new SourcePosition(0);
		spF.lineStart = token.posn.lineNum;
		Expression Fe1 = parseExpressionG();
		while (token.kind == Token.TIMES || token.kind == Token.DIVIDE)
		{
			Operator o =  new Operator(token);
			acceptIt();
			Expression Fe2 = parseExpressionG();
			spF.lineFinish = token.posn.lineNum;
			Fe1 = new BinaryExpr(o,Fe1,Fe2, spF);
		}
		return Fe1;
	}	
	
	private Expression parseExpressionG()
	{
		SourcePosition spG =  new SourcePosition(0);
		spG.lineStart = token.posn.lineNum;
		Expression Ge = null;
		switch(token.kind)
		{
			case Token.NULL:
			{
				NullLiteral n = new NullLiteral(token);
				spG.lineFinish = token.posn.lineNum;
				acceptIt();
				return new LiteralExpr(n,spG);
			}
			
			case Token.INTLITERAL:
			{
				IntLiteral i = new IntLiteral(token);
				spG.lineFinish = token.posn.lineNum;
				acceptIt();
				return new LiteralExpr(i,spG);
			}
			
			case Token.TRUE:
			{
				BooleanLiteral b = new BooleanLiteral(token);
				spG.lineFinish = token.posn.lineNum;
				acceptIt();
				return new LiteralExpr(b,spG);
			}
			
			case Token.FALSE:
			{
				BooleanLiteral b = new BooleanLiteral(token);
				spG.lineFinish = token.posn.lineNum;
				acceptIt();
				return new LiteralExpr(b,spG);
			}
			
			case Token.NEW:
			{
				SourcePosition sp2 = new SourcePosition(0);
				acceptIt();
				if(token.kind == Token.INT)
				{
					sp2.lineStart = token.posn.lineNum;
					sp2.lineFinish = token.posn.lineNum;
					TypeDenoter type = new BaseType(TypeKind.INT, sp2);
					acceptIt();
					accept(Token.LBRACKET);
					Expression e = parseExpression();
					spG.lineFinish = token.posn.lineNum;
					accept(Token.RBRACKET);
					return new NewArrayExpr(type, e, spG);
				}
				
				else if (token.kind == Token.IDENTIFIER)
				{
					sp2.lineStart = token.posn.lineNum;
					sp2.lineFinish = token.posn.lineNum;
					Identifier id = new Identifier(token);
					ClassType type = new ClassType(id, sp2);
					acceptIt();
					
					if (token.kind == Token.LBRACKET)
					{
						acceptIt();
						Expression e = parseExpression();
						spG.lineFinish = token.posn.lineNum;
						accept(Token.RBRACKET);
						return new NewArrayExpr(type, e, spG);
					}
					
					else if (token.kind == Token.LPAREN)
					{
						acceptIt();
						spG.lineFinish = token.posn.lineNum;
						accept(Token.RPAREN);
						return new NewObjectExpr(type, spG);
					}
					
					else
					{
						parseError("Invalid Term - expecting LPAREN or LBRACKET but found " + token.kind);
						return null;
					}
				}
				
				else
				{
					parseError("Invalid Term - expecting INT or IDENTIFIER but found " + token.kind);
					return null;
				}
			}
			
			case Token.LPAREN:
			{
				acceptIt();
				Expression e = parseExpression();
				accept(Token.RPAREN);
				return e;
			}
			
			case Token.IDENTIFIER: case Token.THIS:
			{
				Reference r = parseReference();
				spG.lineFinish = token.posn.lineNum;
				if(token.kind == Token.LPAREN)
				{
					acceptIt();
					if(token.kind == Token.RPAREN)
					{
						spG.lineFinish = token.posn.lineNum;
						acceptIt();
						ExprList elist =  new ExprList();
						return new CallExpr(r, elist, spG);
					}
					else
					{
						ExprList elist =  parseArgumentList();
						spG.lineFinish = token.posn.lineNum;
						accept(Token.RPAREN);
						return new CallExpr(r, elist, spG);
					}
				}
				return new RefExpr(r, spG);
			}
			
			case Token.COMPLEMENT: case Token.MINUS:
			{
				Operator o = new Operator(token);
				acceptIt();
				Expression e = parseExpressionG();
				spG.lineFinish = token.posn.lineNum;
				return new UnaryExpr(o, e, spG);
			}
			
			default:
			{
				parseError("Invalid Term - expecting IDENTIFIER or THIS or UNOP or LPAREN or INTLITERAL or TRUE or FALSE or NEW but found " + token.kind);
				return null;
			}
		}
	}
	
	private Statement parseStatement()
	{
		SourcePosition sp = new SourcePosition(0);
		sp.lineStart = token.posn.lineNum;
		switch(token.kind)
		{
			case Token.WHILE:
			{
				acceptIt();
				accept(Token.LPAREN);
				Expression e = parseExpression();
				accept(Token.RPAREN);
				Statement s = parseStatement();
				sp.lineFinish = token.posn.lineNum;
				return new WhileStmt(e,s,sp);
			}
			
			case Token.IF:
			{
				acceptIt();
				accept(Token.LPAREN);
				Expression e = parseExpression();
				accept(Token.RPAREN);
				Statement s = parseStatement();
				sp.lineFinish = token.posn.lineNum;
				if (token.kind == Token.ELSE)
				{
					acceptIt();
					Statement s2 = parseStatement();
					sp.lineFinish = token.posn.lineNum;
					return new IfStmt(e,s,s2, sp);
				}
				return new IfStmt(e,s,sp);
			}
			
			case Token.RETURN:
			{
				acceptIt();
				if (token.kind == Token.SEMICOLON)
				{
					sp.lineFinish = token.posn.lineNum;
					acceptIt();
					return new ReturnStmt(null,sp);
				}
				Expression e = parseExpression();
				sp.lineFinish = token.posn.lineNum;
				accept(Token.SEMICOLON);
				return new ReturnStmt(e,sp);
			}
			
			case Token.LCURLY:
			{
				StatementList slist = new StatementList();
				acceptIt();
				while(token.kind != Token.RCURLY)
				{
					Statement s = parseStatement();
					slist.add(s);
				}
				sp.lineFinish = token.posn.lineNum;
				accept(Token.RCURLY);
				return new BlockStmt(slist, sp);
			}
			
			case Token.BOOLEAN: case Token.INT:
			{
				TypeDenoter type = parseType();
				String name = "";
				if (token.kind == Token.IDENTIFIER)
				{
					name = token.spelling;
					acceptIt();
				}
				else
				{
					accept(Token.IDENTIFIER);

				}
				
				accept(Token.EQUALS);
				Expression e = parseExpression();
				sp.lineFinish = token.posn.lineNum;
				accept(Token.SEMICOLON);
				VarDecl vd = new VarDecl(type, name, sp);
				return new VarDeclStmt(vd, e, sp);
			}
			
			case Token.THIS:
			{
				Reference r = parseReference();
				if (token.kind == Token.LPAREN)
				{
					acceptIt();
					if (token.kind == Token.RPAREN)
					{
						acceptIt();
						sp.lineFinish = token.posn.lineNum;
						accept(Token.SEMICOLON);
						ExprList elist = new ExprList();
						return new CallStmt(r, elist, sp);
					}
					
					else
					{
						ExprList elist = parseArgumentList();
						accept(Token.RPAREN);
						sp.lineFinish = token.posn.lineNum;
						accept(Token.SEMICOLON);
						return new CallStmt(r,elist,sp);
					}
				}
				
				else if (token.kind == Token.EQUALS)
				{
					acceptIt();
					Expression e = parseExpression();
					sp.lineFinish = token.posn.lineNum;
					accept(Token.SEMICOLON);
					return new AssignStmt(r,e,sp);
				}
				
				else
				{
					parseError("Invalid Term - expecting LPAREN or EQUALS but found " + token.kind);
					return null;
				}
			}
			
			case Token.IDENTIFIER:
			{
				SourcePosition sp2 = new SourcePosition(0);
				sp2.lineStart = token.posn.lineNum;
				sp2.lineFinish = token.posn.lineNum;

				Identifier i =  new Identifier(token);
				ClassType ctype = new ClassType(i, sp2);
				ArrayType atype = new ArrayType(ctype, sp2);
				Reference ref;
				String name = "";
				acceptIt();
				
				if (token.kind == Token.IDENTIFIER)
				{
					name = token.spelling;
					acceptIt();
					accept(Token.EQUALS);
					Expression e = parseExpression();
					sp.lineFinish = token.posn.lineNum;
					accept(Token.SEMICOLON);
					VarDecl vd =  new VarDecl(ctype, name, sp);
					return new VarDeclStmt(vd,e,sp);
				}
				
				else if (token.kind == Token.LBRACKET)
				{
					acceptIt();
					if (token.kind == Token.RBRACKET)
					{
						acceptIt();
						if (token.kind == Token.IDENTIFIER)
						{
							name = token.spelling;
							acceptIt();
						}
						else
						{
							accept(Token.IDENTIFIER);
						}
						
						accept(Token.EQUALS);
						Expression e = parseExpression();
						sp.lineFinish = token.posn.lineNum;
						accept(Token.SEMICOLON);
						VarDecl vd =  new VarDecl(atype, name, sp);
						return new VarDeclStmt(vd, e, sp);
					}
					
					else
					{
						Expression e = parseExpression();
						sp.lineFinish = token.posn.lineNum;
						ref = new IxIdRef(i, e, sp);
						Identifier i2 = null;
						accept(Token.RBRACKET);
						
						while(token.kind == Token.PERIOD)
						{
							SourcePosition sp3 = new SourcePosition(0);
							sp3.lineStart = sp.lineStart;
							acceptIt();
							
							if (token.kind == Token.IDENTIFIER)
							{
								i2 = new Identifier(token);
								sp3.lineFinish = token.posn.lineNum;
								acceptIt();
							}
							else
							{
								accept(Token.IDENTIFIER);
							}
							
							if(token.kind == Token.LBRACKET)
							{
								acceptIt();
								e = parseExpression();
								sp3.lineFinish = token.posn.lineNum;
								accept(Token.RBRACKET);
								ref =  new IxQRef(ref, i2, e, sp3);
								continue;
							}
							ref =  new QRef(ref,i2,sp3);
							
						}
						
						if (token.kind == Token.LPAREN)
						{
							acceptIt();
							if (token.kind == Token.RPAREN)
							{
								acceptIt();
								sp.lineFinish = token.posn.lineNum;
								accept(Token.SEMICOLON);
								ExprList elist = new ExprList();
								return new CallStmt(ref, elist, sp);
							}
							
							else
							{
								ExprList alist = parseArgumentList();
								accept(Token.RPAREN);
								sp.lineFinish = token.posn.lineNum;
								accept(Token.SEMICOLON);
								return new CallStmt(ref, alist, sp);
							}
						}
						
						else if (token.kind == Token.EQUALS)
						{
							acceptIt();
							Expression e2 = parseExpression();
							sp.lineFinish = token.posn.lineNum;
							accept(Token.SEMICOLON);
							return new AssignStmt(ref, e2, sp);
						}
						
						else
						{
							parseError("Invalid Term - expecting LPAREN or EQUALS but found " + token.kind);
							return null;
						}
					}
				}
				
				else if(token.kind == Token.LPAREN)
				{
					SourcePosition sp4 = new SourcePosition(0);
					sp4.lineStart = sp.lineStart;
					sp4.lineFinish = token.posn.lineNum;
					ref = new IdRef(i, sp4);
					acceptIt();
					SourcePosition sp3 = new SourcePosition(0);
					sp3.lineStart = sp.lineStart;

					if (token.kind == Token.RPAREN)
					{
						acceptIt();
						accept(Token.SEMICOLON);
						ExprList elist =  new ExprList();
						sp3.lineFinish = token.posn.lineNum;
						return new CallStmt(ref, elist, sp3);
					}
					
					else
					{
						ExprList alist = parseArgumentList();
						accept(Token.RPAREN);
						sp3.lineFinish = token.posn.lineNum;
						accept(Token.SEMICOLON);
						return new CallStmt(ref, alist, sp3);
					}
				}
				
				else if(token.kind == Token.EQUALS)
				{
					SourcePosition sp4 = new SourcePosition(0);
					sp4.lineStart = sp.lineStart;
					sp4.lineFinish = token.posn.lineNum;
					ref = new IdRef(i, sp4);
					acceptIt();
					Expression e = parseExpression();
					sp.lineFinish = token.posn.lineNum;
					accept(Token.SEMICOLON);
					return new AssignStmt(ref, e, sp);
				}
				
				else if (token.kind == Token.PERIOD)
				{
					SourcePosition sp4 = new SourcePosition(0);
					sp4.lineStart = sp.lineStart;
					sp4.lineFinish = token.posn.lineNum;
					ref = new IdRef(i, sp4);
					while(token.kind == Token.PERIOD)
					{
						SourcePosition sp3 = new SourcePosition(0);
						sp3.lineStart = sp.lineStart;
						acceptIt();
						
						if (token.kind == Token.IDENTIFIER)
						{
							i = new Identifier(token);
							sp3.lineFinish = token.posn.lineNum;
							acceptIt();
						}
						
						else
						{
							accept(Token.IDENTIFIER);
						}
						
						if(token.kind == Token.LBRACKET)
						{
							acceptIt();
							Expression e = parseExpression();
							sp3.lineFinish = token.posn.lineNum;
							accept(Token.RBRACKET);
							ref = new IxQRef(ref, i, e, sp3);
							continue;
						}
						ref = new QRef(ref, i, sp3);
						
					}
					
					if (token.kind == Token.LPAREN)
					{
						acceptIt();
						if (token.kind == Token.RPAREN)
						{
							acceptIt();
							sp.lineFinish = token.posn.lineNum;
							accept(Token.SEMICOLON);
							ExprList elist =  new ExprList();
							return new CallStmt(ref, elist, sp);
						}
						
						else
						{
							ExprList alist = parseArgumentList();
							accept(Token.RPAREN);
							sp.lineFinish = token.posn.lineNum;
							accept(Token.SEMICOLON);
							return new CallStmt(ref, alist, sp);
						}
					}
					
					else if (token.kind == Token.EQUALS)
					{
						acceptIt();
						Expression e = parseExpression();
						sp.lineFinish = token.posn.lineNum;
						accept(Token.SEMICOLON);
						return new AssignStmt(ref, e, sp);
					}
					
					else
					{
						parseError("Invalid Term - expecting LPAREN or EQUALS but found " + token.kind);
						return null;
					}
				}
				
				else
				{
					parseError("Invalid Term - expecting LPAREN or EQUALS or IDENTIFIER or PERIOD or LBRACKET but found " + token.kind);
					return null;
				}
			}
			
			default:
			{
				parseError("Invalid Term - expecting WHILE or IF or RETURN or THIS or INT or BOOLEAN or IDENTIFIER or LBRACKET but found " + token.kind);
				return null;
			}
		}
	}
	
	private ClassDecl parseClassDeclaration()
	{
		FieldDeclList flist =  new FieldDeclList();
		MethodDeclList mlist =  new MethodDeclList();
		SourcePosition spc = new SourcePosition(0);
		spc.lineStart = token.posn.lineNum;
		String cname = "";
		accept(Token.CLASS);
		
		if (token.kind == Token.IDENTIFIER)
		{
			cname = token.spelling;
			acceptIt();
		}
		else
		{
			accept(Token.IDENTIFIER);
		}
		
		accept(Token.LCURLY);
				
		while (token.kind != Token.RCURLY)
		{
			SourcePosition spf = new SourcePosition(0);
			boolean priv =  false;
			boolean publ = false;
			boolean stat = false;
			String visibility = parseVisibility();
			String access = parseAccess();
			
			
			if (visibility == "private")
			{
				priv = true;
			}
			
			else if (visibility == "public")
			{
				publ = true;
			}
			
			if (access == "static")
			{
				stat = true;
			}
			

			if (token.kind == Token.BOOLEAN || token.kind == Token.INT || token.kind == Token.IDENTIFIER)
			{
				spf.lineStart = token.posn.lineNum;

				TypeDenoter type2 = parseType();
				StatementList slist = new StatementList();
				ParameterDeclList plist = new ParameterDeclList();
				String name2 = "";
				
				if (token.kind == Token.IDENTIFIER)
				{
					name2 = token.spelling;
					acceptIt();
				}
				else
				{
					accept(Token.IDENTIFIER);
				}
				
				if (token.kind == Token.LPAREN)
				{
					acceptIt();
					if (token.kind == Token.RPAREN)
					{
						acceptIt();
					}
					
					else
					{
						plist = parseParameterList();
						accept(Token.RPAREN);
					}
					
					accept(Token.LCURLY);
					
					while (token.kind != Token.RCURLY)
					{
						slist.add(parseStatement());
					}
					
					spf.lineFinish = token.posn.lineNum;

					accept(Token.RCURLY);
					FieldDecl fd = new FieldDecl(priv, stat, type2, name2, spf);
					MethodDecl md = new MethodDecl(fd, plist, slist, spf);
					mlist.add(md);
					
				}
				
				else if (token.kind == Token.SEMICOLON)
				{
					spf.lineFinish = token.posn.lineNum;
					FieldDecl fd = new FieldDecl(priv, stat, type2, name2, spf);
					flist.add(fd);
					acceptIt();
				}
				
				else
				{
					parseError("Invalid Term - expecting LPAREN or SEMICOLON but found " + token.kind);
					return null;
				}
			}
			
			else if (token.kind == Token.VOID)
			{
				SourcePosition spf2 =  new SourcePosition(0);
				spf2.lineStart = token.posn.lineNum;
				spf2.lineFinish = token.posn.lineNum;

				spf.lineStart = token.posn.lineNum;


				BaseType type2 = new BaseType(TypeKind.VOID, spf2);
				String name2 = "";
				StatementList slist = new StatementList();
				ParameterDeclList plist = new ParameterDeclList();
				acceptIt();
				
				if (token.kind == Token.IDENTIFIER)
				{
					name2 = token.spelling;
					acceptIt();
				}
				else
				{
					accept(Token.IDENTIFIER);
				}
				
				accept(Token.LPAREN);
				if (token.kind == Token.RPAREN)
				{
					acceptIt();
				}
				
				else
				{
					plist = parseParameterList();
					accept(Token.RPAREN);
				}
				
				accept(Token.LCURLY);
				while (token.kind != Token.RCURLY)
				{
					slist.add(parseStatement());
				}
				
				spf.lineFinish = token.posn.lineNum;

				accept(Token.RCURLY);
				FieldDecl fd =  new FieldDecl(priv, stat, type2, name2, spf);
				MethodDecl md = new MethodDecl(fd, plist, slist, spf);
				mlist.add(md);
			}
			
			else
			{
				parseError("Invalid Term - expecting BOOLEAN or INT or IDENTIFIER or VOID but found " + token.kind);
				return null;
			}
		}
		spc.lineFinish = token.posn.lineNum;

		accept(Token.RCURLY);
		return new ClassDecl(cname, flist, mlist, spc);	
	}
	
	private Package parseProgram()
	{
		ClassDeclList clist =  new ClassDeclList();
		SourcePosition sp =  new SourcePosition(0);
		sp.lineStart = 1;
		while (token.kind != Token.EOT)
		{
			clist.add(parseClassDeclaration());
		}
		sp.lineFinish = token.posn.lineNum;
		accept(Token.EOT);
		return new Package(clist, sp);
	}
	
	
	private void acceptIt() throws SyntaxError {
		accept(token.kind);
	}
	
	private void accept(byte expectedTokenKind) throws SyntaxError {
		if (token.kind == expectedTokenKind) {
			if (trace)
				pTrace();
			token = scanner.scan();
		}
		else
			parseError("expecting '" + expectedTokenKind +
					"' but found '" + token.kind + "'");
	}
	
	class SyntaxError extends Error {
		private static final long serialVersionUID = 1L;	
	}
	
	
	private void parseError(String e) throws SyntaxError {
		reporter.reportError("Parse error: " + e);
		throw new SyntaxError();
	}
	
	private void pTrace() {
		StackTraceElement [] stl = Thread.currentThread().getStackTrace();
		for (int i = stl.length - 1; i > 0 ; i--) {
			if(stl[i].toString().contains("parse"))
				System.out.println(stl[i]);
		}
		System.out.println("accepting: " + token.kind + " (\"" + token.spelling + "\")");
		System.out.println();
	}
}

