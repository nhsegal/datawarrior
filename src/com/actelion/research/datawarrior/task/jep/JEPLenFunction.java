/* * Copyright 2014 Actelion Pharmaceuticals Ltd., Gewerbestrasse 16, CH-4123 Allschwil, Switzerland * * This file is part of DataWarrior. *  * DataWarrior is free software: you can redistribute it and/or modify it under the terms of the * GNU General Public License as published by the Free Software Foundation, either version 3 of * the License, or (at your option) any later version. *  * DataWarrior is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. * See the GNU General Public License for more details. * You should have received a copy of the GNU General Public License along with DataWarrior. * If not, see http://www.gnu.org/licenses/. * * @author Thomas Sander */package com.actelion.research.datawarrior.task.jep;import java.util.Stack;import org.nfunk.jep.ParseException;import org.nfunk.jep.function.PostfixMathCommand;
/**
 * An example custom function class for JEP.
 */
public class JEPLenFunction extends PostfixMathCommand {
	/**
	 * Constructor
	 */
	public JEPLenFunction() {
		numberOfParameters = 1;
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
		Object param1 = inStack.pop();
		// check whether the argument is of the right type		if (param1 instanceof String) {
			// calculate the result
			double len = ((String)param1).length();

			// push the result on the inStack
			inStack.push(new Double(len));
		    }		else {
			throw new ParseException("Invalid parameter type");
		    }
	    }
    }
