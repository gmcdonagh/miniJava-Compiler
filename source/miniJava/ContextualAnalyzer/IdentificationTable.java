package miniJava.ContextualAnalyzer;
import java.util.*;
import miniJava.AbstractSyntaxTrees.*;

public class IdentificationTable {
	public Stack<HashMap<String, Declaration>> idTable;
	
	public IdentificationTable()
	{
		idTable = new Stack<HashMap<String, Declaration>>();
	}
	
	public void openScope()
	{
		HashMap<String, Declaration> newScope = new HashMap<String, Declaration>();
		idTable.push(newScope);
	}
	
	public void closeScope()
	{
		idTable.pop();
	}
	
	public void removeID(String idName)
	{
		for(int i = idTable.size()-1; i >= 0 ; i--)
		{
			HashMap<String, Declaration> currScope = idTable.get(i);
			if(currScope.containsKey(idName))
			{
				currScope.remove(idName);
			}
		}
		
	}
	public void enterID(String idName, Declaration idDecl)
	{
		HashMap<String, Declaration> currScope = idTable.peek();
		
		if(currScope.containsKey(idName))
		{
			System.out.println("*** " + idDecl.posn.toString() + " Can't mask identifier in same scope *** ");
			System.exit(4);
		}
		
		if (idTable.size() >= 5)
		{
			for(int i = idTable.size()-1; i >= 0 ; i--)
			{
				HashMap<String, Declaration> currScope2 = idTable.get(i);
				if(currScope2.containsKey(idName))
				{
					if (i >= 3)
					{
						System.out.println("*** " + idDecl.posn.toString() +" Identifiers at scope level 4 or higher may not mask identifiers at scope 3 or higher *** ");
						System.exit(4);
					}
				}
			}			
		}
		
		currScope.put(idName, idDecl);

	}
	
	public Declaration getDecl(String idName, boolean staticCheck)
	{
		int foundLevel = -1;
		for(int i = idTable.size()-1; i >= 0 ; i--)
		{
			HashMap<String, Declaration> currScope = idTable.get(i);
			if(currScope.containsKey(idName))
			{
				if (staticCheck)
				{
					if (i == 2)
					{
						Declaration decl = currScope.get(idName);
						if (decl instanceof FieldDecl)
						{
							if(!((FieldDecl) decl).isStatic)
							{
								return new VarDecl(null, "ThisIsAStaticReferenceErrorInAStaticMethod", null);
							}
						}
						
						else if (decl instanceof MethodDecl)
						{
							if (!((MethodDecl) decl).isStatic)
							{
								return new VarDecl(null, "ThisIsAStaticReferenceErrorInAStaticMethod", null);
							}
						}
					}
				}
				return currScope.get(idName);
				
			}
		}
		
		
		return null;
	}
	
	

}
