/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.Token;

abstract public class Terminal extends AST {

  public Terminal (Token t) {
	super(t.posn);
    spelling = t.spelling;
    kind = t.kind;
  }

  public byte kind;
  public String spelling;
}
