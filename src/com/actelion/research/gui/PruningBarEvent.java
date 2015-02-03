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

package com.actelion.research.gui;

import java.util.EventObject;

public class PruningBarEvent extends EventObject {
    private static final long serialVersionUID = 0x20101217;

    private float mLowValue,mHighValue;
	private int mID;
	private boolean mAdjusting;

    public PruningBarEvent(Object source, float low, float high, boolean adjusting, int id) {
		super(source);
		mLowValue = low;
		mHighValue = high;
		mAdjusting = adjusting;
		mID = id;
	    }

	public float getLowValue() {
		return mLowValue;
		}

	public float getHighValue() {
		return mHighValue;
		}

	public boolean isAdjusting() {
		return mAdjusting;
		}

	public int getID() {
		return mID;
		}
	}