/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.lib.regexp;

import java.util.*;

import com.caucho.util.*;
import com.caucho.quercus.env.StringValue;

class RegexpNode {
  private static final L10N L = new L10N(RegexpNode.class);
  
  static final int RC_END = 0;
  static final int RC_NULL = 1;
  static final int RC_STRING = 2;
  static final int RC_SET = 3;
  static final int RC_NSET = 4;
  static final int RC_BEG_GROUP = 5;
  static final int RC_END_GROUP = 6;
  
  static final int RC_GROUP_REF = 7;
  static final int RC_LOOP = 8;
  static final int RC_LOOP_INIT = 9;
  static final int RC_LOOP_SHORT = 10;
  static final int RC_LOOP_UNIQUE = 11;
  static final int RC_LOOP_SHORT_UNIQUE = 12;
  static final int RC_LOOP_LONG = 13;
  
  static final int RC_OR = 64;
  static final int RC_OR_UNIQUE = 65;
  static final int RC_POS_LOOKAHEAD = 66;
  static final int RC_NEG_LOOKAHEAD = 67;
  static final int RC_POS_LOOKBEHIND = 68;
  static final int RC_NEG_LOOKBEHIND = 69;
  static final int RC_LOOKBEHIND_OR = 70;
  
  static final int RC_WORD = 73;
  static final int RC_NWORD = 74;
  static final int RC_BLINE = 75;
  static final int RC_ELINE = 76;
  static final int RC_BSTRING = 77;
  static final int RC_ESTRING = 78;
  static final int RC_ENSTRING = 79;
  static final int RC_GSTRING = 80;
  
  // conditionals
  static final int RC_COND = 81;
  
  // ignore case
  static final int RC_STRING_I = 128;
  static final int RC_SET_I = 129;
  static final int RC_NSET_I = 130;
  static final int RC_GROUP_REF_I = 131;

  static final int RC_LEXEME = 256;
  
  // unicode properties
  static final int RC_UNICODE = 512;
  static final int RC_NUNICODE = 513;

  // unicode properties sets
  static final int RC_C = 1024;
  static final int RC_L = 1025;
  static final int RC_M = 1026;
  static final int RC_N = 1027;
  static final int RC_P = 1028;
  static final int RC_S = 1029;
  static final int RC_Z = 1030;
  
  // negated unicode properties sets
  static final int RC_NC = 1031;
  static final int RC_NL = 1032;
  static final int RC_NM = 1033;
  static final int RC_NN = 1034;
  static final int RC_NP = 1035;
  static final int RC_NS = 1036;
  static final int RC_NZ = 1037;
  
  // POSIX character classes
  static final int RC_CHAR_CLASS = 2048;
  static final int RC_ALNUM = 1;
  static final int RC_ALPHA = 2;
  static final int RC_BLANK = 3;
  static final int RC_CNTRL = 4;
  static final int RC_DIGIT = 5;
  static final int RC_GRAPH = 6;
  static final int RC_LOWER = 7;
  static final int RC_PRINT = 8;
  static final int RC_PUNCT = 9;
  static final int RC_SPACE = 10;
  static final int RC_UPPER = 11;
  static final int RC_XDIGIT = 12;
  
  /*
  static final int RC_C = 512;
  static final int RC_CC = 513;
  static final int RC_CF = 514;
  static final int RC_CN = 515;
  static final int RC_CO = 516;
  static final int RC_CS = 517;
  static final int RC_L = 518;
  static final int RC_LL = 519;
  static final int RC_LM = 520;
  static final int RC_LO = 521;
  static final int RC_LT = 522;
  static final int RC_LU = 523;
  static final int RC_M = 524;
  static final int RC_MC = 525;
  static final int RC_ME = 526;
  static final int RC_MN = 527;
  static final int RC_N = 528;
  static final int RC_ND = 529;
  static final int RC_NL = 530;
  static final int RC_NO = 531;
  static final int RC_P = 532;
  static final int RC_PC = 533;
  static final int RC_PD = 534;
  static final int RC_PE = 535;
  static final int RC_PF = 536;
  static final int RC_PI = 537;
  static final int RC_PO = 538;
  static final int RC_PS = 539;
  static final int RC_S = 540;
  static final int RC_SC = 541;
  static final int RC_SK = 542;
  static final int RC_SM = 543;
  static final int RC_SO = 544;
  static final int RC_Z = 545;
  static final int RC_ZL = 546;
  static final int RC_ZP = 547;
  static final int RC_ZS = 548;
  */
  
  public static final int FAIL = -1;
  public static final int SUCCESS = 0;
  
  static RegexpNode END = RegexpNode.create(RC_END);
  static RegexpNode NULL = RegexpNode.create(RC_NULL);
  
  static final RegexpNode N_END = new End();
  static final RegexpNode DOT;
  static final RegexpNode ANY_CHAR;

  RegexpNode _rest;
  
  //for lookbehind
  int _length;
  
  static int _count = 0;
  int _id = -1;

  
  /**
   * Creates a node with a code
   */
  protected RegexpNode()
  {
    _rest = END;
    
    _id = _count++;
  }
  
  /**
   * Creates a node with a code
   */
  static RegexpNode create(int code)
  {
    return new Compat(code);
  }

  /**
   * Creates a node with a group index
   */
  static RegexpNode create(int code, int index)
  {
    return new Compat(code, index);
  }

  /**
   * Creates a node with a group index
   */
  static RegexpNode create(int code, RegexpNode branch)
  {
    return new Compat(code, branch);
  }

  /**
   * Creates a node with a group index
   */
  static RegexpNode create(int code, int index, int min, int max)
  {
    return new Compat(code, index, min, max);
  }

  /**
   * Creates a node with a group index
   */
  static RegexpNode create(int code, RegexpSet set)
  {
    return new Compat(code, set);
  }

  //
  // parsing constructors
  //

  RegexpNode concat(RegexpNode next)
  {
    return new Concat(this, next);
  }

  /**
   * '?' operator
   */
  RegexpNode createOptional()
  {
    return createLoop(0, 1);
  }

  /**
   * '*' operator
   */
  RegexpNode createStar()
  {
    return createLoop(0, Integer.MAX_VALUE);
  }

  /**
   * '+' operator
   */
  RegexpNode createPlus()
  {
    return createLoop(1, Integer.MAX_VALUE);
  }

  /**
   * Any loop
   */
  RegexpNode createLoop(int min, int max)
  {
    return new Loop(this, min, max);
  }

  //
  // optimization functions
  //

  int minLength()
  {
    return 0;
  }

  String prefix()
  {
    return "";
  }

  //
  // matching
  //
  
  int match(StringValue string, int offset, RegexpState state)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  int match(CharCursor cursor, Regexp state)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public Object clone()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public String toString()
  {
    return "RegexpNode[]";
  }

  static RegexpNode removeTail(RegexpNode head)
  {
    RegexpNode tail = head._rest;
    
    if (head == null || tail == null)
      return null;
    
    if (tail._rest == null) {
      head._rest = null;
      return tail;
    }
    else
      return removeTail(tail);
  }
  
  /*
   * Cuts out the non-null tail of this node.
   */
  static RegexpNode spliceNonNullTail(RegexpNode head)
  {
    RegexpNode tail = head._rest;
    
    if (head == null || tail == null)
      return null;

    if (tail._rest == null || tail == END || tail == NULL) {
      head._rest = null;
      
      tail._rest = null;
      return tail;
    }
    else
      return spliceNonNullTail(tail);
  }
  
  /**
   * Replaces the tail of a node.
   */
  static RegexpNode replaceTail(RegexpNode node, RegexpNode tail)
  {
    if (node == null || node == END || node == tail)
      return tail;

    Compat compat = (Compat) node;
    if (compat._code == RC_OR)
      compat._branch = replaceTail(compat._branch, tail);

    node._rest = replaceTail(node._rest, tail);

    return node;
  }

  /**
   * Connects lastBegin to the tail, returning the head;
   */
  static RegexpNode concat(RegexpNode head, RegexpNode tail)
  {
    if (head == null || head == END)
      return tail;

    RegexpNode node = head;
    while (node._rest != null && node._rest != END)
      node = node._rest;

    node._rest = tail;

    return head;
  }
  
  public static String code(RegexpNode node)
  {
    if (node == null)
      return "null";
    else
      return code(((Compat) node)._code) + node._id;
  }
  
  public static String code(int code)
  {
    switch (code) {
      case RC_END: return "RC_END";
      case RC_NULL: return "RC_NULL";
      case RC_STRING: return "RC_STRING";
      case RC_SET: return "RC_SET";
      case RC_NSET: return "RC_NSET";
      case RC_BEG_GROUP: return "RC_BEG_GROUP";
      case RC_END_GROUP: return "RC_END_GROUP";
      case RC_GROUP_REF: return "RC_GROUP_REF";
      case RC_LOOP: return "RC_LOOP";
      case RC_LOOP_INIT: return "RC_LOOP_INIT";
      case RC_LOOP_SHORT: return "RC_LOOP_SHORT";
      case RC_LOOP_UNIQUE: return "RC_LOOP_UNIQUE";
      case RC_LOOP_SHORT_UNIQUE: return "RC_LOOP_SHORT_UNIQUE";
      case RC_LOOP_LONG: return "RC_LOOP_LONG";
      case RC_OR: return "RC_OR";
      case RC_OR_UNIQUE: return "RC_OR_UNIQUE";
      case RC_POS_LOOKAHEAD: return "RC_POS_PEEK";
      case RC_NEG_LOOKAHEAD: return "RC_NEG_PEEK";
      case RC_WORD: return "RC_WORD";
      case RC_NWORD: return "RC_NWORD";
      case RC_BLINE: return "RC_BLINE";
      case RC_ELINE: return "RC_ELINE";
      case RC_BSTRING: return "RC_BSTRING";
      case RC_ESTRING: return "RC_ESTRING";
      case RC_ENSTRING: return "RC_ENSTRING";
      case RC_GSTRING: return "RC_GSTRING";
      case RC_COND: return "RC_COND";
      case RC_POS_LOOKBEHIND: return "RC_POS_LOOKBEHIND";
      case RC_NEG_LOOKBEHIND: return "RC_NEG_LOOKBEHIND";
      case RC_LOOKBEHIND_OR: return "RC_LOOKBEHIND_OR";
      case RC_STRING_I: return "RC_STRING_I";
      case RC_SET_I: return "RC_SET_I";
      case RC_NSET_I: return "RC_NSET_I";
      case RC_GROUP_REF_I: return "RC_GROUP_REF_I";
      case RC_LEXEME: return "RC_LEXEME";
      default: return "unknown(" + code + ")";
    }
  }

  static class Compat extends RegexpNode {
    int _code;
  
    CharBuffer _string;
    RegexpSet _set;
    int _index;
    int _min;
    int _max;
    RegexpNode _branch;
  
    //for conditionals
    RegexpNode _condition;
    RegexpNode _nBranch;

    // XXX: needs to be removed
    boolean _mark;
    boolean _printMark;

    byte _unicodeCategory;
  
    /**
     * Creates a node with a code
     */
    Compat(int code)
    {
      _rest = END;
      _code = code;
    
      _id = _count++;
    }

    /**
     * Creates a node with a group index
     */
    Compat(int code, int index)
    {
      this(code);

      _index = index;
    }

    /**
     * Creates a node with a group index
     */
    Compat(int code, RegexpNode branch)
    {
      this(code);

      _branch = branch;
    }

    /**
     * Creates a node with a group index
     */
    Compat(int code, int index, int min, int max)
    {
      this(code);

      _index = index;
      _min = min;
      _max = max;
    }

    /**
     * Creates a node with a group index
     */
    Compat(int code, RegexpSet set)
    {
      this(code);

      _set = set;
      _length = 1;
    }
    
    /**
     * Tries to match the program.
     *
     * @return index to the tail of the match
     */
    int match(CharCursor cursor, Regexp state)
    {
      int tail;
      char ch;
      int value;
    
      int i;
    
      GroupState oldState;

      switch (_code) {
      case RegexpNode.RC_NULL:
	return _rest.match(cursor, state);

      case RegexpNode.RC_LEXEME:
      case RegexpNode.RC_END:
	state._lexeme = _index;
	return _index;

      case RegexpNode.RC_STRING:
	if (true)
	  throw new UnsupportedOperationException();
	
	int length = _string.length();

	if (cursor.regionMatches(_string.getBuffer(), 0, length)) {
	  
	  return _rest.match(cursor, state);
	}
	else {
	  return FAIL;
	}
    
      case RegexpNode.RC_STRING_I:
	length = _string.length();

	if (cursor.regionMatchesIgnoreCase(_string.getBuffer(), 0, length))
	  return _rest.match(cursor, state);
	else
	  return FAIL;

      case RegexpNode.RC_SET:
	if ((ch = cursor.read()) != cursor.DONE && _set.match(ch))
	  return _rest.match(cursor, state);
	else
	  return FAIL;

      case RegexpNode.RC_SET_I:
	if ((ch = cursor.read()) == cursor.DONE)
	  return FAIL;
	int lch = Character.toLowerCase((char) ch);
	int uch = Character.toUpperCase((char) lch);
	if (_set.match(lch) || _set.match(uch))
	  return _rest.match(cursor, state);
	else
	  return FAIL;

      case RegexpNode.RC_NSET:
	if ((ch = cursor.read()) != cursor.DONE && ! _set.match(ch))
	  return _rest.match(cursor, state);
	else
	  return FAIL;

      case RegexpNode.RC_NSET_I:
	if ((ch = cursor.read()) == cursor.DONE)
	  return FAIL;
	
	lch = Character.toLowerCase((char) ch);
	uch = Character.toUpperCase((char) lch);
	if (! _set.match(lch) && ! _set.match(uch))
	  return _rest.match(cursor, state);
	else
	  return FAIL;

	// '('
      case RegexpNode.RC_BEG_GROUP:
	state._groupStart[_index] = cursor.getIndex();
	
	return _rest.match(cursor, state);

	// ')'
      case RegexpNode.RC_END_GROUP:
	int index = 2 * _index;
	
	if (state._groupState.size() <= index + 1)
	  state._groupState.setLength(index + 2);
	state._groupState.set(2 * _index, state._groupStart[_index]);
	state._groupState.set(2 * _index + 1, cursor.getIndex());
	
	state._groupState.setMatched(_index);
	
	return _rest.match(cursor, state);

	// '\nn'
      case RegexpNode.RC_GROUP_REF:
	if (! state._groupState.isMatched(_index))
	  return FAIL;
	else {
	  int begin = state._groupState.get(2 * _index);
	  length = (state._groupState.get(2 * _index + 1)
		    - state._groupState.get(2 * _index));
	  state._cb.setLength(0);
	  cursor.subseq(state._cb, begin, begin + length);
	  if (cursor.regionMatches(state._cb.getBuffer(), 0, length))
	    return _rest.match(cursor, state);
	  else
	    return FAIL;
	}

	// '\nn'
      case RegexpNode.RC_GROUP_REF_I:
	if (! state._groupState.isMatched(_index))
	  return FAIL;
	else {
	  int begin = state._groupState.get(2 * _index);
	  length = (state._groupState.get(2 * _index + 1)
		    - state._groupState.get(2 * _index));

	  state._cb.setLength(0);
	  cursor.subseq(state._cb, begin, begin + length);
	  if (cursor.regionMatchesIgnoreCase(state._cb.getBuffer(), 0, length)) {
	    cursor.skip(length);
	    return _rest.match(cursor, state);
	  } else
	    return FAIL;
	}

      case RegexpNode.RC_LOOP_INIT:
	state._loopCount[((Compat) _rest)._index] = 0;
	state._loopTail[((Compat) _rest)._index] = -1;
	
	return _rest.match(cursor, state);

	// '*' '{n,m}' '+' '?' matches as much as possible
      case RegexpNode.RC_LOOP:
	oldState = state._groupState.copy();
	tail = cursor.getIndex();

	int matchedCount = -1;
	int matchedTail = tail;
	GroupState matchedGroupState = null;
        
	int loopTail = -1;
        
	boolean isParentRestMatched = false;
        
	for (i = 0; i < _max; i++) {
	  if (cursor.current() == cursor.DONE)
	    break;
          
	  // empty string match break
	  if (loopTail == cursor.getIndex())
	    break;

	  loopTail = cursor.getIndex();
          
	  value = _branch.match(cursor, state);
          
	  if (value == FAIL)
	    break;
          
	  int lastPos = cursor.getIndex();
	  GroupState innerState = state._groupState.copy();
          
	  value = _rest.match(cursor, state);

          /*
	  if (value != FAIL && prog._min <= i + 1) {
	    if (_parentLoopRestStack.size() == 0) {
	      matchedCount = i + 1;
	      matchedTail = cursor.getIndex();
              
	      freeGroupState(matchedGroupState);
	      matchedGroupState = _groupState.copy();
	    }
	    else {
	      lastPos = cursor.getIndex();

	      freeGroupState(innerState);
	      innerState = _groupState.copy();
              
	      RegexpNode oldRest = _parentLoopRestStack.pop();
              
	      value = match(oldRest, cursor);
              
	      _parentLoopRestStack.push(oldRest);
              
	      if (value != FAIL || ! isParentRestMatched) {
		isParentRestMatched = isParentRestMatched || value != FAIL;
                
		matchedCount = i + 1;
		matchedTail = lastPos;
                
		freeGroupState(matchedGroupState);
		matchedGroupState = innerState.copy();
	      }
	    }
	  }
          
	  cursor.setIndex(lastPos);
	  setGroupState(innerState);
	  */
	  if (true) throw new UnsupportedOperationException(getClass().getName());
	}

	//System.err.println("outside LOOP: " + RegexpNode.code(prog));
        
	if (_min <= matchedCount) {
	  cursor.setIndex(matchedTail);

	  state.freeGroupState(oldState);
	  state.setGroupState(matchedGroupState);
          
	  return SUCCESS;
	}
	// may have matched the empty string
	else if (_min == 0) {
	  cursor.setIndex(tail);
	  state.setGroupState(oldState);

	  return _rest.match(cursor, state);
	}
	else {
	  cursor.setIndex(tail);
	  state.setGroupState(oldState);
          
	  return FAIL;
	}


	/*
	  tail = cursor.getIndex();
	  if (_loopCount[prog._index]++ < prog._min)
	  prog = prog._branch;
	  else if (_loopCount[prog._index] > prog._max)
	  prog = prog._rest;
	  else if (_loopTail[prog._index] == tail)
	  return FAIL;
	  else {
	  _loopTail[prog._index] = tail;
	  int match = _group.size();

	  if ((ch = cursor.current()) == cursor.DONE)
	  prog = prog._rest;
	  else if (prog._set != null && prog._set.match(ch))
	  prog = prog._branch;
	  else {
	  oldState = _groupState.copy();

	  if ((value = match(prog._branch, cursor)) != FAIL) {
	  return value;
	  }
	  else {
	  _groupState = oldState;
          
	  cursor.setIndex(tail);
	  _group.setLength(match);
	  prog = prog._rest;
	  }
	  }
	  }
	
	*/

    
	// '*' '{n,m}' '+' '?' possessively matches as much as possible
      case RegexpNode.RC_LOOP_LONG:
	oldState = state._groupState.copy();
	tail = cursor.getIndex();
        
	for (i = 0; i < _max; i++) {
	  if (cursor.current() == cursor.DONE)
	    break;

	  int lastPos = cursor.getIndex();
	  GroupState innerState = state._groupState.copy();
          
	  if ((value = _branch.match(cursor, state)) == FAIL) {
	    cursor.setIndex(lastPos);
	    state.setGroupState(innerState);
            
	    break;
	  }
	  else
	    state.freeGroupState(innerState);
	}

	if (_min <= i) {
	  state.freeGroupState(oldState);
	  return _rest.match(cursor, state);
	}
	else {
	  cursor.setIndex(tail);
	  state.setGroupState(oldState);
          
	  return FAIL;
	}
        
	/*
	  tail = cursor.getIndex();

	  if (_loopCount[prog._index] > prog._max)
	  prog = prog._rest;
	  else if (_loopTail[prog._index] == tail)
	  return FAIL;
	  else {
	  _loopTail[prog._index] = tail;
	  int match = _group.size();

	  oldState = _groupState.copy();
          
	  if (match(prog._branch, cursor) != FAIL) {
	  cursor.setIndex(tail);
	  }
	  else {
	  _groupState = oldState;
            
	  if ((ch = cursor.current()) == cursor.DONE)
	  prog = prog._rest;
	  else if (prog._set != null && prog._set.match(ch))
	  prog = prog._branch;
	  else {
	  cursor.setIndex(tail);
	  _group.setLength(match);
	  prog = prog._rest;
	  }
	  }
	  }
	*/

	// '*' '{n,m}' '+' '?' matches as little as possible
      case RegexpNode.RC_LOOP_SHORT:
	oldState = state._groupState.copy();
	tail = cursor.getIndex();

	if (_min == 0) {
	  if (_rest.match(cursor, state) != FAIL)
	    return SUCCESS;

	  state.setGroupState(oldState);
	  oldState = state._groupState.copy();
	  cursor.setIndex(tail);
	}
        
	for (i = 0; i < _max; i++) {
	  if (cursor.current() == cursor.DONE)
	    break;

	  value = _branch.match(cursor, state);
          
	  if (value == FAIL)
	    break;
          
	  int lastPos = cursor.getIndex();
	  GroupState innerState = state._groupState.copy();
          
	  value = _rest.match(cursor, state);

	  if (value != FAIL && _min <= i + 1) {
	    return SUCCESS;
	  }
          
	  cursor.setIndex(lastPos);
	  state.setGroupState(innerState);
	}

	// may have matched the empty string
	if (_min == 0) {
	  cursor.setIndex(tail);
	  state.setGroupState(oldState);
          
	  return _rest.match(cursor, state);
	}
	else {
	  cursor.setIndex(tail);
	  state.setGroupState(oldState);
          
	  return FAIL;
	}

	// The first mismatch for loop unique is necessarily a match
	// for the successor, e.g. a*b as opposed to a*ab
	// XXX: this needs to be changed to be like the or.
      case RegexpNode.RC_LOOP_UNIQUE:

	if (state._loopCount[_index]++ < _min) {
	  return _branch.match(cursor, state);
	}
	else if (_max < state._loopCount[_index])
	  return _rest.match(cursor, state);
	else if ((ch = cursor.current()) == cursor.DONE)
	  return _rest.match(cursor, state);
	else if (_set.match(ch))
	  return _branch.match(cursor, state);
	else
	  return _rest.match(cursor, state);

      case RegexpNode.RC_OR:
	state._match = state._groupState.size();
	tail = cursor.getIndex();
	if ((value = _branch.match(cursor, state)) != FAIL)
	  return value;
	cursor.setIndex(tail);
	state._groupState.setLength(state._match);
	return _rest.match(cursor, state);

	// Here we can tell by the first character if the match works
      case RegexpNode.RC_OR_UNIQUE:
	if ((ch = cursor.current()) == cursor.DONE)
	  return _rest.match(cursor, state);
	else if (_set.match(ch))
	  return _branch.match(cursor, state);
	else
	  return _rest.match(cursor, state);

	// The peek pattern must match but isn't included in the real match
      case RegexpNode.RC_POS_LOOKAHEAD:
	tail = cursor.getIndex();
	oldState = state._groupState.copy();
	
	if (_branch.match(cursor, state) == FAIL)
	  return FAIL;
	
	cursor.setIndex(tail);
	state.setGroupState(oldState);
	
	return _rest.match(cursor, state);

	// The peek pattern must not match and isn't included in the real match
      case RegexpNode.RC_NEG_LOOKAHEAD:
	tail = cursor.getIndex();
	oldState = state._groupState.copy();
	
	if (_branch.match(cursor, state) != FAIL)
	  return FAIL;
	
	state.setGroupState(oldState);
	cursor.setIndex(tail);
        
	return _rest.match(cursor, state);

	// The previous pattern must match and isn't included in the real match
      case RegexpNode.RC_POS_LOOKBEHIND:
	tail = cursor.getIndex();
	oldState = state._groupState.copy();
        
	length = _length;
        
	if (tail < length)
	  return FAIL;
        
	cursor.setIndex(tail - length);
        
	if (_branch.match(cursor, state) == FAIL) {
	  cursor.setIndex(tail);
	  state.setGroupState(oldState);

	  return FAIL;
	}

	cursor.setIndex(tail);
	state.setGroupState(oldState);

	return _rest.match(cursor, state);
        
	// The previous pattern must not match and isn't included in the real match
      case RegexpNode.RC_NEG_LOOKBEHIND:
	tail = cursor.getIndex();
	oldState = state._groupState.copy();
        
	length = _branch._length;
        
	if (length <= tail) {
	  cursor.setIndex(tail - length);
          
	  if (_branch.match(cursor, state) != FAIL) {
	    cursor.setIndex(tail);
	    state.setGroupState(oldState);
            
	    return FAIL;
	  }
	}
        
	cursor.setIndex(tail);
	state.setGroupState(oldState);

	return _rest.match(cursor, state);
        
      case RegexpNode.RC_LOOKBEHIND_OR:
	tail = cursor.getIndex();
	oldState = state._groupState.copy();
        
	int defaultLength = _length;
	boolean isMatched = false;
        
	RegexpNode node = _branch;

	if ((value = _branch.match(cursor, state)) != FAIL) {
	  return value;
	}
	else {
	  state.setGroupState(oldState);
          
	  for (node = _rest;
	       node != null && node != END;
	       node = node._rest) {
	    cursor.setIndex(tail);
	    oldState = state._groupState.copy();
            
	    cursor.setIndex(tail + defaultLength - node._length);
            
	    if (node.match(cursor, state) != FAIL) {
	      isMatched = true;
	      break;
	    }
            
	    cursor.setIndex(tail);
	    state.setGroupState(oldState);
	  }
	}
        
	if (! isMatched)
	  return FAIL;
        
	return _rest.match(cursor, state);

	// Conditional subpattern
      case RegexpNode.RC_COND:
	tail = cursor.getIndex();

	if (state._groupState.isMatched(_index)) {
	  if (_branch.match(cursor, state) == FAIL)
	    return FAIL;
	}
	else if (_nBranch != null) {
	  if (_nBranch.match(cursor, state) == FAIL)
	    return FAIL;
	}

	return _rest.match(cursor, state);

	// Beginning of line
      case RegexpNode.RC_BLINE:
	if (cursor.getIndex() == state._start)
	  return _rest.match(cursor, state);
	else if (cursor.previous() == '\n') {
	  cursor.next();
	  return _rest.match(cursor, state);
	}
	else {
	  cursor.next();
	  return FAIL;
	}

	// End of line
      case RegexpNode.RC_ELINE:
	if (cursor.current() == cursor.DONE || cursor.current() == '\n')
	  return _rest.match(cursor, state);	  // XXX: return on success?
	else
	  return FAIL;

	// Beginning of match
      case RegexpNode.RC_GSTRING:
	if (cursor.getIndex() == state._first)
	  return _rest.match(cursor, state);
	else
	  return FAIL;

	// beginning of string
      case RegexpNode.RC_BSTRING:
	if (cursor.getIndex() == state._start)
	  return _rest.match(cursor, state);
	else
	  return FAIL;

	// end of string
      case RegexpNode.RC_ESTRING:
	if (cursor.current() == cursor.DONE)
	  return _rest.match(cursor, state);      // XXX: return on success?
	else
	  return FAIL;
    
	// end of string or newline at end of string
      case RegexpNode.RC_ENSTRING:
	ch = cursor.current();
	tail = cursor.getIndex();
	if (ch == '\n' && tail == cursor.getEndIndex() - 1
	    || ch == cursor.DONE)
	  return _rest.match(cursor, state);	  // XXX: return on success?
	else
	  return FAIL;

      case RegexpNode.RC_WORD:
	tail = cursor.getIndex();
	if ((tail != state._start && RegexpSet.WORD.match(cursor.prev()))
	    != (cursor.current() != cursor.DONE
		&& RegexpSet.WORD.match(cursor.current())))
	  return _rest.match(cursor, state);
	else
	  return FAIL;

      case RegexpNode.RC_NWORD:
	tail = cursor.getIndex();
      
	if ((tail != state._start && RegexpSet.WORD.match(cursor.prev()))
	    == (cursor.current() != cursor.DONE
		&& RegexpSet.WORD.match(cursor.current())))
	  return _rest.match(cursor, state);
	else
	  return FAIL;
    
      case RegexpNode.RC_UNICODE:
	if ((ch = cursor.read()) == cursor.DONE)
	  return FAIL;
        
	if (Character.getType(ch) == _unicodeCategory)
	  return _rest.match(cursor, state);
	else
	  return FAIL;
        
      case RegexpNode.RC_NUNICODE:
	if ((ch = cursor.read()) == cursor.DONE)
	  return FAIL;
        
	if (Character.getType(ch) != _unicodeCategory)
	  return _rest.match(cursor, state);
	else
	  return FAIL;
        
      case RegexpNode.RC_C:
	if ((ch = cursor.read()) == cursor.DONE)
	  return FAIL;
        
	value = Character.getType(ch);
        
	if (value == Character.CONTROL
	    || value == Character.FORMAT
	    || value == Character.UNASSIGNED
	    || value == Character.PRIVATE_USE
	    || value == Character.SURROGATE)
	  return _rest.match(cursor, state);
	else
	  return FAIL;
        
      case RegexpNode.RC_NC:
	if ((ch = cursor.read()) == cursor.DONE)
	  return FAIL;

	value = Character.getType(ch);
        
	if (value != Character.CONTROL
	    && value != Character.FORMAT
	    && value != Character.UNASSIGNED
	    && value != Character.PRIVATE_USE
	    && value != Character.SURROGATE)
	  return _rest.match(cursor, state);
	else
	  return FAIL;

      case RegexpNode.RC_L:
	if ((ch = cursor.read()) == cursor.DONE)
	  return FAIL;
        
	value = Character.getType(ch);
        
	if (value == Character.LOWERCASE_LETTER
	    || value == Character.MODIFIER_LETTER
	    || value == Character.OTHER_LETTER
	    || value == Character.TITLECASE_LETTER
	    || value == Character.UPPERCASE_LETTER)
	  return _rest.match(cursor, state);
	else
	  return FAIL;
        
      case RegexpNode.RC_NL:
	if ((ch = cursor.read()) == cursor.DONE)
	  return FAIL;
        
	value = Character.getType(ch);
        
	if (value != Character.LOWERCASE_LETTER
	    && value != Character.MODIFIER_LETTER
	    && value != Character.OTHER_LETTER
	    && value != Character.TITLECASE_LETTER
	    && value != Character.UPPERCASE_LETTER)
	  return _rest.match(cursor, state);
	else
	  return FAIL;
        
      case RegexpNode.RC_M:
	if ((ch = cursor.read()) == cursor.DONE)
	  return FAIL;
        
	value = Character.getType(ch);
        
	if (value == Character.COMBINING_SPACING_MARK
	    || value == Character.ENCLOSING_MARK
	    || value == Character.NON_SPACING_MARK)
	  return _rest.match(cursor, state);
	else
	  return FAIL;
        
      case RegexpNode.RC_NM:
	if ((ch = cursor.read()) == cursor.DONE)
	  return FAIL;
        
	value = Character.getType(ch);
        
	if (value != Character.COMBINING_SPACING_MARK
	    && value != Character.ENCLOSING_MARK
	    && value != Character.NON_SPACING_MARK)
	  return _rest.match(cursor, state);
	else
	  return FAIL;
        
      case RegexpNode.RC_N:
	if ((ch = cursor.read()) == cursor.DONE)
	  return FAIL;
        
	value = Character.getType(ch);
        
	if (value == Character.DECIMAL_DIGIT_NUMBER
	    || value == Character.LETTER_NUMBER
	    || value == Character.OTHER_NUMBER)
	  return _rest.match(cursor, state);
	else
	  return FAIL;
        
      case RegexpNode.RC_NN:
	if ((ch = cursor.read()) == cursor.DONE)
	  return FAIL;
        
	value = Character.getType(ch);
        
	if (value != Character.DECIMAL_DIGIT_NUMBER
	    && value != Character.LETTER_NUMBER
	    && value != Character.OTHER_NUMBER)
	  return _rest.match(cursor, state);
	else
	  return FAIL;
        
      case RegexpNode.RC_P:
	if ((ch = cursor.read()) == cursor.DONE)
	  return FAIL;
        
	value = Character.getType(ch);
        
	if (value == Character.CONNECTOR_PUNCTUATION
	    || value == Character.DASH_PUNCTUATION
	    || value == Character.END_PUNCTUATION
	    || value == Character.FINAL_QUOTE_PUNCTUATION
	    || value == Character.INITIAL_QUOTE_PUNCTUATION
	    || value == Character.OTHER_PUNCTUATION
	    || value == Character.START_PUNCTUATION)
	  return _rest.match(cursor, state);
	else
	  return FAIL;
        
      case RegexpNode.RC_NP:
	if ((ch = cursor.read()) == cursor.DONE)
	  return FAIL;
        
	value = Character.getType(ch);
        
	if (value != Character.CONNECTOR_PUNCTUATION
	    && value != Character.DASH_PUNCTUATION
	    && value != Character.END_PUNCTUATION
	    && value != Character.FINAL_QUOTE_PUNCTUATION
	    && value != Character.INITIAL_QUOTE_PUNCTUATION
	    && value != Character.OTHER_PUNCTUATION
	    && value != Character.START_PUNCTUATION)
	  return _rest.match(cursor, state);
	else
	  return FAIL;
        
      case RegexpNode.RC_S:
	if ((ch = cursor.read()) == cursor.DONE)
	  return FAIL;
        
	value = Character.getType(ch);
        
	if (value == Character.CURRENCY_SYMBOL
	    || value == Character.MODIFIER_SYMBOL
	    || value == Character.MATH_SYMBOL
	    || value == Character.OTHER_SYMBOL)
	  return _rest.match(cursor, state);
	else
	  return FAIL;
        
      case RegexpNode.RC_NS:
	if ((ch = cursor.read()) == cursor.DONE)
	  return FAIL;
        
	value = Character.getType(ch);
        
	if (value != Character.CURRENCY_SYMBOL
	    && value != Character.MODIFIER_SYMBOL
	    && value != Character.MATH_SYMBOL
	    && value != Character.OTHER_SYMBOL)
	  return _rest.match(cursor, state);
	else
	  return FAIL;
        
      case RegexpNode.RC_Z:
	if ((ch = cursor.read()) == cursor.DONE)
	  return FAIL;
        
	value = Character.getType(ch);
        
	if (value == Character.LINE_SEPARATOR
	    || value == Character.PARAGRAPH_SEPARATOR
	    || value == Character.SPACE_SEPARATOR)
	  return _rest.match(cursor, state);
	else
	  return FAIL;
        
      case RegexpNode.RC_NZ:
	if ((ch = cursor.read()) == cursor.DONE)
	  return FAIL;
        
	value = Character.getType(ch);
        
	if (value != Character.LINE_SEPARATOR
	    && value != Character.PARAGRAPH_SEPARATOR
	    && value != Character.SPACE_SEPARATOR)
	  return _rest.match(cursor, state);
	else
	  return FAIL;
        
      case RegexpNode.RC_CHAR_CLASS:
	switch (((Compat) _branch)._code) {
	case RegexpNode.RC_SPACE:
	  if ((ch = cursor.read()) == cursor.DONE)
	    return FAIL;
            
	  //value 
            
	  break;
          
	}
	return SUCCESS;
        
      default:
	throw new RuntimeException("Internal error: " + RegexpNode.code(this));
      }
    }

    public Object clone()
    {
      Compat node = new Compat(_code);
      node._rest = _rest;
      node._string = _string;
      node._set = _set;
      node._index = _index;
      node._min = _min;
      node._max = _max;
      node._branch = _branch;
    
      node._length = _length;
      node._unicodeCategory = _unicodeCategory;

      return node;
    }

    public String toString()
    {
      if (_printMark)
	return "...";
    
      _printMark = true;
      try {
	switch (_code) {
	case RC_END:
	  return "";
      
	case RC_STRING:
	  return _string.toString() + (_rest == null ? "" : _rest.toString());
      
	case RC_OR:
	  return "(?:" + _branch + "|" + _rest + ")";
      
	case RC_OR_UNIQUE:
	  return "(?:" + _branch + "|!" + _rest + ")";
      
	case RC_ESTRING:
	  return "\\Z" + (_rest == null ? "" : _rest.toString());
      
	case RC_LOOP_INIT:
	  return _rest.toString();
      
	case RC_LOOP:
	  return ("(?:" + _branch + "){" + _min + "," + _max + "}" +
		  (_rest == null ? "" : _rest.toString()));
      
	case RC_LOOP_UNIQUE:
	  return ("(?:" + _branch + ")!{" + _min + "," + _max + "}" +
		  (_rest == null ? "" : _rest.toString()));
      
	case RC_BEG_GROUP:
	  return "(" + (_rest == null ? "" : _rest.toString());
      
	case RC_END_GROUP:
	  return ")" + (_rest == null ? "" : _rest.toString());
      
	case RC_SET:
	  return "[" + _set + "]" + (_rest == null ? "" : _rest.toString());
      
	case RC_NSET:
	  return "[^" + _set + "]" + (_rest == null ? "" : _rest.toString());
      
	default:
	  return "" + _code + " " + super.toString();
	}
      } finally {
	_printMark = false;
      }
    }
  }

  /**
   * A node with exactly one character matches.
   */
  static class AbstractCharNode extends RegexpNode {
    @Override
    RegexpNode createLoop(int min, int max)
    {
      return new CharLoop(this, min, max);
    }

    @Override
    int minLength()
    {
      return 1;
    }
  }
    
  static class CharNode extends AbstractCharNode {
    private char _ch;

    CharNode(char ch)
    {
      _ch = ch;
    }

    @Override
    int match(StringValue string, int offset, RegexpState state)
    {
      if (offset < string.length() && string.charAt(offset) == _ch)
	return offset + 1;
      else
	return -1;
    }
  }
    
  static class AsciiSet extends AbstractCharNode {
    private final boolean []_set;

    AsciiSet()
    {
      _set = new boolean[128];
    }

    AsciiSet(boolean []set)
    {
      _set = set;
    }

    void setChar(char ch)
    {
      _set[ch] = true;
    }

    void clearChar(char ch)
    {
      _set[ch] = false;
    }

    @Override
    int match(StringValue string, int offset, RegexpState state)
    {
      if (string.length() <= offset)
	return -1;

      char ch = string.charAt(offset);

      if (ch < 128 && _set[ch])
	return offset + 1;
      else
	return -1;
    }
  }
    
  static class AsciiNotSet extends AbstractCharNode {
    private final boolean []_set;

    AsciiNotSet()
    {
      _set = new boolean[128];
    }

    AsciiNotSet(boolean []set)
    {
      _set = set;
    }

    void setChar(char ch)
    {
      _set[ch] = true;
    }

    void clearChar(char ch)
    {
      _set[ch] = false;
    }

    @Override
    int match(StringValue string, int offset, RegexpState state)
    {
      if (string.length() <= offset)
	return -1;

      char ch = string.charAt(offset);

      if (ch < 128 && _set[ch])
	return -1;
      else
	return offset + 1;
    }
  }
  
  static class CharLoop extends RegexpNode {
    private final RegexpNode _node;
    private RegexpNode _next = N_END;

    private int _min;
    private int _max;

    CharLoop(RegexpNode node, int min, int max)
    {
      _node = node;
      _min = min;
      _max = max;
    }

    @Override
    RegexpNode concat(RegexpNode next)
    {
      if (_next != null)
	_next = _next.concat(next);
      else
	_next = next;

      return this;
    }

    @Override
    RegexpNode createLoop(int min, int max)
    {
      if (min == 0 && max == 1) {
	_min = 0;
      
	return this;
      }
      else
	return new Loop(this, min, max);
    }

    //
    // match functions
    //

    @Override
    int match(StringValue string, int offset, RegexpState state)
    {
      RegexpNode next = _next;
      RegexpNode node = _node;
      int min = _min;
      int max = _max;

      int i;
      
      int headOffset = offset;
      
      for (i = 0; i < min; i++) {
	if (node.match(string, offset + i, state) < 0)
	  return -1;
      }

      for (; i < max; i++) {
	if (node.match(string, offset + i, state) < 0) {
	  break;
	}
      }

      for (; min <= i; i--) {
	int tail = next.match(string, offset + i, state);

	if (tail >= 0)
	  return tail;
      }

      return -1;
    }
  }
  
  static class Concat extends RegexpNode {
    private final RegexpNode _head;
    private RegexpNode _next;

    Concat(RegexpNode prev, RegexpNode next)
    {
      _head = prev;
      _next = next;
    }

    @Override
    RegexpNode concat(RegexpNode next)
    {
      _next = _next.concat(next);

      return this;
    }

    //
    // optim functions
    //

    @Override
    int minLength()
    {
      return _head.minLength() + _next.minLength();
    }

    @Override
    String prefix()
    {
      return _head.prefix();
    }

    @Override
    int match(StringValue string, int offset, RegexpState state)
    {
      offset = _head.match(string, offset, state);

      if (offset < 0)
	return -1;
      else
	return _next.match(string, offset, state);
    }
  }
  
  static class End extends RegexpNode {
    @Override
    RegexpNode concat(RegexpNode next)
    {
      return next;
    }
    
    @Override
    int match(StringValue string, int offset, RegexpState state)
    {
      return offset;
    }
  }
  
  static class Group extends RegexpNode {
    private final RegexpNode _node;
    private final int _group;

    Group(RegexpNode node, int group)
    {
      _node = node;
      _group = group;
    }
    
    @Override
    int match(StringValue string, int offset, RegexpState state)
    {
      int tail = _node.match(string, offset, state);

      if (tail < 0)
	return -1;

      state.setBegin(_group, offset);
      state.setEnd(_group, tail);

      return tail;
    }
  }
  
  static class GroupRef extends RegexpNode {
    private final int _group;

    GroupRef(int group)
    {
      _group = group;
    }
    
    @Override
    int match(StringValue string, int offset, RegexpState state)
    {
      int begin = state.getBegin(_group);
      int length = state.getEnd(_group) - begin;

      if (string.regionMatches(offset, string, begin, length))
	return offset + length;
      else
	return -1;
    }
  }
  
  static class Loop extends RegexpNode {
    private final RegexpNode _node;
    private RegexpNode _next = N_END;

    private int _min;
    private int _max;

    Loop(RegexpNode node, int min, int max)
    {
      _node = node;
      _min = min;
      _max = max;
    }

    @Override
    RegexpNode concat(RegexpNode next)
    {
      if (_next != null)
	_next = _next.concat(next);
      else
	_next = next;

      return this;
    }

    @Override
    RegexpNode createLoop(int min, int max)
    {
      if (min == 0 && max == 1) {
	_min = 0;
      
	return this;
      }
      else
	return new Loop(this, min, max);
    }

    //
    // match functions
    //

    @Override
    int match(StringValue string, int offset, RegexpState state)
    {
      RegexpNode next = _next;
      RegexpNode node = _node;
      int min = _min;
      int max = _max;
      
      for (int i = 0; i < min; i++) {
	offset = node.match(string, offset, state);

	if (offset < 0)
	  return -1;
      }

      for (int i = min; i <= max; i++) {
	int tail = next.match(string, offset, state);

	if (tail >= 0)
	  return tail;

	if (i + 1 < max) {
	  int nextOffset = node.match(string, offset, state);

	  if (nextOffset < 0 || nextOffset == offset)
	    return -1;

	  offset = nextOffset;
	}
      }

      return -1;
    }
  }
  
  static class Or extends RegexpNode {
    private final RegexpNode _left;
    private final RegexpNode _right;

    Or(RegexpNode left, RegexpNode right)
    {
      _left = left;
      _right = right;
    }

    @Override
    int minLength()
    {
      return Math.min(_left.minLength(), _right.minLength());
    }

    @Override
    int match(StringValue string, int offset, RegexpState state)
    {
      int value = _left.match(string, offset, state);

      if (value >= 0)
	return value;
      else
	return _right.match(string, offset, state);
    }
  }
  
  static class StringNode extends RegexpNode {
    private final char []_buffer;
    private final int _length;

    StringNode(CharBuffer value)
    {
      _length = value.length();
      _buffer = new char[_length];

      if (_length == 0)
	throw new IllegalStateException("empty string");
      
      System.arraycopy(value.getBuffer(), 0, _buffer, 0, _buffer.length);
    }

    StringNode(char []buffer, int length)
    {
      _length = length;
      _buffer = buffer;

      if (_length == 0)
	throw new IllegalStateException("empty string");
    }

    RegexpNode createLoop(int min, int max)
    {
      if (_length == 1)
	return new CharLoop(this, min, max);
      else {
	char ch = _buffer[_length - 1];
	
	RegexpNode head = new StringNode(_buffer, _length - 1);

	return head.concat(new CharNode(ch).createLoop(min, max));
      }
    }

    //
    // optim functions
    //

    @Override
    int minLength()
    {
      return _length;
    }

    @Override
    String prefix()
    {
      return new String(_buffer, 0, _length);
    }

    //
    // match function
    //

    @Override
    int match(StringValue string, int offset, RegexpState state)
    {
      if (string.regionMatches(offset, _buffer, 0, _length))
	return offset + _length;
      else
	return -1;
    }
  }

  static {
    AsciiNotSet dot = new AsciiNotSet();
    dot.setChar('\n');
    DOT = dot;
    
    ANY_CHAR = new AsciiNotSet();
  }
}
