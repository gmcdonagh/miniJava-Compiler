package miniJava.SyntacticAnalyzer;

public class Token 
{
	public byte kind;
	public String spelling;
	public SourcePosition posn;
	
	public Token(byte kind, String spelling, SourcePosition position)
	{
		this.kind = kind;
		this.spelling = spelling;
		this.posn = position;
		if (kind == IDENTIFIER)
		{
			for (int k = IF; k <= INT; k++)
			{
				if (spelling.equals(spellings[k]))
				{
					this.kind = (byte) k;
					break;
				}
			}
		}
	}
	
	public final static byte
		ERROR = -99,
		IDENTIFIER = 0,
		INTLITERAL = 1,
		GT = 2,				// >
		LT = 3,				// <
		ECOMPARISON = 4,	// ==
		LTE = 5,			// <=
		GTE = 6,			// >=
		NECOMPARISON = 7,	// !=
		AND = 8,			// &&
		OR = 9,				// ||
		PLUS = 10,			// +
		TIMES = 11,			// *
		DIVIDE = 12,		// /
		MINUS = 13,			// -
		COMPLEMENT = 14,	// !
		IF = 15,			// if
		ELSE = 16,			// else
		WHILE = 17,			// while
		RETURN = 18,		// return
		THIS = 19,			// this
		NEW = 20,			// new
		VOID = 21,			// void
		CLASS = 22,			// class
		TRUE = 23,			// true
		FALSE = 24,			// false
		PUBLIC = 25,		// public
		PRIVATE = 26,		// private
		STATIC = 27,		// static
		BOOLEAN = 28,		// boolean
		NULL = 29,			// null
		INT = 30,			// int
		EQUALS = 31,		// =
		SEMICOLON = 32,		// ;
		LPAREN = 33,		// (
		RPAREN = 34,		// )
		PERIOD = 35, 		// .
		COMMA = 36,			// ,
		LBRACKET = 37,		// [
		RBRACKET = 38,		// ]
		LCURLY = 39,		// {
		RCURLY = 40,		// }
		EOT = 99;			// end of text
	
	private final static String[] spellings = 
	{
		"<identifier>", "<integer-literal>", ">", "<", "==", "<=", ">=", "!=", "&&", "||", 
		"+", "*", "/", "-", "!", "if", "else", "while", "return",
		"this", "new", "void", "class", "true", "false", "public", "private", "static",
		"boolean", "null", "int", "=", ";", "(", ")", ".", ",", "[", "]", "{", "}", "<eot>"
	};
}

