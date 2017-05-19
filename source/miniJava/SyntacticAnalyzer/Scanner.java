package miniJava.SyntacticAnalyzer;

import java.io.IOException;
import java.io.InputStream;

import miniJava.ErrorReporter;

public class Scanner 
{
	private InputStream inputStream;
	private ErrorReporter reporter;

	private char currentChar;
	private byte currentKind;
	private StringBuilder currentSpelling;
	private int lineNum = 1;
	
	private boolean eot = false; 
	
	public Scanner(InputStream inputStream, ErrorReporter reporter) {
		this.inputStream = inputStream;
		this.reporter = reporter;

		// initialize scanner state
		readChar();
	}
	
	public Token scan() {
		// skip whitespace
		while (!eot && (currentChar == ' ' || currentChar == '\t' || currentChar == '\n' || currentChar == '\r'))
		{	
			if(currentChar == '\n')
			{
				lineNum++;
			}
			
			skipIt();
		}
		
		// collect spelling and identify token kind
		currentSpelling = new StringBuilder();
		Byte kind = scanToken();
		currentKind = kind;

		// return new token
		SourcePosition sp =  new SourcePosition(lineNum);
		return new Token(kind, currentSpelling.toString(), sp);
	}
	
	private byte scanToken()
	{
		if (eot)
			return(Token.EOT); 
		
		switch(currentChar)
		{
			case 'a': case 'b': case 'c': case 'd': case 'e': case 'f': case 'g': case 'h': case 'i': 
			case 'j': case 'k': case 'l': case 'm': case 'n': case 'o': case 'p': case 'q': case 'r':
			case 's': case 't': case 'u': case 'v': case 'w': case 'x': case 'y': case 'z': 
			case 'A': case 'B': case 'C': case 'D': case 'E': case 'F': case 'G': case 'H': case 'I': 
			case 'J': case 'K': case 'L': case 'M': case 'N': case 'O': case 'P': case 'Q': case 'R': 
			case 'S': case 'T': case 'U': case 'V': case 'W': case 'X': case 'Y': case 'Z': 
			{
				takeIt();
				
				while(isDigit(currentChar) || isLetter(currentChar) || currentChar == '_')
				{
					takeIt();
				}
				
				return Token.IDENTIFIER;
			}
			
			case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': 
			case '9':
			{
				takeIt();
				while(isDigit(currentChar))
				{
					takeIt();
				}
				
				return Token.INTLITERAL;
				
			}
			
			case '>':
			{
				takeIt();
				
				if(currentChar == '=')
				{
					takeIt();
					return Token.GTE;
				}
				
				else
				{
					return Token.GT;
				}
			}
			
			case '<':
			{
				takeIt();
				
				if(currentChar == '=')
				{
					takeIt();
					return Token.LTE;
				}
				
				else
				{
					return Token.LT;
				}
			}
			
			case '&':
			{
				takeIt();
				
				if(currentChar == '&')
				{
					takeIt();
					return Token.AND;
				}
				
				else
				{
					return Token.ERROR;
				}
			}
			
			case '=':
			{
				takeIt();
				
				if(currentChar == '=')
				{
					takeIt();
					return Token.ECOMPARISON;
				}
				
				else
				{
					return Token.EQUALS;
				}
			}
			
			case '!':
			{
				takeIt();
				
				if(currentChar == '=')
				{
					takeIt();
					return Token.NECOMPARISON;
				}
				
				else
				{
					return Token.COMPLEMENT;
				}
			}
			
			case '|':
			{
				takeIt();
				
				if(currentChar == '|')
				{
					takeIt();
					return Token.OR;
				}
				
				else
				{
					return Token.ERROR;
				}
			}
			
			case '+':
			{
				takeIt();
				return Token.PLUS;
			}
			
			
			case '*':
			{
				takeIt();
				return Token.TIMES;
			}
				
			case '/':
			{
				takeIt();
				
				if(currentChar == '*')
				{
					skipIt();
					boolean expecting = true;
					while(!eot)
					{
						while(currentChar != '*' && !eot)
						{
							if(currentChar == '\n')
							{
								lineNum++;
							}
							
							skipIt();
						}
						
						skipIt();
						
						if(currentChar == '/')
						{
							expecting = false;
							skipIt();
							currentSpelling = new StringBuilder();
							return scan().kind;
						}
					}
					
					if (expecting == true)
					{
						return Token.ERROR;
					}
				}
				
				if (currentChar == '/')
				{
					skipIt();
					while(currentChar != '\r' && currentChar != '\n')
					{
						skipIt();
					}
					
					if(currentChar == '\n')
					{
						lineNum++;
					}
					
					skipIt();
					currentSpelling = new StringBuilder();
					return scan().kind;
				}
				return Token.DIVIDE;
			}
			
			case '-':
			{
				takeIt();
				if(currentChar == '-')
				{
					return Token.ERROR;
				}
				else
				{
					return Token.MINUS;
				}
			}
			
			case ';':
			{
				takeIt();
				return Token.SEMICOLON;
			}
			
			case '(':
			{
				takeIt();
				return Token.LPAREN;
			}
			
			case ')':
			{
				takeIt();
				return Token.RPAREN;
			}
			
			case '.':
			{
				takeIt();
				return Token.PERIOD;
			}
			
			case ',':
			{
				takeIt();
				return Token.COMMA;
			}
			
			case '[':
			{
				takeIt();
				return Token.LBRACKET;
			}
			
			case ']':
			{
				takeIt();
				return Token.RBRACKET;
			}
			
			case '{':
			{
				takeIt();
				return Token.LCURLY;
			}
			
			case '}':
			{
				takeIt();
				return Token.RCURLY;
			}
		}
		
		return Token.ERROR;
	}
	
	private boolean isDigit(char c) 
	{
		return Character.isDigit(c);
	}
	
	private boolean isLetter(char c) 
	{
		return Character.isLetter(c);
	}
	
	private void takeIt() {
		currentSpelling.append(currentChar);
		nextChar();
	}
	
	private void skipIt() {
		nextChar();
	}
	
	private void scanError(String m) {
		reporter.reportError("Scan Error:  " + m);
	}
	
	private void nextChar() {
		if (!eot)
			readChar();
	}
	
	private void readChar() {
		try {
			int c = inputStream.read();
			currentChar = (char) c;
			if (c == -1) {
				eot = true;
			}
		} catch (IOException e) {
			scanError("I/O Exception!");
			eot = true;
		}
	}
}
