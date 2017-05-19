package miniJava.SyntacticAnalyzer;

public class SourcePosition 
{
	public int lineNum;
	public int lineStart;
	public int lineFinish;
	
	public SourcePosition(int line)
	{
		lineNum = line;
		lineStart = lineNum;
		lineFinish = lineNum;
	}
	
	public String toString()
	{
		String returnee = Integer.toString(lineStart);
		returnee = returnee + " - " + Integer.toString(lineFinish);
		return returnee;		
	}
}

