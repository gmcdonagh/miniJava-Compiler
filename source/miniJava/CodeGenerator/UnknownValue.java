package miniJava.CodeGenerator;

public class UnknownValue extends RuntimeEntity{
	public int address;
	public String register;
	
	public UnknownValue(int a_size, String a_reg, int a_address)
	{
		address = a_address;
		size = a_size;
		register = a_reg;
	}
}
