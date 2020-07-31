//====================================================================
// BFBASIC -- Basic programming language compiler for BF
// Filename : Variables.java
// Language : Java 1.2+
// Version  : 1.40
// Copyright: (C) 2001-2005 Jeffry Johnston
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as
// published by the Free Software Foundation. See the file LICENSE
// for more details.
//====================================================================

import java.util.HashMap;
import java.util.LinkedList;

/**
 * The main job of this class is to figure out the ordering of 
 * variables and arrays that results in the smallest file size.
 *  
 * @author Jeffry Johnston
 */
public class Variables {
  
  /** gives the order that the variables have been rearranged to */
  private int[] _order; 
  
  /** answers "which index is variable x at?" */
  private int[] _index;
  
  /** number of memory locations each variable takes */
  private int[] _cellCount;
  
  /** starting memory offset of each variable */
  private int[] _start;
  
  /** 
   * interactions between variables (the order that the variables
   * occur in the source code).  Allows testing the efficiency of a 
   * new ordering  
   */
  private int[] _interaction;
  
  /**
   * Prevents the first "x" elements to be moved.  This is to prevent
   * the internal variables from moving apart from each other, 
   * specifically for hardcoded BF code blocks (such as the string
   * generator).  
   */
  private int _first;
  
  public Variables(String[] names, Integer[] cellCount, 
                   LinkedList interaction, int first) {
    /** name of each variable */
    HashMap nameHash = new HashMap();
    _first = first;
    _cellCount = new int[names.length];
    _order = new int[names.length];
    _index = new int[names.length];
    _start = new int[names.length];
    int start = 0;
    for (int i = 0; i < names.length; i++) {
      _order[i] = i;
      _index[i] = i;
      _start[i] = start;
      _cellCount[i] = cellCount[i].intValue();
      start += _cellCount[i];
      nameHash.put(names[i], new Integer(i));
    }
    _interaction = new int[interaction.size()];
    for (int i = 0; i < interaction.size(); i++) {
      _interaction[i] = ((Integer)nameHash.get(interaction.get(i))).intValue();
    }
  }

  /**
   * Swaps two variables
   * @param l Left variable
   * @param r Right variable
   * @return
   */
  public void swap(int l, int r) {
    // update index values
    int temp = _index[_order[l]];
    _index[_order[l]] = _index[_order[r]];
    _index[_order[r]] = temp;
    
    // change the order
    temp = _order[l];
    _order[l] = _order[r];
    _order[r] = temp;
    
    // swap cell counts
    temp = _cellCount[l];
    _cellCount[l] = _cellCount[r];
    _cellCount[r] = temp;
    
    // update variable start positions
    for (int i = l; i < r; i++) {
      _start[i + 1] = _start[i] + _cellCount[i];
    }
  }
  
  /**
   * @return Current ordering (rearrangment) of the variables.
   */
  public int[] getOrder() {
    return _order;
  }

  /**
   * @return memory positions of each variable, in the order given by
   *         getOrder().
   */
  public int[] getStart() {
    return _start;
  }

  /**
   * Calls makeBestMove until it returns false, meaning it has found
   * the best arrangement it knows about.
   */
  public void findBestOrder() {
    System.out.print("Optimizing...");
    while (makeBestMove()) {
      System.out.print(".");
    }
    System.out.println();
  }

  /**
   * @return Determines the cost in >>> <<< arrows of the current 
   *         variable ordering.
   */
  private int numArrows() {
    int mp = 0, total = 0, newmp;
    for (int i = 0; i < _interaction.length; i++) {
      newmp = _start[_index[_interaction[i]]];
      total += Math.abs(newmp - mp);
      mp = newmp;
    }
    return total;
  }
  
  /**
   * Attempts to move each variable right one at a time, testing to 
   * see which movement helps the most.  Note that since a move right
   * is like a swap, the same variable position is moved right again 
   * to restore the original ordering.  If a better ordering has been
   * found then the variables are left in that order.  
   * 
   * @return true if a superior ordering has been found, false if not.
   */
  private boolean makeBestMove() {
    int best = numArrows();
    boolean moved = false;
    for (int l = _first; l < _order.length - 1; l++) {
      for (int r = l + 1; r < _order.length - 1; r++) {
        swap(l, r);
        int curr = numArrows();
        if (curr < best) {
          best = curr;
          moved = true;
        } else {
          swap(l, r);
        }
      }
    }
    return moved;
  }
 
}

