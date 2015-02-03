/*
import java.util.*;
import org.nfunk.jep.*;
import org.nfunk.jep.function.*;
/**
 * An rounding function for JEP.
 */
public class JEPRoundFunction extends PostfixMathCommand {
	/**
	 * Constructor
	 */
	public JEPRoundFunction() {
		numberOfParameters = 2;
	}
	/**
	 * Runs the square root operation on the inStack. The parameter is popped
	 * off the <code>inStack</code>, and the square root of it's value is 
	 * pushed back to the top of <code>inStack</code>.
	 */
	public void run(Stack inStack) throws ParseException {
		// check the stack
		checkStack(inStack);
		// get the parameters from the stack
		// check whether the argument is of the right type