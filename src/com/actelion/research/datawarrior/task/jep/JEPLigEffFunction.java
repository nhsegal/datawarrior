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

import java.util.Stack;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.table.CompoundTableModel;


/**
 * An Actelion custom function class for JEP
 * to rate HTS results considering the molecular weight.
 */
public class JEPLigEffFunction extends PostfixMathCommand {
	public JEPLigEffFunction(CompoundTableModel tableModel) {
	/**
	public void run(Stack inStack) throws ParseException {
		// check the stack
		// get the parameters from the stack
		// check whether the argument is of the right type
            double deltaG_rel = Double.NaN;

            if (mol != null) {
                mol.stripSmallFragments();
                mol.ensureHelperArrays(StereoMolecule.cHelperNeighbours);

                // calculate the result

    			// dG = -RT*ln(Kd) with R=1.986cal/(K*mol); T=300K; dG in kcal/mol
    			// We use IC50 instead of Kd, which is acceptable according to
    			// Andrew L. Hopkins, Colin R. Groom, Alexander Alex
    			// Drug Discovery Today, Volume 9, Issue 10, 15 May 2004, Pages 430-431
    			deltaG_rel = - 1.986 * 0.300 * Math.log(ic50) / mol.getAtoms();
                }
			// push the result on the inStack
		    }
			throw new ParseException("Invalid parameter type");
		    }
	    }
    }