package miniJava;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import mJAM.Disassembler;
import mJAM.Interpreter;
import mJAM.ObjectFile;
import miniJava.ErrorReporter;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;
import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.CodeGenerator.EmittingVisitor;
import miniJava.ContextualAnalyzer.IdentificationTable;
import miniJava.ContextualAnalyzer.IdentifyingVisitor;
import miniJava.ContextualAnalyzer.TypeCheckingVisitor;
public class Compiler 
{
	public static void main(String[] args)
	{
		InputStream inputStream = null;
		if (args.length == 0) {
			System.out.println("Enter Expression");
			inputStream = System.in;
		}
		else {
			try {
				inputStream = new FileInputStream(args[0]);
			} catch (FileNotFoundException e) {
				System.out.println("Input file " + args[0] + " not found");
				System.exit(1);
			}		
		}

		ErrorReporter reporter = new ErrorReporter();
		Scanner scanner = new Scanner(inputStream, reporter);
		Parser parser = new Parser(scanner, reporter);
		
		System.out.println("Syntactic analysis ... ");
		AST ast = parser.parse();		
		
		System.out.print("Syntactic analysis complete:  ");
		if (reporter.hasErrors()) {
			System.out.println("INVALID MiniJava");
			System.exit(4);
		}
		else {
			System.out.println("VALID Minijava\n");
			IdentifyingVisitor idv = new IdentifyingVisitor();
			idv.Identify(ast);
			boolean stringMask = false;
			ClassDeclList cdl = ((Package) ast).classDeclList;
			for(int i = 0; i < cdl.size(); i++)
			{
				if (cdl.get(i).name.equals("String"))
				{
					stringMask = true;
				}
			}
			
			TypeCheckingVisitor tcv = new TypeCheckingVisitor(stringMask);
			tcv.typeCheck(ast);
			if(tcv.errorPresent)
			{
				System.exit(4);
			}
			
			if (stringMask)
			{
				System.out.println("Invalid parameter to main method");
				System.exit(4);
			}
			
			else
			{
				System.out.println("Contextual analysis complete: VALID Minijava\n");
			}
			
			EmittingVisitor ev =  new EmittingVisitor();
			ev.emitInstructions(ast);
			System.out.println("Code Generation complete: VALID Minijava\n");
			
			int begin = args[0].lastIndexOf('/');
			if (begin == -1)
			{
				begin = 0;
			}
			int end = args[0].lastIndexOf('.');
			
			String objectCodeFileName = args[0].substring(begin, end) + ".mJAM";
			ObjectFile objF = new ObjectFile(objectCodeFileName);
			System.out.print("Writing object code file " + objectCodeFileName + " ... ");
			if (objF.write()) {
				System.out.println("FAILED!");
				System.exit(4);
				return;
			}
			else
				System.out.println("SUCCEEDED");
				//System.exit(0);
			 // create asm file using disassembler 
	        String asmCodeFileName = objectCodeFileName.replace(".mJAM",".asm");
	        System.out.print("Writing assembly file " + asmCodeFileName + " ... ");
	        Disassembler d = new Disassembler(objectCodeFileName);
	        if (d.disassemble()) {
	                System.out.println("FAILED!");
	                return;
	        }
	        else
	                System.out.println("SUCCEEDED");

	/* 
	 * run code using debugger
	 * 
	 */
	        System.out.println("Running code in debugger ... ");
	        Interpreter.debug(objectCodeFileName, asmCodeFileName);

	        System.out.println("*** mJAM execution completed");
		}
	}
}
