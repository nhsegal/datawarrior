/*
 * Copyright 2014 Actelion Pharmaceuticals Ltd., Gewerbestrasse 16, CH-4123 Allschwil, Switzerland
 *
 * This file is part of DataWarrior.
 * 
 * DataWarrior is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * DataWarrior is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with DataWarrior.
 * If not, see http://www.gnu.org/licenses/.
 *
 * @author Thomas Sander
 */

package com.actelion.research.datawarrior.task.jep;

import java.util.*;
import org.nfunk.jep.*;
import org.nfunk.jep.function.*;import com.actelion.research.chem.StereoMolecule;import com.actelion.research.table.CompoundTableModel;
/** * An Actelion custom function class for JEP
 * to rate HTS results considering the molecular weight.
 */
public class JEPRateHTSFunction extends PostfixMathCommand {    private CompoundTableModel mTableModel;

	public JEPRateHTSFunction(CompoundTableModel tableModel) {
        mTableModel = tableModel;
		numberOfParameters = 3;
	    }
	/**	 * Runs the operation on the inStack. The parameters are popped	 * off the <code>inStack</code>, and the square root of it's value is 	 * pushed back to the top of <code>inStack</code>.	 */
	public void run(Stack inStack) throws ParseException {
		// check the stack		checkStack(inStack);
		// get the parameters from the stack
		Object param3 = inStack.pop();
		Object param2 = inStack.pop();		Object param1 = inStack.pop();
		// check whether the argument is of the right type		if (param1 instanceof Double		 && param2 instanceof Double		 && param3 instanceof JEPParameter) {            JEPParameter jepParam3 = (JEPParameter)param3;            if (!CompoundTableModel.cColumnTypeIDCode.equals(mTableModel.getColumnSpecialType(jepParam3.column))) {                throw new ParseException("3rd parameter of ligeff1() is not a chemical structure.");                }            double deltaG_rel = Double.NaN;            StereoMolecule mol = mTableModel.getChemicalStructure(jepParam3.record, jepParam3.column, CompoundTableModel.ATOM_COLOR_MODE_NONE, null);            if (mol != null) {                mol.stripSmallFragments();                mol.ensureHelperArrays(StereoMolecule.cHelperNeighbours);
    			// calculate the result    			double ra = ((Double)param1).doubleValue();    			double conc = 0.000001 * ((Double)param2).doubleValue();    
    			double effect = Math.min(99.0, Math.max(1.0, ra));    			double ic50 = conc / (100/effect - 1.0);        			// dG = -RT*ln(Kd) with R=1.986cal/(K*mol); T=300K; dG in kcal/mol                // We use IC50 instead of Kd, which is acceptable according to                // Andrew L. Hopkins, Colin R. Groom, Alexander Alex                // Drug Discovery Today, Volume 9, Issue 10, 15 May 2004, Pages 430-431                deltaG_rel = - 1.986 * 0.300 * Math.log(ic50) / mol.getAtoms();                }
			// push the result on the inStack			inStack.push(new Double(deltaG_rel));
		    }		else {
			throw new ParseException("Invalid parameter type");		    }	    }
    }
