package miniJava.CodeGenerator;

public class KnownValue extends RuntimeEntity{
	public int value;
	
	public KnownValue(int a_size, int a_value)
	{
		value = a_value;
		size = a_size;
	}
}
