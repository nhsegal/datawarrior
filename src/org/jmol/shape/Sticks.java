/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-07-31 16:22:19 +0200 (ven., 31 juil. 2009) $
 * $Revision: 11291 $

 *
 * Copyright (C) 2002-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.shape;

import org.jmol.util.BitSetUtil;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Point3fi;
import org.jmol.viewer.JmolConstants;

import java.util.BitSet;
import java.util.Hashtable;

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;

import org.jmol.g3d.Graphics3D;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.modelset.BondIterator;
import org.jmol.modelset.HBond;

public class Sticks extends Shape {

  short myMask;
  boolean reportAll;
  
  BitSet bsOrderSet;
  BitSet bsSizeSet;
  BitSet bsColixSet;
  BitSet selectedBonds;

  public void initShape() {
    super.initShape();
    myMask = JmolConstants.BOND_COVALENT_MASK;
    reportAll = false;
  }

  /**
   * sets the size of a bond, or sets the selectedBonds set
   * 
   * @param size
   * @param bsSelected
   */
  public void setSize(int size, BitSet bsSelected) {
    if (size == Integer.MAX_VALUE) {
      selectedBonds = (bsSelected == null ? null : BitSetUtil.copy(bsSelected));
      return;
    }
    if (size == Integer.MIN_VALUE) { // smartaromatic has set the orders directly 
      if (bsOrderSet == null)
        bsOrderSet = new BitSet();
      bsOrderSet.or(bsSelected);
      return;
    }
    if (bsSizeSet == null)
      bsSizeSet = new BitSet();
    BondIterator iter = (selectedBonds != null ? modelSet.getBondIterator(selectedBonds)
        : modelSet.getBondIterator(myMask, bsSelected));
    short mad = (short) size;
    while (iter.hasNext()) {
      bsSizeSet.set(iter.nextIndex());
      iter.next().setMad(mad);
    }
  }

  public void setProperty(String propertyName, Object value, BitSet bsSelected) {
    if (Logger.debugging) {
      Logger.debug(propertyName + " " + value + " " + bsSelected);
    }
    if ("type" == propertyName) {
      myMask = ((Integer) value).shortValue();
      return;
    }
    if ("reportAll" == propertyName) {
      // when connections are restored, all we can do is report them all
      reportAll = true;
      return;
    }

    if ("reset" == propertyName) {
      // all bonds have been deleted -- start over
      bsOrderSet = null;
      bsSizeSet = null;
      bsColixSet = null;
      selectedBonds = null;
      return;
    }

    if ("bondOrder" == propertyName) {
      if (bsOrderSet == null)
        bsOrderSet = new BitSet();
      short order = ((Short) value).shortValue();
      BondIterator iter = (selectedBonds != null ? modelSet.getBondIterator(selectedBonds)
          : modelSet.getBondIterator(JmolConstants.BOND_ORDER_ANY, bsSelected));
      while (iter.hasNext()) {
        bsOrderSet.set(iter.nextIndex());
        iter.next().setOrder(order);
      }
      return;
    }
    if ("color" == propertyName) {
      if (bsColixSet == null)
        bsColixSet = new BitSet();
      short colix = Graphics3D.getColix(value);
      byte pid = JmolConstants.pidOf(value);
      if (pid == JmolConstants.PALETTE_TYPE || pid == JmolConstants.PALETTE_ENERGY) {
        //only for hydrogen bonds
        boolean isEnergy = (pid == JmolConstants.PALETTE_ENERGY);
        BondIterator iter = (selectedBonds != null ? modelSet.getBondIterator(selectedBonds)
            : modelSet.getBondIterator(myMask, bsSelected));
        while (iter.hasNext()) {
          bsColixSet.set(iter.nextIndex());
          Bond bond = iter.next();
          if (isEnergy) {
            bond.setColix(setColix(colix, pid, bond));
            ((HBond)bond).setPaletteID(pid);
          } else {
            bond.setColix(Graphics3D.getColix(JmolConstants.getArgbHbondType(bond.getOrder())));
          }
        }
        return;
      }
      if (colix == Graphics3D.USE_PALETTE && pid != JmolConstants.PALETTE_CPK)
        return; //palettes not implemented for bonds
      BondIterator iter = (selectedBonds != null ? modelSet.getBondIterator(selectedBonds)
          : modelSet.getBondIterator(myMask, bsSelected));
      while (iter.hasNext()) {
        int iBond = iter.nextIndex();
        Bond bond = iter.next();
        bond.setColix(colix);
        bsColixSet.set(iBond, (colix != Graphics3D.INHERIT_ALL
            && colix != Graphics3D.USE_PALETTE));
      }
      return;
    }
    if ("translucency" == propertyName) {
      if (bsColixSet == null)
        bsColixSet = new BitSet();
      boolean isTranslucent = (((String) value).equals("translucent"));
      BondIterator iter = (selectedBonds != null ? modelSet.getBondIterator(selectedBonds)
          : modelSet.getBondIterator(myMask, bsSelected));
      while (iter.hasNext()) {
        bsColixSet.set(iter.nextIndex());
        iter.next().setTranslucent(isTranslucent, translucentLevel);
      }
      return;
    }
    
    if ("deleteModelAtoms" == propertyName) {
      return;
    }
    
    super.setProperty(propertyName, value, bsSelected);
  }

  public Object getProperty(String property, int index) {
    if (property.equals("selectionState"))
      return (selectedBonds != null ? "select BONDS " + Escape.escape(selectedBonds) + "\n":"");
    if (property.equals("sets"))
      return new BitSet[] { bsOrderSet, bsSizeSet, bsColixSet };
    return null;
  }

  public void setModelClickability() {
    Bond[] bonds = modelSet.getBonds();
    for (int i = modelSet.getBondCount(); --i >= 0;) {
      Bond bond = bonds[i];
      if ((bond.getShapeVisibilityFlags() & myVisibilityFlag) == 0
          || modelSet.isAtomHidden(bond.getAtomIndex1())
          || modelSet.isAtomHidden(bond.getAtomIndex2()))
        continue;
      bond.getAtom1().setClickable(myVisibilityFlag);
      bond.getAtom2().setClickable(myVisibilityFlag);
    }
  }

  public String getShapeState() {
    Hashtable temp = new Hashtable();
    Hashtable temp2 = new Hashtable();
    boolean haveTainted = false;
    Bond[] bonds = modelSet.getBonds();
    for (int i = modelSet.getBondCount(); --i >= 0;) {
      Bond bond = bonds[i];
      short r;
      if (reportAll || bsSizeSet != null && bsSizeSet.get(i))
        setStateInfo(temp, i, "wireframe "
            + ((r = bond.getMad()) == 1 ? "on" : "" + (r / 2000f)));
      if (reportAll || bsOrderSet != null && bsOrderSet.get(i)
          && (bond.getOrder() & JmolConstants.BOND_NEW) == 0)
        setStateInfo(temp, i, "bondOrder "
            + JmolConstants.getBondOrderNameFromOrder(bond.getOrder()));
      if (bsColixSet != null && bsColixSet.get(i)) {
        short colix = bond.getColix();
        if ((colix & Graphics3D.OPAQUE_MASK) == Graphics3D.USE_PALETTE)
          setStateInfo(temp, i, getColorCommand("bonds",
              JmolConstants.PALETTE_CPK, colix));
        else
          setStateInfo(temp, i, getColorCommand("bonds", colix));
      }
    }

    return getShapeCommands(temp, null, -1, "select BONDS") + "\n"
        + (haveTainted ? getShapeCommands(temp2, null, -1, "select BONDS") + "\n"
             : "");
  }
  
  public Point3fi checkObjectClicked(int x, int y, int modifiers,
                                    BitSet bsVisible) {
    Point3fi pt = new Point3fi();
    Bond bond = findPickedBond(x, y, bsVisible, pt);
    if (bond == null)
      return null;
    viewer.setStatusAtomPicked(-3, "[\"bond\",\"" + bond.getIdentity() + "\"," + pt.x + "," + pt.y + "," + pt.z + "]");
    return pt;
  }

  private final static int MAX_BOND_CLICK_DISTANCE_SQUARED = 10 * 10;
  private final Point3i ptXY = new Point3i();

  private Bond findPickedBond(int x, int y, BitSet bsVisible, Point3fi pt) {
    int dmin2 = MAX_BOND_CLICK_DISTANCE_SQUARED;
    if (g3d.isAntialiased()) {
      x <<= 1;
      y <<= 1;
      dmin2 <<= 1;
    }
    Bond pickedBond = null;
    Point3f v = new Point3f();
    Bond[] bonds = modelSet.getBonds();
    for (int i = modelSet.getBondCount(); --i >= 0;) {
      Bond bond = bonds[i];
      if (bond.getShapeVisibilityFlags() == 0)
        continue;
      Atom atom1 = bond.getAtom1();
      Atom atom2 = bond.getAtom2();
      if (!atom1.isVisible(0) || !atom2.isVisible(0))
        continue;
      v.set(atom1);
      v.add(atom2);
      v.scale(0.5f);
      int d2 = coordinateInRange(x, y, v, dmin2, ptXY);
      if (d2 >= 0) {
        dmin2 = d2;
        pickedBond = bond;
        pt.set(v);
        pt.modelIndex = atom1.modelIndex;
      }
    }
    return pickedBond;
  }
}
