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
 * @author Joel Freyss
 */
package com.actelion.research.chem.calculator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.FFMolecule;
import com.actelion.research.chem.conf.VDWRadii;
import com.actelion.research.util.datamodel.IntQueue;

/**
 * BondsCalculator is used to recreate the bonds and / or calculate the bonds orders 
 * based on the 3D coordinates of the atoms
 * 
 */
public class BondsCalculator {
	
	/**
	 * Calculates the bonds of a molecule by checking the distance between
	 * all atoms. 
	 * 
	 * Complexity O(nAtoms)
	 * Memory O(nAtoms)
	 * 
	 * @param mol
	 * @param which
	 * @throws ActelionException
	 */
	public static void createBonds(FFMolecule mol, boolean lenient, Map<Integer, String> atomToGroup) throws Exception {
		if(mol.getAllAtoms()==0) return;
		boolean displayWarning = true;
		//1. Create a grid 
		MoleculeGrid grid = new MoleculeGrid(mol);
		TreeSet<Integer> atomsToRemove = new TreeSet<Integer>();
	 	List<int[]> potentialBonds = new ArrayList<int[]>();
	 	
		//2. For each atom, check the neighbours and
		//   Create a connection if the distance is close to the sum of VDW 		
		for(int i=0; i<mol.getAllAtoms(); i++) {			
			if(atomsToRemove.contains(i)) continue;
			int atomicNo = mol.getAtomicNo(i);
			if(atomicNo!=1 && atomicNo!=6 && atomicNo!=7 && atomicNo!=8 && atomicNo!=9 && atomicNo!=15 && atomicNo!=16 && atomicNo!=17 && atomicNo!=26 && atomicNo!=35 && atomicNo!=53) continue;
			
			
			//Get the neighbours 
			Set<Integer> set = grid.getNeighbours(mol.getCoordinates(i), 3.2, false);
			for(int j:set) {
				if(i>=j || atomsToRemove.contains(j)) continue;
				
				atomicNo = mol.getAtomicNo(j);
				if(atomicNo!=1 && atomicNo!=6 && atomicNo!=7 && atomicNo!=8 && atomicNo!=9 && atomicNo!=15 && atomicNo!=16 && atomicNo!=17 && atomicNo!=26 && atomicNo!=35 && atomicNo!=53) continue;
				
				double dist = Math.sqrt(mol.getCoordinates(i).distanceSquared(mol.getCoordinates(j)));
				double idealDist = VDWRadii.COVALENT_RADIUS[mol.getAtomicNo(i)] + VDWRadii.COVALENT_RADIUS[mol.getAtomicNo(j)];

				if(!match(atomToGroup.get(i), atomToGroup.get(j)) || (mol.getAllAtoms()>200 && ((j-i)>12 && (j-i)>mol.getAllAtoms()/50))) {
					if(dist>idealDist + .45) continue;
					potentialBonds.add(new int[]{i, j});
					continue;
				} else {					
					if(dist>idealDist + .45) continue;
				}

				if((mol.getAtomicNo(i)==7 && mol.getAllConnAtoms(i)>=4) || (mol.getAtomicNo(j)==7 && mol.getAllConnAtoms(j)>=4) || (mol.getAtomicNo(i)==6 && mol.getAllConnAtoms(i)>=4) || (mol.getAtomicNo(j)==6 && mol.getAllConnAtoms(j)>=4) || (mol.getAtomicNo(i)==8 && mol.getAllConnAtoms(i)>=2) || (mol.getAtomicNo(j)==8 && mol.getAllConnAtoms(j)>=2) ) {
					if(!lenient) throw new Exception("The valence of atoms " + i + "("+mol.getAtomDescription(i)+") or " + j  + "("+mol.getAtomDescription(j)+") is too high");
					
					if(!displayWarning) {
						System.err.println("The valence of atoms " + i + " or " + j + " is too high");
						displayWarning = false;
					} 						
					continue;
				}
				try {
					mol.addBond(i, j, 1);
				} catch (Exception e) {
					if(!lenient) throw e;
				}
			}

		}
		//System.out.println(potentialBonds.size()+" potential bonds");
		if(potentialBonds.size()<mol.getAllAtoms()/30) {
			for (int[] pair: potentialBonds) {
				try {
					mol.addBond(pair[0], pair[1], 1);
				} catch (Exception e) {
					if(!lenient) throw e;
				}				
			}
		}
		
		if(atomsToRemove.size()>0) {
			if(!lenient && atomsToRemove.size()>4) throw new Exception(atomsToRemove.size()+" atoms in close proximity");
			System.err.println(atomsToRemove.size()+" atoms in close proximity ("+mol.getAtomDescription(atomsToRemove.first())+")");
			
			//for (Integer integer : atomsToRemove) mol.setAtomFlag(integer, FFMolecule.FLAG1, true);
			
			mol.deleteAtoms(new ArrayList<Integer>(atomsToRemove));
		}
		//System.out.println("Create bonds in "+(System.currentTimeMillis()-s)+"ms");
	}
	
	private static boolean match(String g1, String g2) {
		return g1.equals(g2);
		/*
		String s1[] = g1.split(" ");
		String s2[] = g2.split(" ");
		for (String ss1 : s1) {
			for (String ss2 : s2) {
				if(ss1.equals(ss2)) return true;
			}			
		}
		return false;
		*/
	}
	
	/**
	 * Calculate the bond orders of the molecule (without knowing the hydrogens).
	 * The calculation is based on the bond distance between each atoms.
	 * 
	 * 
	 * http://www.ccp14.ac.uk/ccp/web-mirrors/i_d_brown/valence.txt
	 * s = exp((Ro - R)/B)
	 *
	 * @param mol
	 */
	public static void calculateBondOrders(FFMolecule mol, boolean lenient) throws Exception {

		int N = mol.getAllBonds();
		boolean visited[] = new boolean[N];

		//Find rings
		List<int[]> allRings = new ArrayList<int[]>();
		List<Integer>[] atomToRings = StructureCalculator.getRings(mol, allRings);

		//
		//Hybridization State Determination
		int[] spOrder = new int[mol.getAllAtoms()];
		for(int i=0; i<mol.getAllAtoms(); i++) {
			
			if(mol.getConnAtoms(i)<=1) {
				spOrder[i] = 1;
			} else if(mol.getConnAtoms(i)==2) {				
				double angle = GeometryCalculator.getAngle(mol, mol.getConnAtom(i, 0), i, mol.getConnAtom(i, 1));
				if(Math.abs(angle-Math.PI)<Math.PI/6) spOrder[i] = 1;
				else spOrder[i] = 2; 								
			} else if(mol.getConnAtoms(i)==3) {
				Coordinates c = GeometryCalculator.getCoordinates(mol, i);
				Coordinates u = c.subC(mol.getCoordinates(mol.getConnAtom(i, 0)));
				Coordinates v = c.subC(mol.getCoordinates(mol.getConnAtom(i, 1)));
				Coordinates w = c.subC(mol.getCoordinates(mol.getConnAtom(i, 2)));
				Coordinates normal = u.cross(v);
				if(normal.distSq()>0) { 
					double proj = normal.unit().dot(w) / w.dist();
					if(Math.abs(proj)<0.3) spOrder[i] = 2;
					else spOrder[i] = 3;
				} else {
					spOrder[i] = 3;
				}
			}
		}
		
		//////////////////////////////////
		// Functional Group Recognition
		
		for(int i=0; i<mol.getAllAtoms(); i++) {
			if(mol.getAllConnAtoms(i)==2) {
				//RN=N=N
				
				
			} else if(mol.getAllConnAtoms(i)==3) {
				int a, b;
				int a1 = mol.getConnAtom(i, 0);
				int a2 = mol.getConnAtom(i, 1);
				int a3 = mol.getConnAtom(i, 2);
				if(mol.getAtomicNo(i)==6 && spOrder[i]==2) {
					if(mol.getRingSize(i)>0) continue;

					//C(R)(O)(=O)
					if( (mol.getAtomicNo(a2)==8 && mol.getAtomicNo(a3)==8 && mol.getAllConnAtoms(a2)==1 && mol.getAllConnAtoms(a3)==1) ||
						(mol.getAtomicNo(a1)==8 && mol.getAtomicNo(a3)==8 && mol.getAllConnAtoms(a1)==1 && mol.getAllConnAtoms(a3)==1) ||
						(mol.getAtomicNo(a1)==8 && mol.getAtomicNo(a2)==8 && mol.getAllConnAtoms(a1)==1 && mol.getAllConnAtoms(a2)==1)) {
							mol.setBondOrder(shortestBond(mol, i, 8, false), 2); continue;
					}										
					
					//C(R)(OR)(=O)
					a = connectedAtom(mol, i, 8, 2, 0, 0);
					b = connectedBond(mol, i, 8, 1);
					if(a>=0 && b>=0) {mol.setBondOrder(b, 2); continue;} 
					
					//C(R)(SR)(=O)
					a = connectedAtom(mol, i, 16, 2, 0, 0);
					b = connectedBond(mol, i, 8, 1);
					if(a>=0 && b>=0) { mol.setBondOrder(b, 2); continue;} 
					
					//C(R)(NR)(=O)
					a = connectedAtom(mol, i, 7, 2, 0, 0);
					b = connectedBond(mol, i, 8, 1);
					if(a>=0 && b>=0) {mol.setBondOrder(b, 2); continue;} 
					
					//C(R)(SR)(=S)
					a = connectedAtom(mol, i, 16, 2, 0, 0);
					b = connectedBond(mol, i, 16, 1);
					if(a>=0 && b>=0) {mol.setBondOrder(b, 2); continue;} 
					
					//C(R)(NR)(=S)
					a = connectedAtom(mol, i, 7, 2, 0, 0);
					b = connectedBond(mol, i, 16, 1);
					if(a>=0 && b>=0) {mol.setBondOrder(b, 2); continue;}
					
					
					//C(CR)(N)(=N)
					if((mol.getAtomicNo(a1)==6 && mol.getAtomicNo(a2)==7 && mol.getAtomicNo(a3)==7 && mol.getAllConnAtoms(a2)==1 && mol.getAllConnAtoms(a3)==1) ||
						(mol.getAtomicNo(a1)==7 && mol.getAtomicNo(a2)==6 && mol.getAtomicNo(a3)==7 && mol.getAllConnAtoms(a1)==1 && mol.getAllConnAtoms(a3)==1) ||
						(mol.getAtomicNo(a1)==7 && mol.getAtomicNo(a2)==7 && mol.getAtomicNo(a3)==6 && mol.getAllConnAtoms(a1)==1 && mol.getAllConnAtoms(a2)==1)) {
							mol.setBondOrder(shortestBond(mol, i, 7, true), 2); continue;
					}				
					//C(NR)(N)(=N) -> Arginin
					if(mol.getAtomicNo(a1)==7 && mol.getAtomicNo(a2)==7 && mol.getAtomicNo(a3)==7) {
						if(mol.getConnAtoms(a1)==2 && mol.getConnAtoms(a2)==1 && mol.getConnAtoms(a3)==1) {
							if(mol.getCoordinates(i).distSquareTo(mol.getCoordinates(a2))<mol.getCoordinates(i).distSquareTo(mol.getCoordinates(a3))) {mol.setBondOrder(mol.getConnBond(i, 1), 2); continue;}
							else {mol.setBondOrder(mol.getConnBond(i, 2), 2); continue;}
						} else if(mol.getConnAtoms(a1)==1 && mol.getConnAtoms(a2)==2 && mol.getConnAtoms(a3)==1) {
							if(mol.getCoordinates(i).distSquareTo(mol.getCoordinates(a1))<mol.getCoordinates(i).distSquareTo(mol.getCoordinates(a3))) {mol.setBondOrder(mol.getConnBond(i, 0), 2); continue;}
							else {mol.setBondOrder(mol.getConnBond(i, 2), 2); continue;}
						} else if(mol.getConnAtoms(a1)==1 && mol.getConnAtoms(a2)==1 && mol.getConnAtoms(a3)==2) {
							if(mol.getCoordinates(i).distSquareTo(mol.getCoordinates(a1))<mol.getCoordinates(i).distSquareTo(mol.getCoordinates(a2))) {mol.setBondOrder(mol.getConnBond(i, 0), 2); continue;}
							else {mol.setBondOrder(mol.getConnBond(i, 1), 2); continue;}
						}
					}								
					/*
					//C(NR)(NR)(=NR)
					if(mol.getAtomicNo(a1)==7 && mol.getAtomicNo(a2)==7 && mol.getAtomicNo(a3)==7 ) {
						
						mol.setBondOrder(shortestBond(mol, i, 7, true), 2); continue;
					}
					*/

				} else if(mol.getAtomicNo(i)==7) {
					//N(R)(R)C=O -> Amide
					a = connectedAtom(mol, i, 6, 2, 8, 1);
					b = connectedBond(mol, a, 8, 1);
					if(a>=0 && b>=0) {mol.setBondOrder(b, 2); continue;} 					

					//N(=O)(=O) -> Nitro
					for (int j = 0; j < mol.getAllConnAtoms(i); j++) {
						if(mol.getAtomicNo(a1)==8 && mol.getAllConnAtoms(a1)==1 && mol.getAtomicNo(a2)==8 && mol.getAllConnAtoms(a2)==1) {
							mol.setBondOrder(mol.getConnBond(i,0), 2);
							mol.setBondOrder(mol.getConnBond(i,1), 2);
						} else if(mol.getAtomicNo(a1)==8 && mol.getAllConnAtoms(a1)==1 && mol.getAtomicNo(a3)==8 && mol.getAllConnAtoms(a3)==1) {
							mol.setBondOrder(mol.getConnBond(i,0), 2);
							mol.setBondOrder(mol.getConnBond(i,2), 2);
						} else if(mol.getAtomicNo(a2)==8 && mol.getAllConnAtoms(a2)==1 && mol.getAtomicNo(a3)==8 && mol.getAllConnAtoms(a3)==1) {
							mol.setBondOrder(mol.getConnBond(i,1), 2);
							mol.setBondOrder(mol.getConnBond(i,2), 2);
						} 
					}
					
					if(a>=0 && b>=0) {mol.setBondOrder(b, 2); continue;} 					
					
				} 
			} else if(mol.getAllConnAtoms(i)==4) {
				if(mol.getAtomicNo(i)==16) {
					int count = 0;
					for(int j=0; count<2 && j<mol.getAllConnAtoms(i); j++) {
						if(mol.getAtomicNo(mol.getConnAtom(i, j))==8 && mol.getAllConnAtoms(mol.getConnAtom(i, j))==1) {
							mol.setBondOrder(mol.getConnBond(i, j), 2);
							count++;
						}
					}
					for(int j=0; count<2 && j<mol.getAllConnAtoms(i); j++) {
						if(mol.getAtomicNo(mol.getConnAtom(i, j))==7 && mol.getAllConnAtoms(mol.getConnAtom(i, j))==1) {
							mol.setBondOrder(mol.getConnBond(i, j), 2);
							count++;
						}
					}
				} else if(mol.getAtomicNo(i)==15) {
/*					int b = shortestBond(mol, i, 8, false);
					if( b>=0) {
						if((mol.getBondAtom(0, b)==i && mol.getAllConnAtoms(mol.getBondAtom(1, b))==1) ||
							(mol.getBondAtom(1, b)==i && mol.getAllConnAtoms(mol.getBondAtom(0, b))==1)) { 
								mol.setBondOrder(b, 2);
						}
					}*/
				}
			}
		}
		//Preliminary pass: process obvious bonds outside rings		
		for (int i = 0; i < mol.getAllBonds(); i++) {
			int a1 = mol.getBondAtom(0, i);
			int a2 = mol.getBondAtom(1, i);
			if(atomToRings[a1].size()>0 || atomToRings[a2].size()>0) continue;
			if(StructureCalculator.getImplicitHydrogens(mol, a1)==0 || StructureCalculator.getImplicitHydrogens(mol, a2)==0) continue;
			if(!isPlanar(mol, a1, a2)) continue;

			double order = getLikelyOrder(mol, a1, a2);
			if(order>3.0 && spOrder[a1]==1 && spOrder[a2]==1 && StructureCalculator.getImplicitHydrogens(mol, a1)>=2 && StructureCalculator.getImplicitHydrogens(mol, a2)>=2) {
				mol.setBondOrder(i, 3);
				visited[i] = true;
			} else if(order>2.6 && spOrder[a1]<=2 && spOrder[a2]<=2 ) {
				mol.setBondOrder(i, 2);
				visited[i] = true;
			}
			
		}


		/////////////////////////////////////////////////////////
		// Aromatic Ring Perception
		// This procedure calculates a normal to the ring and check that all 
		// atoms in the ring and their neighbours are within the plane defined by the normal
		
		//int[] pyrolles = new int[allRings.size()];
		//int[] oxo = new int[allRings.size()];
		//int[] toDo = new int[mol.getAllAtoms()];
		boolean[] aromaticRing = new boolean[allRings.size()];
		for (int size = 5; size <= 6; size++) 			
		for (int ringNo = 0; ringNo < allRings.size(); ringNo++) {
			int[] ring = allRings.get(ringNo);
			if(ring.length!=size) continue;  
						
			Coordinates c0 = mol.getCoordinates(ring[0]);
			Coordinates c1 = mol.getCoordinates(ring[1]);
			Coordinates c2 = mol.getCoordinates(ring[2]);
			Coordinates normal = c1.subC(c0).cross(c1.subC(c2));
			if(normal.distSq()==0) continue;
			//int startIndex = 0;
			boolean planar = true;
			
			for(int i=0; i<ring.length && planar; i++) {
				Coordinates c3 = mol.getCoordinates(ring[i]);
				Coordinates w = c1.subC(c3);
				if(Math.abs(normal.unit().dot(w) / w.dist())>0.3) {planar=false;}
				
				//Make sure that all carbon in the ring are planar
				if(mol.getAtomicNo(ring[i])==6 || mol.getAtomicNo(ring[i])==7) {
					if(spOrder[ring[i]]!=2) {planar=false;}
				} else if(mol.getAtomicNo(ring[i])>16) {
					planar=false;  //continue if the ring has a metal atom
				}
			}			
			if(!planar) continue;

			//
			//Special case 1: Histidine (some obfuscated code in order to avoid a SS search)
			if(ring.length==5) {
				//Central C:			
				int start = -1;
				int[] posN = {-1, -1};
				boolean ok = true;
				for(int i=0; ok && i<ring.length; i++) {
					if(mol.getAtomicNo(ring[i])==6 && mol.getAllConnAtoms(ring[i])==3) {start = i;}
					else if(mol.getAllConnAtoms(ring[i])!=2) ok = false;
					else if(mol.getAtomicNo(ring[i])==7) {
						if(posN[0]<0) posN[0] = i;
						else if(posN[1]<0) posN[1] = i;
						else ok = false;
					}
				}
				if(ok && start>=0 && posN[1]>=0) {
					if((start+2)%5==posN[0] && (start+4)%5==posN[1]) {
						mol.setBondOrder(mol.getBondBetween(ring[start], ring[(start+1)%5]), 2);
						mol.setBondOrder(mol.getBondBetween(ring[(start+3)%5], ring[(start+4)%5]), 2);
						continue;	
					} else if((start+2)%5==posN[1] && (start+4)%5==posN[0]) {
						mol.setBondOrder(mol.getBondBetween(ring[start], ring[(start+1)%5]), 2);
						mol.setBondOrder(mol.getBondBetween(ring[(start+3)%5], ring[(start+4)%5]), 2);
						continue;	
					} else if((start+3)%5==posN[0] && (start+1)%5==posN[1]) {
						mol.setBondOrder(mol.getBondBetween(ring[start], ring[(start+4)%5]), 2);
						mol.setBondOrder(mol.getBondBetween(ring[(start+1)%5], ring[(start+2)%5]), 2);
						continue;	
					} else if((start+3)%5==posN[1] && (start+1)%5==posN[0]) {
						mol.setBondOrder(mol.getBondBetween(ring[start], ring[(start+4)%5]), 2);
						mol.setBondOrder(mol.getBondBetween(ring[(start+1)%5], ring[(start+2)%5]), 2);
						continue;	
					}
					
				}
				
			}

			// 
			//Check Huckel's rule and Find the starting position
			int start = -1;
			int nElectrons = 0;
			int nAmbiguousN = 0;
			int nAmbiguousC = 0;
			for(int i=0; i<ring.length; i++) {
				int a1 = ring[(i)%ring.length];				
				int a2 = ring[(i+1)%ring.length];				
				int a0 = ring[(i-1+ring.length)%ring.length];				
				int bnd1 = mol.getBondBetween(a1, a2);
				int bnd2 = mol.getBondBetween(a1, a0);
				if(mol.getAtomicNo(a1)==6) {
					if(mol.getAllConnAtoms(a1)==3 && (connectedAtom(mol, a1, 8, -1, 0, 0)>=0 || connectedAtom(mol, a1, 16, -1, 0, 0)>=0) ) {
						int valence = mol.getConnBondOrder(a1, 0) + mol.getConnBondOrder(a1, 1) + mol.getConnBondOrder(a1, 2);
						if(valence==4 && (mol.getBondOrder(bnd1)==2 || mol.getBondOrder(bnd2)==2)) nElectrons++;
						else if(valence==4) nElectrons += 0;
						else {nAmbiguousC++; nElectrons++;/*if(start<0) start = i; */} 											
					} else { 
						if(mol.getConnAtoms(a1)==3 && start<0) start=i;
						nElectrons++;
					}
				} else if(mol.getAtomicNo(a1)==7) {
					if(mol.getConnAtoms(a1)==3) {
						nElectrons+=2;
					} else if(mol.getConnAtoms(a1)==2) {
						nAmbiguousN++; nElectrons++; 
					} else {
						nElectrons++;
					}
				} else {
					nElectrons+=2;
				}

				if(mol.getBondOrder(bnd2)>1) start = i;
				else if(StructureCalculator.getImplicitHydrogens(mol, a1)>0 && StructureCalculator.getImplicitHydrogens(mol, a0)>0 && 
						(atomToRings[a1].size()==1 || atomToRings[a0].size()==1)) {
					if(mol.getConnAtoms(a1)==3 || mol.getConnAtoms(a0)==3)  start = i;
					else if(start<0) start = i;
				}				
			}
			
			int nPyroles = 0;
			int nOxo = 0;
			int diff = nElectrons%4-2; 
			if(diff<0) {
				nPyroles+=Math.min(-diff, Math.max(0, nAmbiguousN)); nElectrons+=nPyroles;
			} else if(diff>0) {
				nOxo+=Math.min(diff, Math.max(0, nAmbiguousC)); nElectrons-=nOxo;
			}

//			if(ringNo==29) {
			
			if(nElectrons%4!=2) {
				if(ring.length==3) continue; //cyclopropane is of course planar but not aromatic
				boolean ok = false;
				if(diff>0) {
					for (int i = 0; i < ring.length; i++) {
						if(mol.getAtomicNo(ring[i])==7) {							
							//toDo[ring[i]]=2;//Protonated N?
							ok=true;
						}
					}					
				} 
				if(!ok) {
					if(!lenient) throw new Exception("Huckel's rule not verified");
					continue;
				}
			}
			aromaticRing[ringNo] = true;
			/*
			if(start<0) start = 0;
			pyrolles[ringNo] = nPyroles;
			oxo[ringNo] = nOxo;
			/*
			for(int i=0; i<ring.length; i++) {
				int a1 = ring[i];
				if(StructureCalculator.getImplicitHydrogens(mol, a1)==0) continue;
				if(mol.getAtomicNo(a1)==7) {
					if(nPyroles>0) {
						toDo[a1] = 2; //This N may not need a double bond
					} else {
						toDo[a1] = 1; //this N needs a double bond
					}
				} else if(mol.getAtomicNo(a1)==6) {
					double doub = 0; for (int j = 0; j < mol.getAllConnAtoms(a1); j++) if(mol.getConnBondOrder(a1, j)>1) doub++;
					if(doub==0) {
						toDo[a1] = 1;
						if(nOxo>0) { 
							for (int j = 0; j < mol.getAllConnAtoms(a1); j++) {
								int a2 = mol.getConnAtom(a1, j);
								if(mol.getAtomicNo(a2)==8 && mol.getAllConnAtoms(a2)==1) {
									toDo[a2] = 2;
								}
							}
						}
					}
				}				
			}
			*/
			
		}

		//Aromatizer
		//Initialize the visited atoms 
		boolean[] visited2 = new boolean[mol.getAllAtoms()];
		Set<Integer> nonAromaticAtom = new HashSet<Integer>();
		for (int a=0; a<mol.getAllAtoms(); a++) {
			if(StructureCalculator.connected(mol, a, -1, 2)>=0) visited2[a] = true; //This atom has been processed above
			if(mol.getAtomicNo(a)==6) {
				boolean ok = false;
				for(int r: atomToRings[a]) {
					if(aromaticRing[r]) ok = true;
				}
				if(!ok) nonAromaticAtom.add(a);
			}
		}
		
		for (int i = 0; i < aromaticRing.length; i++) {			
			if(aromaticRing[i]) {
				boolean success = aromatize(mol, i, allRings, aromaticRing, nonAromaticAtom, visited2, 0, allRings.get(i).length%2, new ArrayList<Integer>(), true);
				if(!success) success = aromatize(mol, i, allRings, aromaticRing, nonAromaticAtom, visited2, 0, allRings.get(i).length%2, new ArrayList<Integer>(), false);
				if(!success) {
					System.out.println("Could not aromatize ring "+i);
					aromaticRing[i] = false;
				}
			}
		}
		boolean[] aromaticAtoms = new boolean[mol.getAllAtoms()];
		for (int i = 0; i < allRings.size(); i++) {
			if(aromaticRing[i]) {
				for(int atm: allRings.get(i)) {
					aromaticAtoms[atm] = true;
				}
			}
		}

		/////////////////////////////////
		//2nd pass: find obvious double bonds on sp2 carbons outside aromatic rings
		for(int i=0; i<mol.getAllAtoms(); i++) {
			if(spOrder[i]==2 && !aromaticAtoms[i] && mol.getAtomicNo(i)==6 && mol.getAllConnAtoms(i)==3 && StructureCalculator.getImplicitHydrogens(mol, i)>0 && StructureCalculator.connected(mol, i, -1, 2)>=0) {
				int a1 = mol.getConnAtom(i, 0);
				int a2 = mol.getConnAtom(i, 1);
				int a3 = mol.getConnAtom(i, 2);
				double order1, order2, order3;

				if(StructureCalculator.getImplicitHydrogens(mol, a1)==0 && StructureCalculator.connected(mol, a1, -1, 2)>=0) order1 = 1;
				else order1 = getLikelyOrder(mol, i, a1);

				if(StructureCalculator.getImplicitHydrogens(mol, a2)==0 && StructureCalculator.connected(mol, a2, -1, 2)>=0) order2 = 1;
				else order2 = getLikelyOrder(mol, i, a2);
				
				if(StructureCalculator.getImplicitHydrogens(mol, a3)==0 && StructureCalculator.connected(mol, a3, -1, 2)>=0) order3 = 1;
				else order3 = getLikelyOrder(mol, i, a3);
				
				
				//the highest is the most likely to have a double bond
				int connBond = -1;
				if(order1>order2 && order1>order3 && order1>1 /*&& ((mol.getAtomicNo(i)!=6 && mol.getAtomicNo(a1)!=6) || isPlanar(mol, i, a1))*/) {
					connBond = 0;  					
				} else if(order2>order1 && order2>order3 && order2>1 /*&& ((mol.getAtomicNo(i)!=6 && mol.getAtomicNo(a2)!=6) || isPlanar(mol, i, a2))*/) {
					connBond = 1;  					
				} else if(order3>order1 && order3>order2 && order3>1 /*&& ((mol.getAtomicNo(i)!=6 && mol.getAtomicNo(a3)!=6) || isPlanar(mol, i, a3))*/) {
					connBond = 2;  					
				}  	
				
				if(connBond>=0) {
					mol.setBondOrder(mol.getConnBond(i, connBond), 2);
				} 
			}
		}		

		//3rd pass, double bonds inside non aromatic rings
		IntQueue queue = new IntQueue();
		for(int i=0; i<N; i++) {
			if(!visited[i]) queue.push(i);
			while(!queue.isEmpty()) {
				int bnd = queue.pop();
				if(visited[bnd]) continue;
				visited[bnd] = true;
				int atm1 = mol.getBondAtom(0, bnd);
				int atm2 = mol.getBondAtom(1, bnd);
				
				//Push the neighbour bonds into the queue
				for(int j=0; j<mol.getAllConnAtoms(atm1); j++) {
					queue.push(mol.getConnBond(atm1, j));
				}
				for(int j=0; j<mol.getAllConnAtoms(atm2); j++) {
					queue.push(mol.getConnBond(atm2, j));
				}
				
				//Compute the free valence and increase the bond order if needed
				double order = getLikelyOrder(mol, atm1, atm2);

				if(order>2 && !aromaticAtoms[atm1] && !aromaticAtoms[atm2]) {
					if(mol.getValence(atm1)!=mol.getConnAtoms(atm1)) continue; //no adjacent double bonds
					if(mol.getValence(atm2)!=mol.getConnAtoms(atm2)) continue; //no adjacent double bonds
					
					//Special case CS
					if(mol.getAtomicNo(atm1)==16 && mol.getAllConnAtoms(atm1)<=2) continue;
					if(mol.getAtomicNo(atm2)==16 && mol.getAllConnAtoms(atm2)<=2) continue;
					
					int freeValence1 = StructureCalculator.getMaxFreeValence(mol, atm1);
					int freeValence2 = StructureCalculator.getMaxFreeValence(mol, atm2);
						
					//boolean planar = (spOrder[atm1]<3) && (spOrder[atm2]<3); 
					boolean aligned = spOrder[atm1]==1 && spOrder[atm2]==1;					
					if(order>3.0 && freeValence1>1 && freeValence2>1 && aligned) {
						mol.setBondOrder(bnd, 3);
					} else if(freeValence1>0  && freeValence2>0) {
						if((mol.getAtomicNo(atm1)==6 && mol.getAtomicNo(atm2)==6) && !isPlanar(mol, atm1, atm2)) continue;
						if(mol.getAtomicNo(atm1)==6 && spOrder[atm1]>2) continue;
						if(mol.getAtomicNo(atm2)==6 && spOrder[atm2]>2) continue;						 
						mol.setBondOrder(bnd, 2);
					}
				}
					

			}
		}
		
	}
	
	public static boolean aromatize(FFMolecule mol, Set<Integer> aromaticAtoms, Set<Integer> aromaticBonds) {
		List<int[]> rings = mol.getAllRings();
			
		//Flag the aromatic rings
		boolean[] aromaticRings = new boolean[rings.size()];
		for (int i = 0; i < rings.size(); i++) {			
			//Is is an aromatic ring
			boolean isAromatic = true;
			for (int j = -1; j < rings.get(i).length; j++) {
				int a1 = j==-1? rings.get(i)[rings.get(i).length-1]: rings.get(i)[j];
				int a2 = j==rings.get(i).length-1? rings.get(i)[0]: rings.get(i)[j+1];
				
				int b = mol.getBondBetween(a1, a2);
				if(!aromaticBonds.contains(b)) {
					isAromatic = false;
				}
			}
				
			aromaticRings[i] = isAromatic;
		}
		Set<Integer> nonAromaticAtoms = new HashSet<Integer>();
		for (int i=0;i<mol.getAllAtoms();i++) nonAromaticAtoms.add(i);
		nonAromaticAtoms.removeAll(aromaticAtoms);		
		
		//Launch the aromatizer
		boolean ok = true;
		for (int i = 0; i < aromaticRings.length; i++) {
			if(aromaticRings[i]) {
				boolean success = aromatize(mol, i, rings, aromaticRings, nonAromaticAtoms, new boolean[mol.getAllAtoms()], 0, rings.get(i).length%2, new ArrayList<Integer>(), true);
				if(!success) success = aromatize(mol, i, rings, aromaticRings, nonAromaticAtoms, new boolean[mol.getAllAtoms()], 0, rings.get(i).length%2, new ArrayList<Integer>(), false);
				if(!success) {
					System.out.println("Could not aromatize ring "+i);
					aromaticRings[i] = false;
					ok = false;
				}				
			}
		}
		return ok; 
	}
	
	private static boolean aromatize(FFMolecule mol, int index, List<int[]> allRings, boolean[] aromatic, Set<Integer> nonAromaticAtoms, boolean[] visited, int seed, int left, List<Integer> bondsMade, boolean easy) {
		
		//Ends if the ring has been fully visited
		int[] ring = (int[]) allRings.get(index);
		boolean allVisited = true;
		int bestSeed = -1;
		for (int i = 0; i < ring.length; i++) {
			if(!visited[ring[(seed + i)%ring.length]]) {				 
				if(bestSeed<0) bestSeed = seed + i; 
				allVisited = false;				
			}
		}
		if(allVisited) {
			return true;
		} else  {
			seed = bestSeed;
		}
		
		
		int a = ring[seed%ring.length];
		int ap = ring[(seed+1)%ring.length];
		if(visited[a]) { //already treated
			
			return aromatize(mol, index, allRings, aromatic, nonAromaticAtoms, visited, seed+1, left, bondsMade, easy);
			
		} else {
		
			//Try to create a double bond from the atom a to a connected one
			for (int j = -1; j < mol.getAllConnAtoms(a); j++) {
				int a2 = j==-1? ap: mol.getConnAtom(a, j);
				//System.out.println("ring "+index+"."+seed+"("+left+") bond "+a+"("+mol.getAtomicNo(a)+") - "+a2+"("+mol.getAtomicNo(a2)+") "+(StructureCalculator.connected(mol, a, -1, 2)>0)+" "+(StructureCalculator.connected(mol, a2, -1, 2)>0));
				
				if(visited[a2]) continue;
				if(j>=0 && a2==ap) continue;
				
				if(nonAromaticAtoms.contains(a2)) continue;
				if(nonAromaticAtoms.contains(a)) continue;
				
				if(mol.getAtomicNo(a)==8 || mol.getAtomicNo(a)==16) continue;
				if(mol.getAtomicNo(a2)==8 || mol.getAtomicNo(a2)==16) continue;
				if(easy && mol.getValence(a)>=StructureCalculator.getMaxValence(mol.getAtomicNo(a))) continue;
				if(easy && mol.getValence(a2)>=StructureCalculator.getMaxValence(mol.getAtomicNo(a2))) continue;
				if(StructureCalculator.connected(mol, a, -1, 2)>=0) continue;
				if(StructureCalculator.connected(mol, a2, -1, 2)>=0) continue;
				
				visited[a] = visited[a2] = true;
				int b = mol.getBondBetween(a, a2);
				mol.setBondOrder(b, 2);
				
				//Test whole ring 
				List<Integer> trackBondsMade = new ArrayList<Integer>();
				boolean success = aromatize(mol, index, allRings, aromatic, nonAromaticAtoms, visited, seed+1, left, trackBondsMade, easy);

				//Test connecting rings
				if(success) {
					List<Integer> rings = mol.getAtomToRings()[a2];
					if(rings.size()>1) {
						for (int r : rings) {
							if(r!=index && aromatic[r]) {
								int newSeed;
								for(newSeed=0; allRings.get(r)[newSeed]!=a2; newSeed++) {}
							
								//System.out.println("try connected ring "+r+" / "+newSeed);
								success = aromatize(mol, r, allRings, aromatic, nonAromaticAtoms, visited, newSeed, allRings.get(r).length%2, trackBondsMade, easy);
								//System.out.println("try connected ring "+r +" -> " +success+" "+trackBondsMade.size());
								if(!success) break;
							}
						}
					}
				}
				
				if(success) {
					//It works!!!
					bondsMade.add(b);
					bondsMade.addAll(trackBondsMade);
					return true;
				} else {
					//Backtrack changes
					visited[a] = visited[a2] = false;
					mol.setBondOrder(b, 1);
					for (int b2 : trackBondsMade) {
						//System.out.println("retrack "+mol.getBondAtom(0, b2)+"-"+mol.getBondAtom(1, b2));
						mol.setBondOrder(b2, 1);
						visited[mol.getBondAtom(0, b2)] = visited[mol.getBondAtom(1, b2)] = false;
					}
				} 
			}
			
			//Try to skip this atom
			if(left>0 && (mol.getAtomicNo(a)!=6)) {
				visited[a] = true;
				List<Integer> trackBondsMade = new ArrayList<Integer>();
				boolean success = aromatize(mol, index, allRings, aromatic, nonAromaticAtoms, visited, seed+1, left-1, trackBondsMade, easy);
				if(success) {
					bondsMade.addAll(trackBondsMade);
					return true;
				} else {
					visited[a] = false;
					for (int b2 : trackBondsMade) {
						mol.setBondOrder(b2, 1);
						visited[mol.getBondAtom(0, b2)] = visited[mol.getBondAtom(1, b2)] = false;
					}
				}
			}
			
			
			return false;
			
		}		
	}
	
	private static boolean isPlanar(FFMolecule mol, int a1, int a2) {
		Coordinates ci = mol.getCoordinates(a1);		
		Coordinates u = null, v =null;
		
		for (int i = 0; v==null && i < mol.getAllConnAtoms(a1); i++) {
			if(u==null) u = mol.getCoordinates(mol.getConnAtom(a1, i)).subC(ci);
			else {v = mol.getCoordinates(mol.getConnAtom(a1, i)).subC(ci);} 
		}
		for (int i = 0; v==null && i < mol.getAllConnAtoms(a2); i++) {
			if(u==null) u = mol.getCoordinates(mol.getConnAtom(a2, i)).subC(ci);
			else {v = mol.getCoordinates(mol.getConnAtom(a2, i)).subC(ci);} 
		}
		
		if(u==null) return false;
		
		Coordinates normal = u.cross(v);
		if(normal.distSq()==0) return false; //what to do?
		normal = normal.unit();

		Coordinates cj = mol.getCoordinates(a2);					
		for(int k=0; k<mol.getAllConnAtoms(a2); k++) {
			Coordinates ck = mol.getCoordinates(mol.getConnAtom(a2, k));
			if(Math.abs(ck.subC(cj).dot(normal))>0.2) return false;
		}					
		for(int k=0; k<mol.getAllConnAtoms(a1); k++) {
			Coordinates ck = mol.getCoordinates(mol.getConnAtom(a1, k));
			if(Math.abs(ck.subC(cj).dot(normal))>0.2) return false;
		}					
		return true;		
	}
	
	private static double getLikelyOrder(FFMolecule mol, int atm1, int atm2) {	
		int k;				
		for(k=0; k<PARAMS.length; k++) if((PARAMS[k][0]==mol.getAtomicNo(atm1) && PARAMS[k][1]==mol.getAtomicNo(atm2)) || (PARAMS[k][1]==mol.getAtomicNo(atm1) && PARAMS[k][0]==mol.getAtomicNo(atm2))) break;
		if(k>=PARAMS.length) return 1;
						
		//Calculate the order
		double r = GeometryCalculator.getDistance(mol, atm1, atm2);
		return Math.exp((PARAMS[k][2] - r) / PARAMS[k][3]);
	}

	/**
	 * Util function for substructure searches
	 * @param mol
	 * @param a
	 * @param atomicNo
	 * @param valence
	 * @param otherAtomicNo
	 * @param otherValence
	 * @return
	 */
	private final static int connectedAtom(FFMolecule mol, int a, int atomicNo, int valence, int otherAtomicNo, int otherValence) {
		loop: for(int i=0; i<mol.getAllConnAtoms(a); i++) {
			int atm = mol.getConnAtom(a, i);
			if(atomicNo>0 && mol.getAtomicNo(atm)!=atomicNo) continue;
			if(valence>0 && mol.getAllConnAtoms(atm)!=valence) continue;
			if(otherAtomicNo>0 || otherValence>0) {
				for (int j = 0; j < mol.getAllConnAtoms(atm); j++) {
					int otherAtm = mol.getConnAtom(atm, j);
					if(otherAtm==a) continue loop;
					if(otherAtomicNo>0 && mol.getAtomicNo(otherAtm)!=otherAtomicNo) continue loop;
					if(otherValence>0 && mol.getAllConnAtoms(otherAtm)!=otherValence) continue loop;					
				}	
			}
			
			return atm;
		}
		return -1;
	}
	
	private final static int connectedBond(FFMolecule mol, int a, int atomicNo, int valence) {
		if(a<0) return -1;
		for(int i=0; i<mol.getAllConnAtoms(a); i++) {
			int atm = mol.getConnAtom(a, i);
			if(atomicNo>0 && mol.getAtomicNo(atm)!=atomicNo) continue;
			if(valence>0 && mol.getAllConnAtoms(atm)!=valence) continue;
			return mol.getConnBond(a, i);
		}
		return -1;
	}
	private final static int shortestBond(FFMolecule mol, int a, int toAtomicNo, boolean privilegeRing) {
		int bestBond = -1;
		double bestDist = Double.MAX_VALUE;
		for(int i=0; i<mol.getAllConnAtoms(a); i++) {
			int atm = mol.getConnAtom(a, i);					
			if(toAtomicNo>0 && mol.getAtomicNo(atm)!=toAtomicNo) continue;
			if(StructureCalculator.getMaxFreeValence(mol, atm)==0) continue;
			int ring = mol.getRingSize(new int[] {a,atm});
			double dist = GeometryCalculator.getDistance(mol, a, atm) - (privilegeRing && ring>0?2:0);
			if(dist<bestDist) {
				bestDist = dist;
				bestBond = mol.getConnBond(a, i);
			}			
		}
		return bestBond;
	}
	
	final static double[][] PARAMS = new double[][] {
		//AtomicNo, AtomicNo, R0, B
		//s = exp((Ro - R)/B)

		{ 6, 6, 1.523, 0.1855}, //1 > 1.523(16)  2 > 1.3944(25)  3 > 1.212(2)  
		{ 6, 7, 1.470, 0.2458}, //1 > 1.47(28)  2 > 1.2996(10)  3 > 1.158(2)  
		{ 6, 8, 1.410, 0.21}, //1 > 1.435(24)  2 > 1.2884(10)  
		{ 6, 16, 1.815, 0.0690}, //1 > 1.815(12)  2 > 1.7672(20)  
		{ 7, 7, 1.381, 0.1919}, //1 > 1.381(49)  2 > 1.248(4)  3 > 1.23(1)  
		{ 7, 8, 1.310, 0.1500}, //1 > 1.31(42)    
		{ 8, 8, 1.428, 0.0000}, //1 > 1.428(36)  
		{ 8, 15, 1.696, 0.22},   
		{ 8, 16, 1.657, 0.24}, //1 > 1.657  2 > 1.48(8)  
		{ 16, 16, 2.024, 0.36}, //1 > 2.024(9)  2 > 1.771(16)  

	};

	/**
	 * Extract the main structure
	 * @param mol
	 * @return
	 */
	public static void extractMainStructure(FFMolecule mol, FFMolecule res) {
		List<List<Integer>> cc = StructureCalculator.getConnexComponents(mol);
		int[] sizes = new int[cc.size()];
		for (int i = 0; i < sizes.length; i++) sizes[i] = cc.get(i).size();
		
		
		//Find the main structure: take the biggest one
		int biggest = 0;
		for (int i = 0; i < sizes.length; i++) {
			if(sizes[biggest]<sizes[i]) biggest = i;
		}		
		List<Integer> main = cc.get(biggest);
		Coordinates mainMin = mol.getCoordinates(main.get(0));
		Coordinates mainMax = mol.getCoordinates(main.get(0));
		for (int j = 1; j < main.size(); j++) {
			int a = main.get(j);
			mainMin = mainMin.min(mol.getCoordinates(a));
			mainMax = mainMax.max(mol.getCoordinates(a));
		}
		
		//Find the interconnected structures
		List<Integer> toBeAdded = new ArrayList<Integer>();
		MoleculeGrid grid = new MoleculeGrid(mol);
		for (int i = 0; i < cc.size(); i++) {
			if(i==biggest) continue;
			if(sizes[i]>sizes[biggest]*.98) continue;
			List<Integer> g = cc.get(i);
			Coordinates min = mol.getCoordinates(g.get(0));
			Coordinates max = mol.getCoordinates(g.get(0));
			for (int j = 1; j < g.size(); j++) {
				int a = g.get(j);
				min = min.min(mol.getCoordinates(a));
				max = max.max(mol.getCoordinates(a));
			}
			
			if(mainMax.x<min.x || mainMin.x>max.x) continue;
			if(mainMax.y<min.y || mainMin.y>max.y) continue;
			if(mainMax.z<min.z || mainMin.z>max.z) continue;
			
			Coordinates interMin = min.max(mainMin);
			Coordinates interMax = max.min(mainMax);			
						
			double dist = interMin.distance(interMax);
			double dist2 = min.distance(max);
			if(dist>2*dist2/3) {
				Set<Integer> neigh = grid.getNeighbours(new Coordinates[]{min, max}, 4);
				neigh.retainAll(main);
				if(neigh.size()>00) {
					toBeAdded.addAll(g);
				}
			} 
		}
		
		res.clear();				
		main.addAll(toBeAdded);		
		StructureCalculator.copyAtoms(mol, res, main);
	}
	
	/*
	public static void main(String[] args) throws Exception {
		PDBFileParser parser = new PDBFileParser();
		parser.setLoadMainStructureOnly(false);
		FFMolecule m = parser.load("D:\\NoBackup\\PDB\\ENTRIES\\e4\\pdb1e47.ent.gz");
		FFViewer.viewMolecule(m);
	}
	*/
}
