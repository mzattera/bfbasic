//====================================================================
// BFBASIC -- Basic programming language compiler for BF
// Filename : AlgebraicExpression.java
// Language : Java 1.2+
// Version  : 1.40
// Copyright: (C) 2001-2005 Jeffry Johnston
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as
// published by the Free Software Foundation. See the file LICENSE
// for more details.
//====================================================================

import java.util.Arrays;
import java.util.EmptyStackException;
import java.util.Stack;

//********************************************************************
//AlgebraicExpression
//********************************************************************
public class AlgebraicExpression {
  private String _ae;
  private String _token;
  private int _tokentype;
  private int _tempvar = 0;
  private String _parse = null;
  private static final int TOKEN_NULL = 0;
  private static final int TOKEN_LEVEL0 = 1; // and or
  private static final int TOKEN_LEVEL1 = 2; // = <> < <= > >=
  private static final int TOKEN_LEVEL2 = 3; // + -
  private static final int TOKEN_LEVEL3 = 4; // * /
  //private static final int TOKEN_LEVEL4 = 5; // -
  private static final int TOKEN_NUMBER = 10;
  private static final int TOKEN_VARIABLE = 11;
  private static final int TOKEN_STRING = 12;
  private static final int TOKEN_FUNCTION = 13;
  private static final int TOKEN_LPAREN = 14;
  private static final int TOKEN_RPAREN = 15;
  
  
  //------------------------------------------------------------------
  // (constructor)
  //------------------------------------------------------------------
  public AlgebraicExpression(String ae) {
    _ae = ae;
  }

  //------------------------------------------------------------------
  // addTo -- Converts a number to BF format
  // On entry:
  //   n                number (positive for +'s, negative for -'s)
  // Returns:
  //   String           BF code
  //------------------------------------------------------------------
  public String addTo(int n) {
    if (n == 0) { 
      return ""; 
    } 
    char ch;
    if (n < 0) {
      ch = '-';
      n = -n;
    } else {
      ch = '+';
    }
    char[] c = new char[n];
    Arrays.fill(c, ch);
    return new String(c);
  }
  
  //------------------------------------------------------------------
  // getToken
  //------------------------------------------------------------------
  public String getToken() {
    _token = "";
    _tokentype = TOKEN_NULL;
    _ae = _ae.trim() + "   ";
    char p = _ae.charAt(0);
    int i;
    if (_ae.equals("   ")) { // empty
      _ae = "";
    } else if ((_ae.substring(0, 2)).equalsIgnoreCase("&h")) { // hex number
      _tokentype = TOKEN_NUMBER;
      _token = "0x";
      for (i = 2; i < _ae.length(); i++) {
        p = _ae.charAt(i);
        if ((p >= '0' && p <= '9') || (p >= 'a' && p <= 'f') || (p >= 'A' && p <= 'F')) {
          _token += p + "";
        } else break;
      }
      try {
        _token = (Integer.decode(_token)).toString();
      } catch (NumberFormatException e) { }
      _ae = _ae.substring(i);
    } else if (p >= '0' && p <= '9') { // decimal number
      _tokentype = TOKEN_NUMBER;
      _token = p + "";
      for (i = 1; i < _ae.length(); i++) {
        p = _ae.charAt(i);
        if (p >= '0' && p <= '9') {
          _token += p + "";
        } else break;
      }
      _ae = _ae.substring(i);
    } else if (p == '\"') { // string literal
      _tokentype = TOKEN_STRING;
      i = _ae.indexOf('\"');
      if (i == -1) {
        bfbasic.errout("Unclosed string literal");
      } else {
        _token = _ae.substring(1, i);
      }
      _ae = _ae.substring(i + 1);
    } else if (p == '(') { // left parenthesis
      _tokentype = TOKEN_LPAREN;
      _ae = _ae.substring(1);
    } else if (p == ')') { // right parenthesis
      _tokentype = TOKEN_RPAREN;
      _ae = _ae.substring(1);
    } else if (p == '\'') { // comment
      _ae = "";
    } else if ((_ae.substring(0, 3)).equalsIgnoreCase("and")) { // operator level 0: logic
      _token = " " + _ae.substring(0, 3).toUpperCase() + " ";
      _tokentype = TOKEN_LEVEL0;
      _ae = _ae.substring(3);
    } else if ((_ae.substring(0, 2)).equalsIgnoreCase("or")) { // operator level 0: logic
      _token = " " + _ae.substring(0, 2).toUpperCase() + " ";
      _tokentype = TOKEN_LEVEL0;
      _ae = _ae.substring(2);
    } else if ("<=>".indexOf(p) != -1) { // operator level 1: compare
      _tokentype = TOKEN_LEVEL1;
      if ((p == '<' || p == '>') && _ae.charAt(1) == '=') { // combine operators
        _token = p + "=";
        _ae = _ae.substring(2);
      } else if (p == '<' && _ae.charAt(1) == '>') {
        _token = p + ">";
        _ae = _ae.substring(2);
      } else {
        _token = p + "";
        _ae = _ae.substring(1);
      }
    } else if ("+-".indexOf(p) != -1) { // operator level 2: add, subtract
      _tokentype = TOKEN_LEVEL2;
      _token = p + "";
      _ae = _ae.substring(1);
    } else if ("*/".indexOf(p) != -1) { // operator level 3: multiply, divide
      _tokentype = TOKEN_LEVEL3;
      _token = p + "";
      _ae = _ae.substring(1);
      /*
       } else if (p == '!') { // operator level 4: negate
       tokentype = TOKEN_LEVEL4;
       token = p + "";
       ae = ae.substring(1);
       */
    } else if ((p >= 'a' && p <= 'z') || (p >= 'A' && p <= 'Z') || (p == '_')) { // variable
      _tokentype = TOKEN_VARIABLE;
      _token = p + "";
      for (i = 1; i < _ae.length(); i++) {
        p = _ae.charAt(i);
        if (p == '(') { // function or array
          _tokentype = TOKEN_FUNCTION;
          i++;
          break;
        } else if ((p >= 'a' && p <= 'z') || (p >= 'A' && p <= 'Z') || (p >= '0' && p <= '9')) {
          _token += p + "";
        } else break;
      }
      _token = _token.toUpperCase();
      _ae = _ae.substring(i);
    } else { // unknown
      bfbasic.errout("Invalid token \'" + p + "\'");
    }
    return _token;
  }
  
  //------------------------------------------------------------------
  // parse
  //------------------------------------------------------------------
  public String parse() {
    if (_parse != null) {
      return _parse;
    }  
    String parsed = "";
    String operator;
    Stack tokens = new Stack();
    Stack tokentypes = new Stack();
    Stack operands = new Stack();
    Stack operandTypes = new Stack();
    Stack functions = new Stack();
    int operand1 = 0, operand2 = 0;
    _tempvar = 0;
    getToken();
    try {
      while (_tokentype != TOKEN_NULL) {
        if (_tokentype == TOKEN_LPAREN) {
          tokens.push(_token);
          tokentypes.push(new Integer(_tokentype));
          functions.push(_token);
        } else if (_tokentype == TOKEN_FUNCTION) {
          tokens.push(_token);
          tokentypes.push(new Integer(TOKEN_LPAREN));
          functions.push(_token);
        } else if (_tokentype == TOKEN_RPAREN) {
          while (((Integer) tokentypes.peek()).intValue() != TOKEN_LPAREN) {
            operator = (String) tokens.pop();
            operand2 = ((Integer) operands.pop()).intValue();
            int simple = ((Integer) operandTypes.pop()).intValue();
            operand1 = ((Integer) operands.peek()).intValue();
            int operand1Type = ((Integer) operandTypes.peek()).intValue();
            if (operand1Type >= 0) {
              if (bfbasic._debug > 0) {
                parsed += "\n(_T" + operand1 + "=" + operand1Type + ")\n";
              }
              parsed += "@_T" + operand1 + "[-]" + addTo(operand1Type);
              operandTypes.pop();
              operandTypes.push(new Integer(-1));
            }
            /*
             if (((Integer) tokentypes.pop()).intValue() == TOKEN_LEVEL4) {
             operand1 = -1;
             } else {
             operand1 = ((Integer) operands.peek()).intValue();
             }
             */
            String code = getCode("_T" + operand1, operator, "_T" + operand2, simple);
            if (code == null) {
              if (bfbasic._debug > 0) {
                parsed += "\n(_T" + operand2 + "=" + simple + ")\n";
              }
              parsed += "@_T" + operand2 + "[-]" + addTo(simple);
              code = getCode("_T" + operand1, operator, "_T" + operand2, -1);
            }
            parsed += code;
            _tempvar = operand2;
            tokentypes.pop();
          }
          if (!((String) functions.peek()).equals("")) {
            String funct = ((String) functions.peek()).toUpperCase();
            if (funct.equals("NOT")) {
              //V1=NOT(V1)
              //  @T0 ([-]) -           T0=255
              //  @V1[@T0-@V1-]         T0=T0-V1:V1=0
              //  @T0[@V1+@T0-]         V1=T0:T0=0
              if (bfbasic._debug > 0) {
                parsed += "\n(_T" + (_tempvar - 1) + "=NOT(_T" + (_tempvar - 1) + "))\n";
              }
              parsed += "@_0-@_T" + (_tempvar - 1) + "[@_0-@_T" + (_tempvar - 1)
              + "-]@_0[@_T" + (_tempvar - 1) + "+@_0-]";
            } else {
              //V1=V2(V1)
              //  @V1[@V2>>+<<@V1-] set up A (offset) = V1, clear V1
              //  @V2>>[[>>]+[<<]>>-]+[>>]<[<[<<]>+<
              //  @V1+
              //  @V2>>[>>]<-]<[<<]>[>[>>]<+<[<<]>-]>[>>]<<[-<<]
              
              if (bfbasic._debug > 0) {
                parsed += "\n(_T" + (_tempvar - 1) + "=" + funct + "(_T"
                + (_tempvar - 1) + "))\n";
              }
              
              //  @V1 = @_T" + (_tempvar - 1)
              //  @V2 = "@~" + funct 
              parsed += "@_T" + (_tempvar - 1) + "[@~" + funct
              + ">>+<<@_T" + (_tempvar - 1) + "-]@~" + funct
              + ">>[[>>]+[<<]>>-]+[>>]<[<[<<]>+<"
              + "@_T" + (_tempvar - 1) + "+@~" + funct 
              + ">>[>>]<-]<[<<]>[>[>>]<+<[<<]>-]>[>>]<<[-<<]";

            }
          }
          functions.pop();
          tokens.pop();
          tokentypes.pop();
        } else if (_tokentype == TOKEN_NUMBER) {
          //V1=#
          //  (...)               V1=#:T0=0
          // doesn't actually add a number assignment (for 
          // optimization).  It will be added later when needed.
          int val = 0;
          try {
            val = Integer.parseInt(_token);
          } catch (NumberFormatException e) {
            bfbasic.errout("Invalid number or label '" + _token + "'");
          }
          operands.push(new Integer(_tempvar));  
          operandTypes.push(new Integer(val)); // means simple
          _tempvar++;
        } else if (_tokentype == TOKEN_VARIABLE) {
          if (bfbasic._debug > 0) {
            parsed += "\n(_T" + _tempvar + "=" + _token + ")\n";
          }
          if (_token.equalsIgnoreCase("INKEY")) {
            //V1=INKEY
            //  @V1,                V1=ASC(INPUT$(1,1))
            parsed += "@_T" + _tempvar + ",";
          } else if (_token.equalsIgnoreCase("RND")) {
            //V1=RND
            //Uses linear congruential generator, V = (A * V + B) % M
            //Where: A = 31821, B = 13849, M = 65536, V = 0 (initial seed)
            //A and B values were obtained from the book:
            //  Texas Instruments TMS320 DSP DESIGNER'S NOTEBOOK Number 43
            //  "Random Number Generation on a TMS320C5x"
            //  Book author: Eric Wilbur
            //BF code by Jeffry Johnston
            //Random numbers will repeat after 65536 calls to this function
            
            //  @RH[@T0+@RH-]                   T0=RH:RH=0
            //  @RL[@T1+@RL-]                   T1=RL:RL=0
            //  @T3+++++++[@T2+++++++++++@T3-]  T2=77:T3=0
            
            //  @T2[                            DO WHILE T2<>0
            //    @T0[@RH+@T3+@T0-]               RH=RH+T0:T3=T0:T0=0
            //    @T3[@T0+@T3-]                   T0=T3:T3=0
            //    @T1[@RH+@T3+@T4+@T1-]           RH=RH+T1:T3=T1:T4=T1:T1=0
            
            //    @T4[@T1+@T4-]                   T1=T4:T4=0
            //    @T3[                            DO WHILE T3<>0
            //      @RL+[@T4+@T5+@RL-]              RL=RL+1:T4=RL:T5=RL:RL=0
            //      @T5[@RL+@T5-]+                  RL=T5:T5=1
            
            //      @T4[                            IF T4<>0 THEN
            //        @T5-                            T5=0
            //      @T4[-]]                         END IF:T4=0
            //      @T5[                            IF T5<>0 THEN
            //        @RH+                            RH=RH+1
            //      @T5-]                           END IF:T5=0
            //    @T3-]                           T3=T3-1:LOOP:T3=0
            //  @T2-]                           T2=T2-1:LOOP:T2=0
            
            //  ++++++[@T3++++++++@T2-]@T3-     T3=47:T2=0
            //  [                               DO WHILE T3<>0
            //    @T1[@RH+@T2+@T1-]               RH=RH+T1:T2=T1:T1=0
            //    @T2[@T1+@T2-]                   T1=T2:T2=0
            
            //  @T3-]                           T3=T3-1:LOOP:T3=0
            //  @T0[-]@T1[-]+++++[@T0+++++@T1-] T0=25:T1=0 (R=R+13849)
            //  @T0[                            DO WHILE T0<>0
            //    @RL+[@T1+@T2+@RL-]              RL=RL+1:T1=RL:T2=RL:RL=0
            
            //    @T2[@RL+@T2-]+                  RL=T2:T2=1
            //    @T1[                            IF T1<>0 THEN
            //      @T2-                            T2=0
            //    @T1[-]]                         END IF:T1=0
            //    @T2[                            IF T2<>0 THEN
            //      @RH+                            RH=RH+1
            //    @T2-]                           END IF:T2=0
            //  @T0-]                           T0=T0-1:LOOP:T0=0
            
            //  ++++++[@RH+++++++++@T0-]        RH=RH+54:T0=0
            
            //  @RH[@V1+@T0+@RH-]               V1=RH:T0=RH:RH=0
            //  @T0[@RH+@T0-]                   RH=T0:T0=0
            parsed += "@_RH[@_0+@_RH-]@_RL[@_1+@_RL-]@_3+++++++[@_2+++++++++++@_3-]"
              + "@_2[@_0[@_RH+@_3+@_0-]@_3[@_0+@_3-]@_1[@_RH+@_3+@_4+@_1-]"
              + "@_4[@_1+@_4-]@_3[@_RL+[@_4+@_5+@_RL-]@_5[@_RL+@_5-]+"
              + "@_4[@_5-@_4[-]]@_5[@_RH+@_5-]@_3-]@_2-]"
              + "++++++[@_3++++++++@_2-]@_3-[@_1[@_RH+@_2+@_1-]@_2[@_1+@_2-]"
              + "@_3-]@_0[-]@_1[-]+++++[@_0+++++@_1-]@_0[@_RL+[@_1+@_2+@_RL-]"
              + "@_2[@_RL+@_2-]+@_1[@_2-@_1[-]]@_2[@_RH+@_2-]@_0-]"
              + "++++++[@_RH+++++++++@_0-]"
              + "@_RH[@_T" + _tempvar + "+@_0+@_RH-]@_0[@_RH+@_0-]";
          } else {
            //V1=V2
            //  (@T0[-])            (T0=0)
            //  @V1[-]              V1=0
            //  @V2[@V1+@T0+@V2-]   V1=V2:T0=V2:V2=0
            //  @T0[@V2+@T0-]       V2=T0:T0=0
            parsed += "@_T" + _tempvar + "[-]@" + _token + "[@_T" + _tempvar
            + "+@_0+@" + _token + "-]@_0[@" + _token + "+@_0-]";
          }
          operands.push(new Integer(_tempvar));
          operandTypes.push(new Integer(-1));
          _tempvar++;
        } else {
          while (!tokens.empty()) {
            if (((Integer) tokentypes.peek()).intValue() == TOKEN_LPAREN) {
              break;
            }
            if (_tokentype > ((Integer) tokentypes.peek()).intValue()) {
              break;
            }
            operator = (String) tokens.pop();
            operand2 = ((Integer) operands.pop()).intValue();
            int simple = ((Integer) operandTypes.pop()).intValue();
            _tempvar = operand2;
            
            operand1 = ((Integer) operands.peek()).intValue();
            int operand1Type = ((Integer) operandTypes.peek()).intValue();
            if (operand1Type >= 0) {
              if (bfbasic._debug > 0) {
                parsed += "\n(_T" + operand1 + "=" + operand1Type + ")\n";
              }
              parsed += "@_T" + operand1 + "[-]" + addTo(operand1Type);
              operandTypes.pop();
              operandTypes.push(new Integer(-1));
            }
            /*
             if (((Integer) tokentypes.pop()).intValue() == TOKEN_LEVEL4) {
             operand1 = -1;
             } else {
             operand1 = ((Integer) operands.peek()).intValue();
             }
             */
            String code = getCode("_T" + operand1, operator, "_T" + operand2, simple);
            if (code == null) {
              if (bfbasic._debug > 0) {
                parsed += "\n(_T" + operand2 + "=" + simple + ")\n";
              }
              parsed += "@_T" + operand2 + "[-]" + addTo(simple);
              code = getCode("_T" + operand1, operator, "_T" + operand2, -1);
            }
            parsed += code;
            tokentypes.pop();
          }
          tokens.push(_token);
          if (_tokentype == TOKEN_FUNCTION) { _tokentype = TOKEN_LPAREN; }
          tokentypes.push(new Integer(_tokentype));
        }
        getToken();
      }
      while (!tokens.empty()) {
        operator = (String) tokens.pop();
        operand2 = ((Integer) operands.pop()).intValue();
        int simple = ((Integer) operandTypes.pop()).intValue();
        _tempvar = operand2;
        operand1 = ((Integer) operands.peek()).intValue();
        int operand1Type = ((Integer) operandTypes.peek()).intValue();
        if (operand1Type >= 0) {
          if (bfbasic._debug > 0) {
            parsed += "\n(_T" + operand1 + "=" + operand1Type + ")\n";
          }
          parsed += "@_T" + operand1 + "[-]" + addTo(operand1Type);
          operandTypes.pop();
          operandTypes.push(new Integer(-1));
        }
        /*
         if (((Integer) tokentypes.pop()).intValue() == TOKEN_LEVEL4) {
         operand1 = -1;
         } else {
         operand1 = ((Integer) operands.peek()).intValue();
         }
         */
        String code = getCode("_T" + operand1, operator, "_T" + operand2, simple);
        if (code == null) {
          if (bfbasic._debug > 0) {
            parsed += "\n(_T" + operand2 + "=" + simple + ")\n";
          }
          parsed += "@_T" + operand2 + "[-]" + addTo(simple);
          code = getCode("_T" + operand1, operator, "_T" + operand2, -1);
        }
        parsed += code;
        tokentypes.pop();
      }
    } catch (EmptyStackException e) {
      bfbasic.errout("Mismatched or empty parenthesis");
    }
    if (operandTypes.size() == 0) {
      bfbasic.errout("Mismatched or empty parenthesis");
    }
    if (operandTypes.size() > 0) {
      int value = ((Integer) operandTypes.pop()).intValue();
      if (value >= 0) {
        parsed += "@_T" + ((Integer) operands.pop()).intValue() +
                  "[-]" + addTo(value);
      }
    }
    _parse = parsed;
    return parsed;
  }
  
  //------------------------------------------------------------------
  // getCode
  //------------------------------------------------------------------
  String getCode(String operand1, String operator_, String operand2, int simple) {
    String code = "";
    if (bfbasic._debug > 0) {
      if (operand1.equals("_T-1")) {
        code += "\n(_T" + operand2 + "=-_T" + operand2 + ")\n";
      } else if (simple < 0) {
        code += "\n(" + operand1 + "=" + operand1 + bfbasic.debugtext(operator_)
        + operand2 + ")\n";
      } else {
        code += "\n(" + operand1 + "=" + operand1 + bfbasic.debugtext(operator_)
        + simple + ")\n";
      }
    }
    String operator = operator_.trim();
    if (operator.equals("!")) {
      //V1=-V1
      //  @T0[-]                T0=0
      //  @V1[@T0-@V1-]         T0=T0-V1:V1=0
      //  @T0[@V1+@T0-]         V1=T0:T0=0
      if (simple < 0) {
        code += "@_0[-]@_T" + operand2 + "[@_0-@_T" + operand2 + "-]"
        + "@_0[@_T" + operand2 + "+@_0-]";
      } else {
        return null;
      }
    } else if (operator.equals("+")) {
      //V1=V1+V2
      //  (@T0[-])              (T0=0)
      //  @V2[@V1+@T0+@V2-]     V1=V1+V2:T0=V2:V2=0
      //  @T0[@V2+@T0-]         V2=T0:T0=0
      if (simple < 0) {
        code += "@" + operand2 + "[@" + operand1 + "+@_0+@" + operand2 + "-]"
        + "@_0[@" + operand2 + "+@_0-]";
      } else {
        code += "@" + operand1 + addTo(simple); 
      }  
    } else if (operator.equals("-")) {
      //V1=V1-V2
      //  (@T0[-])              (T0=0)
      //  @V2[@V1-@T0+@V2-]     V1=V1-V2:T0=V2:V2=0
      //  @T0[@V2+@T0-]         V2=T0:T0=0
      if (simple < 0) {
        code += "@" + operand2 + "[@" + operand1 + "-@_0+@" + operand2 + "-]"
        + "@_0[@" + operand2 + "+@_0-]";
      } else {
        code += "@" + operand1 + addTo(-simple); 
      }  
    } else if (operator.equals("*")) {
      //V1=V1*V2
      //  (@T0[-])              (T0=0)
      //  (@T1[-])              (T1=0)
      //  (@T2[-])              (T2=0)
      //  @V1[@T1+@V1-]         T1=V1:V1=0
      //  @T1[                  DO WHILE T1<>0
      
      //    @V2[@V1+@T0+@V2-]     V1=V1+V2:T0=V2:V2=0
      
      //    @T0[@V2+@T0-]         V2=T0:T0=0
      //  @T1-]                 T1=T1-1:LOOP
      if (simple < 0) {
        code += "@" + operand1 + "[@_1+@" + operand1 + "-]@_1["
        + "@" + operand2 + "[@" + operand1 + "+@_0+@" + operand2 + "-]"
        + "@_0[@" + operand2 + "+@_0-]@_1-]";
      } else {
        return null;
      }
    } else if (operator.equals("/")) {
      //V1=V1/V2
      //  (@T0[-])              (T0=0)
      //  (@T1[-])              (T1=0)
      //  (@T2[-])              (T2=0)
      //  (@T3[-])              (T3=0)
      //  @V1[@T3+@V1-]         T3=V1:V1=0
      //  @T3[                  DO WHILE T3<>0
      
      //    @V2[@T2+@T0+@V2-]     T2=V2:T0=V2:V2=0
      //    @T0[@V2+@T0-]         V2=T0:T0=0
      
      //    @T2[                  DO WHILE T2<>0
      //      @T3-[@T1+@T0+@T3-]    T3=T3-1:T1=T3:T0=T3
      //      @T0[@T3+@T0-]+        T3=0:T0=1
      //      @T1[                  DO WHILE T1<>0
      //        @T0-                  T0=0
      //      @T1[-]]               END IF:T1=0
      
      //      @T0[                  DO WHILE T0<>0
      //        @T2-[                 T2=T2-1:IF T2<>0 THEN
      //          @V1-                  V1=V1-1
      //        @T2[-]]+              END IF:T2=1
      //      @T0[-]]               END IF:T0=0
      //    @T2-]                 T2=T2-1:LOOP
      //    @V1+                  V1=V1+1
      //  @T3]                  LOOP:(T3=0)
      if (simple < 0) {
        code += "@" + operand1 + "[@_3+@" + operand1 + "-]@_3["
        + "@" + operand2 + "[@_2+@_0+@" + operand2 + "-]@_0[@" + operand2 + "+@_0-]"
        + "@_2[@_3-[@_1+@_0+@_3-]@_0[@_3+@_0-]+@_1[@_0-@_1[-]]"
        + "@_0[@_2-[@" + operand1 + "-@_2[-]]+@_0[-]]@_2-]@" + operand1 + "+@_3]";
      } else {
        return null;
      }
    } else if (operator.equals("=")) {
      //V1=V1=V2
      //  (@T0[-])              (T0=0)
      //  (@T1[-])              (T1=0)
      //  @V1[@T1+@V1-]-        T1=V1:V1=255
      //  @V2[@T1-@T0+@V2-]     T1=T1-V2:T0=V2:V2=0
      //  @T0[@V2+@T0-]         V2=T0:T0=0
      //  @T1[                  IF T1<>0 THEN
      //    @V1+                  V1=0
      //  @T1[-]]               END IF:T1=0
      if (simple < 0) {
        code += "@" + operand1 + "[@_1+@" + operand1 + "-]-@" + operand2 + "[@_1-@_0+@"
        + operand2 + "-]@_0[@" + operand2 + "+@_0-]@_1[@" + operand1 + "+@_1[-]]";
      } else {
        return null;
      }
    } else if (operator.equals("<>")) {
      //V1=V1<>V2
      //  (@T0[-])              (T0=0)
      //  (@T1[-])              (T1=0)
      //  @V1[@T1+@V1-]         T1=V1:V1=0
      //  @V2[@T1-@T0+@V2-]     T1=T1-V2:T0=V2:V2=0
      //  @T0[@V2+@T0-]         V2=T0:T0=0
      //  @T1[                  IF T1<>0 THEN
      //    @V1-                  V1=255
      //  @T1[-]]               END IF:T1=0
      if (simple < 0) {
        code += "@" + operand1 + "[@_1+@" + operand1 + "-]@" + operand2 + "[@_1-@_0+@"
        + operand2 + "-]@_0[@" + operand2 + "+@_0-]@_1[@" + operand1 + "-@_1[-]]";
      } else {
        return null;
      }
    } else if (operator.equals("<")) {
      //V1=V1<V2
      //  (@T0[-])              (T0=0)
      //  (@T1[-])              (T1=0)
      //  (@T2[-])              (T2=0)
      //  (@T3[-])              (T3=0)
      //  (@T4[-])              (T4=0)
      //  @V1[@T4+@V1-]         T4=V1:V1=0
      
      //  @V2[@T1+@T2+@T0+@V2-] T1=V2:T2=V2:T0=V2:V2=0
      
      //  @T0[@V2+@T0-]         V2=T0:T0=0
      //  @T2[                  DO WHILE T2<>0
      //    @T4[@T3+@T0+@T4-]     T3=T4:T0=T4:T4=0
      //    @T0[@T4+@T0-]+        T4=T0:T0=1
      
      //    @T3[                  IF T3<>0 THEN
      //      @T1-                  T1=T1-1
      //      @T4-                  T4=T4-1
      //      @T0-                  T0=0
      //    @T3[-]]               END IF:T3=0
      //    @T0[                  IF T0<>0 THEN
      //      @T2[-]+               T2=1
      //    @T0-]                 END IF:T0=0
      //  @T2-]                 T2=T2-1:LOOP
      
      //  @T1[                  IF T1<>0 THEN
      //    @V1-                  V1=255
      //  @T1[-]]               END IF:T1=0
      //  @T4[-]                T4=0
      if (simple < 0) {
        code += "@" + operand1 + "[@_4+@" + operand1 + "-]"
        + "@" + operand2 + "[@_1+@_2+@_0+@" + operand2 + "-]"
        + "@_0[@" + operand2 + "+@_0-]@_2[@_4[@_3+@_0+@_4-]@_0[@_4+@_0-]+"
        + "@_3[@_1-@_4-@_0-@_3[-]]@_0[@_2[-]+@_0-]@_2-]"
        + "@_1[@" + operand1 + "-@_1[-]]@_4[-]";
      } else {
        return null;
      }
    } else if (operator.equals("<=")) {
      //V1=V1<=V2
      //  (@T0[-])              (T0=0)
      //  (@T1[-])              (T1=0)
      //  (@T2[-])              (T2=0)
      //  (@T3[-])              (T3=0)
      //  (@T4[-])              (T4=0)
      //  @V1[@T4+@V1-]-        T4=V1:V1=255
      
      //  @V2[@T1+@T2+@T0+@V2-] T1=V2:T2=V2:T0=V2:V2=0
      
      //  @T0[@V2+@T0-]         V2=T0:T0=0
      //  @T2[                  DO WHILE T2<>0
      //    @T4[@T3+@T0+@T4-]     T3=T4:T0=T4:T4=0
      //    @T0[@T4+@T0-]+        T4=T0:T0=1
      
      //    @T3[                  IF T3<>0 THEN
      //      @T1-                  T1=T1-1
      //      @T4-                  T4=T4-1
      //      @T0-                  T0=0
      //    @T3[-]]               END IF:T3=0
      //    @T0[                  IF T0<>0 THEN
      //      @T2[-]+               T2=1
      //    @T0-]                 END IF:T0=0
      //  @T2-]                 T2=T2-1:LOOP
      
      //  @T4[                  IF T4<>0 THEN
      //    @V1-                  V1=255
      //  @T4[-]]               END IF:T4=0
      //  @T1[-]                T1=0
      if (simple < 0) {
        code += "@" + operand1 + "[@_4+@" + operand1 + "-]-"
        + "@" + operand2 + "[@_1+@_2+@_0+@" + operand2 + "-]"
        + "@_0[@" + operand2 + "+@_0-]@_2[@_4[@_3+@_0+@_4-]@_0[@_4+@_0-]+"
        + "@_3[@_1-@_4-@_0-@_3[-]]@_0[@_2[-]+@_0-]@_2-]"
        + "@_4[@" + operand1 + "+@_4[-]]@_1[-]";
      } else {
        return null;
      }
    } else if (operator.equals(">")) {
      //V1=V1>V2
      //  (@T0[-])              (T0=0)
      //  (@T1[-])              (T1=0)
      //  (@T2[-])              (T2=0)
      //  (@T3[-])              (T3=0)
      //  (@T4[-])              (T4=0)
      //  @V1[@T4+@V1-]         T4=V1:V1=0
      
      //  @V2[@T1+@T2+@T0+@V2-] T1=V2:T2=V2:T0=V2:V2=0
      
      //  @T0[@V2+@T0-]         V2=T0:T0=0
      //  @T2[                  DO WHILE T2<>0
      //    @T4[@T3+@T0+@T4-]     T3=T4:T0=T4:T4=0
      //    @T0[@T4+@T0-]+        T4=T0:T0=1
      
      //    @T3[                  IF T3<>0 THEN
      //      @T1-                  T1=T1-1
      //      @T4-                  T4=T4-1
      //      @T0-                  T0=0
      //    @T3[-]]               END IF:T3=0
      //    @T0[                  IF T0<>0 THEN
      //      @T2[-]+               T2=1
      //    @T0-]                 END IF:T0=0
      //  @T2-]                 T2=T2-1:LOOP
      
      //  @T4[                  IF T4<>0 THEN
      //    @V1-                  V1=255
      //  @T4[-]]               END IF:T4=0
      //  @T1[-]                T1=0
      if (simple < 0) {
        code += "@" + operand1 + "[@_4+@" + operand1 + "-]"
        + "@" + operand2 + "[@_1+@_2+@_0+@" + operand2 + "-]"
        + "@_0[@" + operand2 + "+@_0-]@_2[@_4[@_3+@_0+@_4-]@_0[@_4+@_0-]+"
        + "@_3[@_1-@_4-@_0-@_3[-]]@_0[@_2[-]+@_0-]@_2-]"
        + "@_4[@" + operand1 + "-@_4[-]]@_1[-]";
      } else {
        return null;
      }
    } else if (operator.equals(">=")) {
      //V1=V1>=V2
      //  (@T0[-])              (T0=0)
      //  (@T1[-])              (T1=0)
      //  (@T2[-])              (T2=0)
      //  (@T3[-])              (T3=0)
      //  (@T4[-])              (T4=0)
      //  @V1[@T4+@V1-]-        T4=V1:V1=255
      
      //  @V2[@T1+@T2+@T0+@V2-] T1=V2:T2=V2:T0=V2:V2=0
      
      //  @T0[@V2+@T0-]         V2=T0:T0=0
      //  @T2[                  DO WHILE T2<>0
      //    @T4[@T3+@T0+@T4-]     T3=T4:T0=T4:T4=0
      //    @T0[@T4+@T0-]+        T4=T0:T0=1
      
      //    @T3[                  IF T3<>0 THEN
      //      @T1-                  T1=T1-1
      //      @T4-                  T4=T4-1
      //      @T0-                  T0=0
      //    @T3[-]]               END IF:T3=0
      //    @T0[                  IF T0<>0 THEN
      //      @T2[-]+               T2=1
      //    @T0-]                 END IF:T0=0
      //  @T2-]                 T2=T2-1:LOOP
      
      //  @T1[                  IF T1<>0 THEN
      //    @V1+                  V1=0
      //  @T1[-]]               END IF:T1=0
      //  @T4[-]                T4=0
      if (simple < 0) {
        code += "@" + operand1 + "[@_4+@" + operand1 + "-]-"
        + "@" + operand2 + "[@_1+@_2+@_0+@" + operand2 + "-]"
        + "@_0[@" + operand2 + "+@_0-]@_2[@_4[@_3+@_0+@_4-]@_0[@_4+@_0-]+"
        + "@_3[@_1-@_4-@_0-@_3[-]]@_0[@_2[-]+@_0-]@_2-]"
        + "@_1[@" + operand1 + "+@_1[-]]@_4[-]";
      } else {
        return null;
      }
    } else if (operator.equals("OR")) {
      //V1=V1 OR V2
      //  (@T0[-])              (T0=0)
      //  @V1[@T0+@V1-]         T0=V1:V1=0
      //  @T0[                  IF T0<>0 THEN
      //    @V1-                  V1=255
      //  @T0[-]]               END IF:T0=0
      
      //  @V2[@T0+@T1+@V2-]     T0=V2:T1=V2:V2=0
      
      //  @T1[@V2+@T1-]         V2=T1:T1=0
      //  @T0[                  IF T0<>0 THEN
      //    @V1[-]-               V1=255
      //  @T0[-]]               END IF:T0=0
      if (simple < 0) {
        code += "@" + operand1 + "[@_0+@" + operand1 + "-]@_0[@" + operand1 + "-@_0[-]]"
        + "@" + operand2 + "[@_0+@_1+@" + operand2 + "-]"
        + "@_1[@" + operand2 + "+@_1-]@_0[@" + operand1 + "[-]-@_0[-]]";
      } else {
        return null;
      }
    } else if (operator.equals("AND")) {
      //V1=V1 AND V2
      //  (@T0[-])              (T0=0)
      //  @V1[@T0+@V1-]         T0=V1:V1=0
      //  @T0[                  IF T0<>0 THEN
      //    @T0[-]                T0=0
      
      //    @V2[@T0+@T1+@V2-]     T0=V2:T1=V2:V2=0
      
      //    @T1[@V2+@T1-]         V2=T1:T1=0
      //    @T0[                  IF T0<>0 THEN
      //      @V1-                  V1=255
      //    @T0[-]]               END IF:T0=0
      //  ]                     END IF
      if (simple < 0) {
        code += "@" + operand1 + "[@_0+@" + operand1 + "-]@_0[@_0[-]"
        + "@" + operand2 + "[@_0+@_1+@" + operand2 + "-]"
        + "@_1[@" + operand2 + "+@_1-]@_0[@" + operand1 + "-@_0[-]]]";
      } else {
        return null;
      }
    } else {
      bfbasic.errout("Unknown operator \'" + operator + "\'");
    }
    return code;
  }
  
}
