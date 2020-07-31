//====================================================================
// BFBASIC -- Basic programming language compiler for BF
// Filename : bfbasic.java
// Language : Java 1.2+
// Version  : 1.41
// Copyright: (C) 2001-2005 Jeffry Johnston
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as
// published by the Free Software Foundation. See the file LICENSE
// for more details.
//
// Version history.  See bfbasic.txt for more information:
// 1.41    29 Jun 2005
// 1.40    17 Mar 2005
// 1.30    30 Oct 2003
// 1.20    23 Oct 2003
// 1.10    16 Oct 2003
// 1.00    15 Oct 2003
// 0.90    23 Sep 2003
// 0.80    20 Sep 2003
// 0.70    14 Mar 2003
// 0.60    12 Mar 2003
// 0.50    11 Mar 2003 Initial release
//====================================================================

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Stack;

//********************************************************************
// bfbasic
//********************************************************************
public class bfbasic {
  public static final String VERSION = "1.41";
  
  static final int WRAP_DEFAULT = 72;

  static boolean _debug = false, _extraDebug = false, 
                 _insert = false, _crlf = false,
                 _needPre = true, _needPost = false;
  static BufferedReader _in; 
  static PrintStream _out;
  static StringWriter _tempString = new StringWriter(); 
  static PrintWriter _tempOut = new PrintWriter(_tempString); 
  static LinkedHashMap _var = new LinkedHashMap(), 
                       _label = new LinkedHashMap();
  static int _line = 0, _wrapWidth = 0, _optLevel = 2;
  static String _p = "", _sourceLine = "";
  static char _a;
  static Stack _doStack = new Stack(), _forStack = new Stack(),
               _forTopStack = new Stack(), _forStepStack = new Stack(),
               _ifStack = new Stack();
  static int _annex = 1, _doAnnex = 1, _ifAnnex = 1, _forAnnex = 1, 
             _gosubAnnex = 0;

  //------------------------------------------------------------------
  // main
  //------------------------------------------------------------------
  public static void main(String[] cmdline) {
    // display copyright text
    System.out.println("BFBASIC Basic Compiler, version " + VERSION
                       + ".  Copyright 2001-2005 Jeffry Johnston.");

    // process command line
    String f = "", f2 = "";
    int n;
    for (n = 0; ; n++) {
      try {
        if (cmdline[n].equals("-?") || cmdline[n].equals("-h") ) {
          usage();
          System.exit(0);
        } else if (cmdline[n].equals("-c")) { // crlf
          _crlf = true;
        } else if (cmdline[n].equals("-d")) { // debug output
          _wrapWidth = WRAP_DEFAULT;
          _debug = true;
        } else if (cmdline[n].equals("-D")) { // verbose debug output
          _wrapWidth = WRAP_DEFAULT;
          _debug = true;
          _extraDebug = true;
        } else if (cmdline[n].equals("-O1")) { // optimization level
          _optLevel = 1;
        } else if (cmdline[n].equals("-O2")) { // optimization level
          _optLevel = 2;
        } else if (cmdline[n].equals("-O3")) { // optimization level
          _optLevel = 3;
        } else if (cmdline[n].equals("-o")) {
          try {
            f2 = cmdline[n + 1];
          } catch (ArrayIndexOutOfBoundsException e) {
            errout("Error: Missing output filename"); 
          }
          n++;
        } else if (cmdline[n].equals("-w")) {
          _wrapWidth = WRAP_DEFAULT;
          String width = null;
          try {
            width = cmdline[n + 1];
          } catch (ArrayIndexOutOfBoundsException e) {
          }
          if (width != null) {
            try {
              _wrapWidth = Integer.parseInt(width);
              if (_wrapWidth < 1) {
                errout("Error: Bad wrap width: " + _wrapWidth);
              }
              n++;
            } catch (NumberFormatException e) {
            }  
          }
        } else if (cmdline[n].startsWith("-")) {
          errout("Error: Unrecognized option: " + cmdline[n]);
        } else {
          if (f == "")
            f = cmdline[n];
          else if (f2 == "")
            f2 = cmdline[n]; 
        }
      } catch (ArrayIndexOutOfBoundsException e) {
        break;
      }
    }
    if (f.equals("")) {
      usage();
      errout("Error: No input file given");
    }
    if ((n = f.indexOf('.')) == -1) {
      // create default output filename
      if (f2.equals("")) { f2 = f + ".bf"; }
      // add missing ".bas" extension
      f += ".bas";
    } else {
      // create default output filename
      if (f2.equals("")) { f2 = f.substring(0, n) + ".bf"; }
    }

    // display filenames
    System.out.println();
    System.out.println("Input file : " + f);
    System.out.println("Output file: " + f2);

    if (f.equals(f2)) {
      errout("Error: Filenames must be different");
    }

    // open files
    try {
      _in = new BufferedReader(new FileReader(f)); // Java 1.1
    } catch (FileNotFoundException e) {
      errout("Error opening '" + f + "'.  File not found");
    }
    try {
      _out = new PrintStream(new FileOutputStream(f2));
    } catch (IOException e) {
      errout("Error opening '" + f2 + "' for output");
    }

    // output banner
    _out.print("[ BF Code Produced by BFBASIC " + VERSION);
    if (_debug) { _out.print(" (debug comments)"); }
    _out.println(" ]");

    // add reserved variables
    // @_Q  0 = quit (end), 1 = otherwise
    // @_G  0 = performing a jump (goto), 1 = otherwise
    String l = "QGT0123456";
    for (n = 0; n < l.length(); n++) {
      addvar("_" + l.charAt(n), 1);
    }

    // output header
    first();

    // main loop
    while (true) {
      _sourceLine = _sourceLine.trim();

      // strip leading colons, if any
      while (_sourceLine.length() > 0 && _sourceLine.charAt(0) == ':') {
        _sourceLine = (_sourceLine.substring(1)).trim();
      }

      // remove line comment
      if (_sourceLine.length() > 0 && (_sourceLine.charAt(0) == '\'')) {
        _sourceLine = "";
      }

      // if line is empty, read another one
      if (_sourceLine.equals("")) {
        try {
          _line++;
          _sourceLine = _in.readLine();
        } catch (IOException e) {
          errout("Error reading '" + f + "'");
        }
        // no more source lines?  done compiling
        if (_sourceLine == null) {
          break;
        }
        _sourceLine = _sourceLine.trim();
      }

      // parse first token of statement
      parse();
      _p = _p.toUpperCase();
      if (_p.equals("")) {
        // NOP
      } else if (_p.equals("BEEP")) {
        bf_beep();
      } else if (_p.equals("BF")) {
        bf_bf();
      } else if (_p.equals("CLS")) {
        bf_cls();
      } else if (_p.equals("COLOR") || _p.equals("COLOUR")) {
        bf_color();
      } else if (_p.equals("DIM")) {
        bf_dim();
      } else if (_p.equals("DO")) {
        bf_do();
      } else if (_p.equals("ELSE")) {
        bf_else();
      } else if (_p.equals("END")) {
        bf_end();
      } else if (_p.equals("EXIT")) {
        bf_exit();
      } else if (_p.equals("FOR")) {
        bf_for();
      } else if (_p.equals("GOSUB")) {
        bf_gosub();
      } else if (_p.equals("GOTO")) {
        bf_goto();
      } else if (_p.equals("IF")) {
        bf_if();
      } else if (_p.equals("INPUT")) {
        bf_input();
      } else if (_p.equals("LET")) {
        parse();
        bf_let();
      } else if (_p.equals("LOCATE")) {
        bf_locate();
      } else if (_p.equals("LOOP")) {
        bf_loop();
      } else if (_p.equals("NEXT")) {
        bf_next();
      } else if (_p.equals("PRINT")) {
        bf_print();
      } else if (_p.equals("RANDOMIZE")) {
        bf_randomize();
      } else if (_p.equals("REM")) {
        bf_rem();
      } else if (_p.equals("RETURN")) {
        bf_return();
      } else if (_p.equals("STOP")) {
        bf_stop();
      } else if (_p.equals("SWAP")) {
        bf_swap();
      } else if (_p.equals("SYSTEM")) {
        bf_system();
      } else if (_p.equals("WEND")) {
        bf_loop();
      } else if (_p.equals("WHILE")) {
        _sourceLine = "WHILE " + _sourceLine;
        bf_do();
      } else if (_p.equals("?")) {
        bf_print();
      } else if (_a == ':') {
        bf_label();
      } else {
        bf_let();
      }
    }

    // output footer
    last();
    String out = _tempString.getBuffer().toString();
    arrows(out, false);
    write(arrows(out, true));
    if (_debug) { write("\n"); }
    write("@\n");

    // display successful completion text
    System.out.println();
    System.out.println("Done, line " + _line + ".");
  }

  //------------------------------------------------------------------
  // addvar -- adds a new variable name and size to internal table
  //------------------------------------------------------------------
  public static void addvar(String varname, int elements) {
    varname = varname.toUpperCase();
    for (int n = 0; n < varname.length(); n++) {
      char c = varname.charAt(n);
      if ((n == 0 && (c < '0' || c > '9')) && ((c < 'A' || c > 'Z')
          && c != '_' && c != '~')) {
        errout("Illegal variable name '" + varname + "'");
      }
    }
    if (_debug) { write("(DIM " + varname + "(" + elements + "))\n"); }
    if (_var.containsKey(varname)) {
      errout("Variable '" + varname + "' already dimensioned");
    }
    _var.put(varname, new Integer(elements)); 
  }

  //------------------------------------------------------------------
  // arrows -- turns @ variables into BF < and >, writes code to disk
  //------------------------------------------------------------------
  public static String arrows(String o, boolean write) {
    int l2, pos, mp = 0;
    char c;
    String varname = "";
    StringBuffer out = new StringBuffer("");
    LinkedList interaction = new LinkedList();

    for (int l = 0; l < o.length(); l++) {
      // get character
      c = o.charAt(l);

      // variable
      if (c == '@') {
        // find end of variable name
        for (l2 = l + 1; l2 < o.length(); l2++) {
          if (("+-<>[].,@".indexOf(o.charAt(l2))) != -1) {
            // get variable name (middle of string)
            varname = o.substring(l + 1, l2);
            break;
          } else if (l2 == o.length() - 1) {
            // get variable name (last thing in string)
            varname = o.substring(l + 1);
            l++;
          }
        }
        varname = varname.toUpperCase();
        l = l2 - 1;

        // search for variable
        pos = 0;
        if (_var.containsKey(varname)) {
          pos = ((Integer) _var.get(varname)).intValue();
        } else {
          // variable doesn't exist, add it
          if (varname.charAt(0) == '~') {
            addvar(varname, 25);
          } else {
            addvar(varname, 1);
          }

          // get position of newest variable
          pos = ((Integer) _var.get(varname)).intValue();
        }
        if (_optLevel >= 2 && !write) {
          interaction.add(varname);
        }
        
        if (write && _extraDebug) { out.append(mp + ""); }

        // output > and <
        if (write) {
          if (mp < pos) { out.append(string(pos - mp, '>')); }
          if (mp > pos) { out.append(string(mp - pos, '<')); }
          mp = pos;
          if (write && _extraDebug) { out.append("@" + varname); }
        }  
      } else {
        // output char
        if (write) { out.append(c + ""); }
      }
    }
    
    // If we're not writing things out this pass, then figure out 
    // the best variable ordering.  
    if (!write) {
      if (_optLevel == 1) { 
        Iterator iter = _var.keySet().iterator();
        int start = 0;
        while (iter.hasNext()) {
          Object key = iter.next();
          int next = ((Integer)_var.get(key)).intValue();
          _var.put(key, new Integer(start));
          start += next;
        }
      } else if (_optLevel >= 2) {
        // find the best ordering
        String[] names = (String[]) _var.keySet().toArray(
            new String[_var.keySet().size()]);
        Variables var = new Variables(names, (Integer[]) _var.values().
            toArray(new Integer[_var.values().size()]), 
            interaction, 10);
        var.findBestOrder();
        
        // clear out the old variable info and rebuild 
        _var.clear();
        int order[] = var.getOrder();
        int start[] = var.getStart();
        for (int i = 0; i < start.length; i++) {
          _var.put(names[Math.abs(order[i])], new Integer(start[i]));
        }
      } 
    }
    
    return out.toString();
  }

  //------------------------------------------------------------------
  // bf_beep -- writes out BF code for the BEEP statement
  //------------------------------------------------------------------
  public static void bf_beep() {
    //BEEP
    //  @T0+++++++.[-]          T0=7:PRINT CHR$(T0);:T0=0
    String o = pre() + "@_0+++++++.[-]" + post();
    if (_debug) { o = "\n(BEEP)\n" + o; }
    writeTemp(o); //arrows(o);
  }

  //------------------------------------------------------------------
  // bf_bf -- writes out BF code for the BF statement
  //------------------------------------------------------------------
  public static void bf_bf() {
    _sourceLine = (_a + _sourceLine).trim();
    // BF (commands)
    int n;
    if ((n = _sourceLine.indexOf('\'')) != -1) {
      _sourceLine = _sourceLine.substring(0, n);
    }
    _needPost = false;
    String o = pre() + _sourceLine.trim() + post();
    if (_debug) { o = "\n(BF)\n" + o; }
    writeTemp(o); //arrows(o);
    _sourceLine = "";
    _needPre = false;
  }

  //------------------------------------------------------------------
  // bf_cls -- builds BFBASIC code for the CLS statement
  //------------------------------------------------------------------
  public static void bf_cls() {
    //CLS
    //  (...)                   builds PRINT CHR$(27) + "[2J";:
    if (_debug) { writeTemp("\n{CLS}\n"); }
    _sourceLine = "PRINT \"\033[2J\";:" + _sourceLine;
  }

  //------------------------------------------------------------------
  // bf_color -- builds BFBASIC code for the COLOR statement
  //------------------------------------------------------------------
  public static void bf_color() {
    _sourceLine = (_a + _sourceLine).trim();
    int n = findexpr("", 0);
    if (n == -1) { errout("Syntax error"); }
    String expr = (_sourceLine.substring(0, n)).trim();
    _sourceLine = "PRINT \"\033[\"; " + expr + "; \"m\";:"
                 + (_sourceLine.substring(n)).trim();
    if (_debug) { writeTemp("\n{COLOR " + debugtext(expr) + "}\n"); }
  }

  //------------------------------------------------------------------
  // bf_dim -- code for the DIM statement
  //------------------------------------------------------------------
  public static void bf_dim() {
    parse();
    int n = 0;
    if (_a == '(') {
      // DIM V1(#)
      String p0 = _p;
      parse();
      if (_a != ')') { errout("Mismatched parenthesis"); }
      try {
        n = Integer.parseInt(_p);
      } catch (NumberFormatException e) {
        errout("Invalid number '" + _p + "'");
      }
      if (n < 0 || n > 255) {
        errout("Array size '" + n + "' out of range (0 - 255)");
      }
      // array length=elements*2+3, elements=a+1, (a+1)*2+3=a*2+5
      addvar("~" + p0, n * 2 + 5);
    } else {
      // DIM V1
      addvar(_p, 1);
    }
  }

  //------------------------------------------------------------------
  // bf_do -- builds BFBASIC code for the DO statement
  //------------------------------------------------------------------
  public static void bf_do() {
    String p0 = "_D" + _doAnnex;
    if (parseif()) {
      // no params
      // DO
      if (_debug) { writeTemp("\n{DO}\n"); }
      _doStack.push(new Integer(_doAnnex));
    } else {
      // params
      parse();
      _p = _p.toUpperCase();
      if (_p.equals("WHILE")) {
        // DO WHILE expr
        _sourceLine = (_a + _sourceLine).trim();
        int n = findexpr("", 0);
        if (n == -1) { errout("Syntax error"); }
        String expr = (_sourceLine.substring(0, n)).trim();
        if (_debug) { writeTemp("\n{DO WHILE " + debugtext(expr) + "}\n"); }
        _doStack.push(new Integer(-_doAnnex));
        _sourceLine = "IF NOT(" + expr + ") THEN GOTO _D" + (_doAnnex + 1) + ":"
                     + (_sourceLine.substring(n)).trim();
      } else if (_p.equals("UNTIL")) {
        // DO UNTIL expr
        _sourceLine = (_a + _sourceLine).trim();
        int n = findexpr("", 0);
        if (n == -1) { errout("Syntax error"); }
        String expr = (_sourceLine.substring(0, n)).trim();
        if (_debug) { writeTemp("\n{DO UNTIL " + debugtext(expr) + "}\n"); }
        _doStack.push(new Integer(-_doAnnex));
        _sourceLine = "IF " + expr + " THEN GOTO _D" + (_doAnnex + 1) + ":"
                     + (_sourceLine.substring(n)).trim();
      } else {
        errout("Syntax error");
      }
    }
    _p = p0;
    bf_label();
    _doAnnex += 2;
  }

  //------------------------------------------------------------------
  // bf_else -- builds BFBASIC code for the ELSE statement
  //------------------------------------------------------------------
  public static void bf_else() {
    // ELSE
    if (_debug) { writeTemp("\n{ELSE}\n"); }
    if (_ifStack.empty()) { errout("ELSE without IF"); }
    Integer l = (Integer) _ifStack.pop();
    _sourceLine = "GOTO _I" + _ifAnnex + ":_I" + l + ":" + _sourceLine;
    _ifStack.push(new Integer(_ifAnnex++));
  }

  //------------------------------------------------------------------
  // bf_end -- writes out BF code for the END and END IF statements
  //------------------------------------------------------------------
  public static void bf_end() {
    parse();
    if (_p.equalsIgnoreCase("IF")) {
      // END IF
      if (_debug) { writeTemp("\n{END IF}\n"); }
      if (_ifStack.empty()) { errout("END IF without IF"); }
      Integer l = (Integer) _ifStack.pop();
      _sourceLine = "_I" + l + ":" + _sourceLine;
    } else {
      //END
      //  @G-                   G=0 (no more basic statements execute)
      //  @Q-                   Q=0 (quit)
      _needPost = true;
      _sourceLine = (_p + _a + _sourceLine).trim();
      String o = pre() + "@_G-@_Q-" + post();
      if (_debug) { o = "\n(END)\n" + o; }
      writeTemp(o); //arrows(o);
      _needPre = true;
    }
  }

  //------------------------------------------------------------------
  // bf_exit -- builds BFBASIC code for the EXIT statements
  //------------------------------------------------------------------
  public static void bf_exit() {
    parse();
    _p = _p.toUpperCase();
    if (_p.equals("DO")) {
      if (_debug) { writeTemp("\n{EXIT DO}\n"); }
      if (_doStack.empty()) { errout("EXIT DO without DO"); }
      Integer l = (Integer) _doStack.pop();
      _sourceLine = "GOTO _D" + (Math.abs(l.intValue()) + 1) + ":" + _sourceLine;
      if (l.intValue() > 0) { l = new Integer(-l.intValue()); }
      _doStack.push(l);
    } else if (_p.equals("FOR")) {
      if (_debug) { writeTemp("\n{EXIT FOR}\n"); }
      if (_forStack.empty()) { errout("EXIT FOR without FOR"); }
      Integer l = (Integer) _forStack.pop();
      _sourceLine = "GOTO _F" + (Math.abs(l.intValue()) + 1) + ":" + _sourceLine;
      if (l.intValue() > 0) { l = new Integer(-l.intValue()); }
      _forStack.push(l);
    } else {
      errout("Syntax error");
    }
  }

  //------------------------------------------------------------------
  // bf_for -- builds BFBASIC code for the FOR statement
  //------------------------------------------------------------------
  public static void bf_for() {
    // FOR expr=expr2 TO expr3
    String p0 = "_F" + _forAnnex;
    int n = findexpr("=", 0);
    if (n == -1) { errout("Syntax error"); }
    String expr = (_sourceLine.substring(0, n)).trim();
    _sourceLine = (_sourceLine.substring(n + 1)).trim();
    n = findexpr("TO", 0);
    if (n == -1) { errout("Syntax error"); }
    String expr2 = (_sourceLine.substring(0, n)).trim();
    _sourceLine = (_sourceLine.substring(n + 2)).trim();
    n = findexpr("", 0);
    if (n == -1) { errout("Syntax error"); }
    String expr3 = (_sourceLine.substring(0, n)).trim();
    _forStack.push(new Integer(-_forAnnex));
    _forTopStack.push(new String(expr3));
    _sourceLine = expr + "=" + expr2 + ":" + p0
                 + ":" + (_sourceLine.substring(n)).trim();
    if (_debug) {
      writeTemp("\n{FOR " + debugtext(expr) + "=" + debugtext(expr2)
            + " TO " + debugtext(expr3) + "}\n");
    }
    _forAnnex += 2;
  }

  //------------------------------------------------------------------
  // bf_gosub -- builds BFBASIC code for the GOSUB statement
  //------------------------------------------------------------------
  public static void bf_gosub() {
    // GOSUB label
    parse();
    int n = _gosubAnnex++;
    if (n == 256) {
      System.out.println("Warning: 257th GOSUB encountered.  " +
          "Will crash if using 8-bit cells.");
    }
    _sourceLine = "_GS(_GP)=" + n + ":_GP=_GP+1:GOTO " + _p + ":"
                 + ":_G" + n + ":" + _sourceLine;
    if (_debug) { writeTemp("\n{GOSUB " + _p + "}\n"); }
  }

  //------------------------------------------------------------------
  // bf_goto -- writes out BF code for the GOTO statement
  //------------------------------------------------------------------
  public static void bf_goto() {
    //GOTO label
    //  @_L#+@G-              LABEL[p]=1:G=0
    _needPost = true;
    parse();
    String o = pre() + label(_p) + "+@_G-" + post();
    if (_debug) { o = "\n(GOTO " + _p +")\n" + o; }
    writeTemp(o); //arrows(o);
    _needPre = true;
  }

  //------------------------------------------------------------------
  // bf_if -- writes out BF code for the IF statement
  //------------------------------------------------------------------
  public static void bf_if() {
    _sourceLine = (_a + _sourceLine).trim();
    int n = findexpr("THEN", 0);
    if (n == -1) { errout("IF without THEN"); }
    String expr = (_sourceLine.substring(0, n)).trim();
    _sourceLine = (_sourceLine.substring(n)).trim();
    parse();
    parse();
    _p = _p.toUpperCase();
    String o = "";
    if (_p.equals("GOTO")) {
      //IF expr THEN GOTO label
      //  @TEMP0[                 IF TEMP0<>0 THEN
      //  (...)                     LABEL#=1:G=0
      //  @TEMP0[-]]              END IF:TEMP0=0
      parse();
      if (_debug) { o = "\n(IF " + debugtext(expr) + " THEN GOTO " + _p + ")\n"; }
    } else if (_p.equals("EXIT")) {
      // IF expr THEN EXIT
      parse();
      _p = _p.toUpperCase();
      if (_p.equals("DO")) {
        // IF expr THEN EXIT DO
        if (_doStack.empty()) { errout("EXIT DO without DO"); }
        Integer l = (Integer) _doStack.pop();
        _p = "_D" + (Math.abs(l.intValue()) + 1);
        if (l.intValue() > 0) { l = new Integer(-l.intValue()); }
        _doStack.push(l);
        if (_debug) {
          o = "\n(IF " + debugtext(expr) + " THEN {EXIT DO}(GOTO _D"
              + (Math.abs(l.intValue()) + 1) + "))\n";
        }
      } else if (_p.equals("FOR")) {
        // IF expr THEN EXIT FOR
        if (_forStack.empty()) { errout("EXIT FOR without FOR"); }
        Integer l = (Integer) _forStack.pop();
        _p = "_F" + (Math.abs(l.intValue()) + 1);
        if (l.intValue() > 0) { l = new Integer(-l.intValue()); }
        _forStack.push(l);
        if (_debug) {
          o = "\n(IF " + debugtext(expr) + " THEN {EXIT FOR}(GOTO _F"
              + (Math.abs(l.intValue()) + 1) + "))\n";
        }
      } else {
        errout("Syntax error");
      }
    } else {
      // IF expr THEN
      if (_debug) { writeTemp("\n{IF " + debugtext(expr) + " THEN}\n"); }
      _sourceLine = "IF NOT(" + expr + ") THEN GOTO _I" + _ifAnnex + ":" + _sourceLine;
      _ifStack.push(new Integer(_ifAnnex++));
      return;
    }
    _needPost = true;
    AlgebraicExpression ae = new AlgebraicExpression(expr);
    o += pre() + ae.parse() + "@_T0[" + label(_p) + "+@_G-@_T0[-]]" + post();
    writeTemp(o); //arrows(o);
    _needPre = true;
  }

  //------------------------------------------------------------------
  // bf_input -- writes out BF code for the INPUT statement
  //------------------------------------------------------------------
  public static void bf_input() {
    parse();
    String o = "";
    if (_a == '(') {
      //INPUT V1(expr)
      //  (...)                                 TEMP0=expr
      //  @V1>>[-]<<@TEMP0[@V1>>+<<@TEMP0-]     E=B:TEMP0=0

      //  @T0+                                  T0=1
      //  [                                     DO WHILE T0<>0
      //    -                                     T0=0
      //    @T1,.                                 T1=ASC(INPUT$(1,1)):PRINT CHR$(T1);
      //    -------------                         T1=T1-13 or 10
      //    [                                     IF T1<>0 THEN
      //      @T0+                                  T0=1

      //      @T2+++++[@T1-------@T2-]              T1=T1-35 or 38:T2=0

      //      @V1[@T2+@V1-]                         T2=TEMP0:TEMP0=0
      //      @T2[                                  DO WHILE T2<>0
      //        @V1++++++++++                         T3=10:TEMP0=TEMP0+T3:T3=0
      //      @T2-]                                 T2=T2-1:LOOP:T2=0
      //      @T1[@V1+@T1-]                         TEMP0=TEMP0+T1:T1=0
      //    ]                                     END IF
      //  @T0]                                  LOOP:T0=0

      //  ++++++++++.[-]                        T0=10:PRINT CHR$(T0); or not

      //  @V1>[-]<@TEMP0[@V1>+<@TEMP0-]         F=M:TEMP0=0

      //  @V1>                                  @F
      //  >[>>[-]<<-[>>+<<-]+>>]>[-]<<<[<<]>[>[>>]>+<<<[<<]>-]>-<
      //  <
      int n = findexpr("", 1);
      if (n == -1) { errout("Syntax error"); }
      String expr = (_sourceLine.substring(0, n)).trim();
      if (expr.length() < 1 || expr.charAt(expr.length() - 1) != ')') {
        errout("Mismatched parenthesis");
      }
      expr = (expr.substring(0, expr.length() - 1)).trim();
      _sourceLine = (_sourceLine.substring(n)).trim();
      AlgebraicExpression ae = new AlgebraicExpression(expr);
      o = pre() + ae.parse() + "@~" + _p + ">>[-]<<@_T0[@~" + _p + ">>+<<@_T0-]"
          + "@_0+[-@_1,.----------" + (_crlf?"---":"") + "[@_0+"
          + "@_2+++++" + (_crlf?"":"+") + "[@_1------" + (_crlf?"-":"") + "@_2-]"
          + "@_T0[@_2+@_T0-]@_2[@_T0++++++++++@_2-]@_1[@_T0+@_1-]]@_0]"
          + (_crlf?"++++++++++.[-]":"")
          + "@~" + _p + ">[-]<@_T0[@~" + _p + ">+<@_T0-]@~" + _p
          + ">>[>>[-]<<-[>>+<<-]+>>]>[-]<<<[<<]>[>[>>]>+<<<[<<]>-]>-<<" + post();
      if (_debug) {
        o = "\n(INPUT " + _p + "(" + debugtext(expr) + ")\n" + o;
      }
    } else {
      //INPUT V1
      //  @V1[-]@T0+                    V1=0:T0=1
      //  [                             DO WHILE T0<>0
      //    -                             T0=0
      //    @T1,.                         T1=ASC(INPUT$(1,1)):PRINT CHR$(T1);
      //    -------------                 T1=T1-13 or 10
      //    [                             IF T1<>0 THEN
      //      @T0+                          T0=1

      //      @T2+++++[@T1-------@T2-]      T1=T1-35 or 38:T2=0

      //      @V1[@T2+@V1-]                 T2=V1:V1=0
      //      @T2[                          DO WHILE T2<>0
      //        @V1++++++++++                 T3=10:V1=V1+T3:T3=0
      //      @T2-]                         T2=T2-1:LOOP:T2=0

      //      @T1[@V1+@T1-]                 V1=V1+T1:T1=0
      //    ]                             END IF
      //  @T0]                          LOOP:T0=0
      //  ++++++++++.[-]                PRINT CHR$(T0); or not
      if (_debug) { o = "\n(INPUT " + _p + ")\n"; }
      o += pre() + "@" + _p + "[-]@_0+[-@_1,.----------" + (_crlf?"---":"") + "[@_0+"
           + "@_2+++++" + (_crlf?"":"+") + "[@_1------" + (_crlf?"-":"") + "@_2-]"
           + "@" + _p + "[@_2+@" + _p + "-]@_2[@" + _p + "++++++++++@_2-]"
           + "@_1[@" + _p + "+@_1-]]@_0]" + (_crlf?"++++++++++.[-]":"") + post();
    }
    writeTemp(o); //arrows(o);
  }

  //------------------------------------------------------------------
  // bf_label -- writes out BF code for labels
  //------------------------------------------------------------------
  public static void bf_label() {
    //L#: label
    //  @LINE#[@G+@LINE#-]
    String o = "";
    if (_debug) {
      o = "\n(LABEL " + _p + ")\n";
      if (!_needPre) {
        _needPost = true;
        o += "\t" + post();
      }
      o += "\n    (code) \t";
    } else {
      if (!_needPre) {
        _needPost = true;
        o += post();
      }
    }
    o += label(_p) + "[@_G+" + label(_p) + "-]";
    if (_debug) { o += "\t"; }
    writeTemp(o); //arrows(o);
    _p = "";
    _needPre = true;
  }

  //------------------------------------------------------------------
  // bf_let -- parses the LET statement
  //------------------------------------------------------------------
  public static void bf_let() {
    // expression
    if (_a == '(') {
      bf_letarray();
    } else if (_a == '=') {
      bf_letvar();
    } else {
      errout("Syntax error");
    }
  }

  //------------------------------------------------------------------
  // bf_letarray -- writes out BF code for the array = expr statement
  //------------------------------------------------------------------
  public static void bf_letarray() {
    //[ V1 B A . . . . ]
    //V1(exprA)=exprB 
    //  (...)                                   TEMP0=expr
    //  @TEMP0[@A>>+<<@TEMP0-]                  A=TEMP0:TEMP0=0 (offset)
    //  (...)                                   TEMP0=expr2
    //  @TEMP0[@B>+<@TEMP0-]                    B=TEMP0:TEMP0=0 (value)
    //  @V1                                     start at V1 (0-stopper)
    //  >>[[>>]+[<<]>>-]+[>>]<[-]<[<<]>[>[>>]<+<[<<]>-]>[>>]<<[-<<]
    
    int n = findexpr("=", 1);
    if (n == -1) { errout("Syntax error"); }
    String expr = (_sourceLine.substring(0, n)).trim();
    if (expr.length() < 1 || expr.charAt(expr.length() - 1) != ')') {
      errout("Mismatched parenthesis");
    }
    expr = (expr.substring(0, expr.length() - 1)).trim();
    _sourceLine = (_sourceLine.substring(n)).trim();
    String p0 = _p;
    parse();
    AlgebraicExpression ae = new AlgebraicExpression(expr);
    String o = pre() + ae.parse() + "@_T0[@~" + p0 + ">>+<<@_T0-]";
    n = findexpr("", 0);
    if (n == -1) { errout("Syntax error"); }
    String expr2 = (_sourceLine.substring(0, n)).trim();
    _sourceLine = (_sourceLine.substring(n)).trim();
    ae = new AlgebraicExpression(expr2);
    o += ae.parse() + "@_T0[@~" + p0 + ">+<@_T0-]@~" + p0
         + ">>[[>>]+[<<]>>-]+[>>]<[-]<[<<]>[>[>>]<+<[<<]>-]>[>>]<<[-<<]" 
         + post();
    if (_debug) {
      o = "\n(" + p0 + "(" + debugtext(expr) + ")=" + debugtext(expr2) + ")\n" + o;
    }
    writeTemp(o); //arrows(o);
  }

  //------------------------------------------------------------------
  // bf_letvar -- writes out BF code for the var = expr statement
  //------------------------------------------------------------------
  public static void bf_letvar() {
    //V1=expr
    //  (...)                   TEMP0=expr
    //  @V1[-]                  V1=0
    //  @TEMP0[@V1+@TEMP0-]     V1=TEMP0:TEMP0=0
    int n = findexpr("", 0);
    if (n == -1) { errout("Syntax error"); }
    String expr = (_sourceLine.substring(0, n)).trim();
    _sourceLine = (_sourceLine.substring(n)).trim();
    AlgebraicExpression ae = new AlgebraicExpression(expr);
    String parsing = ae.parse();

    StringBuffer out = new StringBuffer("");
    if (_optLevel >= 3) {
      // if this is a simple assign (the variable being assigned to 
      // isn't in the expression), then we can replace @_T0 with that
      // variable.
      int l2;
      char c;
      String varname = "";
      for (int l = 0; l < parsing.length(); l++) {
        // get character
        c = parsing.charAt(l);
        
        // variable
        if (c == '@') {
          // find end of variable name
          for (l2 = l + 1; l2 < parsing.length(); l2++) {
            if (("+-<>[].,".indexOf(parsing.charAt(l2))) != -1) {
              // get variable name (middle of string)
              varname = parsing.substring(l + 1, l2);
              break;
            } else if (l2 == parsing.length() - 1) {
              // get variable name (last thing in string)
              varname = parsing.substring(l + 1);
              l++;
            }
          }
          varname = varname.toUpperCase();
          l = l2 - 1;
          if (varname.equals(_p)) {
            out = null;
            break;
          }  
          if (varname.equals("_T0")) {
            varname = _p;
          }
          out.append("@" + varname); 
        } else {
          out.append(c + ""); 
        }  
      } 
    }    
    String o;
    if (out == null || _optLevel < 3) {
      o = pre() + parsing + "@" + _p + "[-]@_T0[@" + _p + "+@_T0-]" + post();
    } else {
      o = pre() + out.toString() + post();
    }  
    if (_debug) { o = "\n(" + _p + "=" + debugtext(expr) + ")\n" + o; }
    writeTemp(o); //arrows(o);
  }

  //------------------------------------------------------------------
  // bf_locate -- builds BFBASIC code for the LOCATE statement
  //------------------------------------------------------------------
  public static void bf_locate() {
    _sourceLine = (_a + _sourceLine).trim();
    int n = findexpr(",", 0);
    if (n == -1) { errout("Syntax error"); }
    String expr = (_sourceLine.substring(0, n)).trim();
    _sourceLine = (_sourceLine.substring(n)).trim();
    parse();
    n = findexpr("", 0);
    if (n == -1) { errout("Syntax error"); }
    String expr2 = (_sourceLine.substring(0, n)).trim();
    _sourceLine = "PRINT \"\033[\"; " + expr + "; \";\"; "
                 + expr2 + "; \"H\";:" + (_sourceLine.substring(n)).trim();
    if (_debug) {
      writeTemp("\n{LOCATE " + debugtext(expr + "," + expr2) + "}\n");
    }
  }

  //------------------------------------------------------------------
  // bf_loop -- builds BFBASIC code for the LOOP statement
  //------------------------------------------------------------------
  public static void bf_loop() {
    if (_doStack.empty()) { errout("LOOP without DO"); }
    Integer l;
    if (parseif()) {
      // no params
      // LOOP
      if (_debug) { writeTemp("\n{LOOP}\n"); }
      l = (Integer) _doStack.pop();
      String temp = "GOTO _D" + Math.abs(l.intValue()) + ":";
      if (l.intValue() < 0) { temp += "_D" + (-l.intValue() + 1) + ":"; }
      _sourceLine = temp + _sourceLine;
    } else {
      // params
      parse();
      _p = _p.toUpperCase();
      if (_p.equals("WHILE")) {
        // LOOP WHILE expr
        _sourceLine = (_a + _sourceLine).trim();
        int n = findexpr("", 0);
        if (n == -1) { errout("Syntax error"); }
        String expr = (_sourceLine.substring(0, n)).trim();
        _sourceLine = (_sourceLine.substring(n)).trim();
        if (_debug) { writeTemp("\n{LOOP WHILE " + debugtext(expr) + "}\n"); }
        l = (Integer) _doStack.pop();
        String temp = "IF " + expr + " THEN GOTO _D" + Math.abs(l.intValue()) + ":";
        if (l.intValue() < 0) { temp += "_D" + (-l.intValue() + 1) + ":"; }
        _sourceLine = temp + _sourceLine;
      } else if (_p.equals("UNTIL")) {
        // LOOP UNTIL expr
        _sourceLine = (_a + _sourceLine).trim();
        int n = findexpr("", 0);
        if (n == -1) { errout("Syntax error"); }
        String expr = (_sourceLine.substring(0, n)).trim();
        _sourceLine = (_sourceLine.substring(n)).trim();
        if (_debug) { writeTemp("\n{LOOP UNTIL " + debugtext(expr) + "}\n"); }
        l = (Integer) _doStack.pop();
        String temp = "IF NOT(" + expr + ") THEN GOTO _D" + Math.abs(l.intValue()) + ":";
        if (l.intValue() < 0) { temp += "_D" + (-l.intValue() + 1) + ":"; }
        _sourceLine = temp + _sourceLine;
      } else {
        errout("Syntax error");
      }
    }
  }

  //------------------------------------------------------------------
  // bf_next -- builds BFBASIC code for the NEXT statement
  //------------------------------------------------------------------
  public static void bf_next() {
    // NEXT expr
    int n = findexpr("", 0);
    if (n == -1) { errout("Syntax error"); }
    String expr = (_sourceLine.substring(0, n)).trim();
    Integer l = (Integer) _forStack.pop();
    String m = (String) _forTopStack.pop();
    // if m is a number in the range 0-255 then {                                                     
    // String temp = expr + "=" + expr + "+1:IF NOT(" + expr + "="
    //              + ((m+1)&255) + ") THEN GOTO _F" + Math.abs(l.intValue()) + ":";
    // } else { // m is not a number
    String temp = expr + "=" + expr + "+1:IF NOT(" + expr + "="
                 + m + "+1) THEN GOTO _F" + Math.abs(l.intValue()) + ":";
    // } 
    if (l.intValue() < 0) { temp += "_F" + (-l.intValue() + 1) + ":"; }
    _sourceLine = temp + (_sourceLine.substring(n)).trim();
    if (_debug) { writeTemp("\n{NEXT " + debugtext(expr) + "}\n"); }
    // writeTemp(o);
  }

  //------------------------------------------------------------------
  // bf_print -- writes out BF code for the PRINT statement
  //------------------------------------------------------------------
  public static void bf_print() {
    if (parseif()) {
      // no params
      //PRINT
      //  (...)                 PRINT
      String o = pre() + newline() + post();
      if (_debug) { o = "\n(PRINT)\n" + o; }
      writeTemp(o); //arrows(o);
    } else {
      // params
      boolean sameline;
      // parse print expression list
      String o = "";
      while (!parseif()) {
        sameline = false;
        //System.out.println("p["+p+"] a["+a+"] sl["+sourceline+"]");

        if (_sourceLine.length() > 0 && _sourceLine.charAt(0) == '\"') {
          // string
          parse();
          _p = _p.substring(1);
          int n;
          if ((n = _p.indexOf('\"')) == -1) { errout("Mismatched quotation mark"); }
          if (_a == ';') {
            sameline = true;
            _a = ' ';
          }
          _p = _p.substring(0, n);
          if (_p.length() == 0) {
            // empty string
            //PRINT
            //  (...)             PRINT
            if (!sameline) {
              if (_debug) { o += "\n(PRINT \"\")\n"; }
              o += newline();
            }
          } else {
            // string literal
            //PRINT "text"
            //  @T0               (...)
            //  (...)             PRINT "text"
            if (_debug) {
              o += "\n(PRINT \"" + debugtext(_p) + "\"";
              if (sameline) { o += ";"; }
              o += ")\n";
            }
            o += "@_0" + bf_text(_p);
            if (!sameline) { o += newline(); }
          }
        } else if (_sourceLine.length() >= 1 && _sourceLine.charAt(0) == ';') {
          // PRINT ;
          _sourceLine = _sourceLine.substring(1);
        } else if ((_sourceLine.length() >= 4)
                   && (_sourceLine.substring(0, 4)).equalsIgnoreCase("CHR$")) {
          // PRINT CHR$(expr)
          //   (...)              TEMP0=expr
          //   @TEMP0.            PRINT CHR$(TEMP0);
          parse();
          if (_a != '(') { errout("Syntax error"); }
          int n = findexpr("", 1);
          if (n == -1) { errout("Syntax error"); }
          String expr = (_sourceLine.substring(0, n)).trim();
          if (expr.length() < 1 || expr.charAt(expr.length() - 1) != ')') {
            errout("Mismatched parenthesis");
          }
          expr = (expr.substring(0, expr.length() - 1)).trim();
          _sourceLine = (_sourceLine.substring(n)).trim();
          if (_sourceLine.length() > 0 && _sourceLine.charAt(0) == ';') {
            sameline = true;
            _sourceLine = _sourceLine.substring(1);
          }
          AlgebraicExpression ae = new AlgebraicExpression(expr);
          if (_debug) {
            o += "\n(PRINT CHR$(" + debugtext(expr) + ")";
            if (sameline) { o += ";"; }
            o += ")\n";
          }
          o += ae.parse() + "@_T0.";
          if (!sameline) { o += newline(); }
        } else if ((_sourceLine.length() >= 6)
                   && (_sourceLine.substring(0, 6)).equalsIgnoreCase("SPACE$")) {
          // PRINT SPACE$(expr)
          //   (@T0[-])           (T0=0)
          //   (@T1[-])           (T1=0)
          //   (...)              TEMP0=expr
          //   @T1++++[           T1=4:DO WHILE T1<>0
          //     @T0++++++++        T0=T0+8
          //   @T1-]              T1=T1-1:LOOP
          //   @TEMP0[            DO WHILE TEMP0<>0
          //     @T0.               PRINT " ";
          //   @TEMP0-]           TEMP0=TEMP0-1:LOOP
          //   @T0[-]             T0=0
          parse();
          if (_a != '(') { errout("Syntax error"); }
          int n = findexpr("", 1);
          if (n == -1) { errout("Syntax error"); }
          String expr = (_sourceLine.substring(0, n)).trim();
          if (expr.length() < 1 || expr.charAt(expr.length() - 1) != ')') {
            errout("Mismatched parenthesis");
          }
          expr = (expr.substring(0, expr.length() - 1)).trim();
          _sourceLine = (_sourceLine.substring(n)).trim();
          if (_sourceLine.length() > 0 && _sourceLine.charAt(0) == ';') {
            sameline = true;
            _sourceLine = _sourceLine.substring(1);
          }
          AlgebraicExpression ae = new AlgebraicExpression(expr);
          if (_debug) {
            o += "\n(PRINT SPACE$(" + debugtext(expr) + ")";
            if (sameline) { o += ";"; }
            o += ")\n";
          }
          o += ae.parse() + "@_1++++[@_0++++++++@_1-]@_T0[@_0.@_T0-]@_0[-]";
          if (!sameline) { o += newline(); }
        } else if ((_sourceLine.length() >= 7)
                   && (_sourceLine.substring(0, 7)).equalsIgnoreCase("STRING$")) {
          // PRINT STRING$(expr1, expr2)
          //   (...)               TEMP0=expr
          ///  @TEMP0[@ST+@TEMP0-] ST=TEMP0
          //   @ST[                DO WHILE ST<>0
          //     @TEMP0.             PRINT CHR$(TEMP0);
          //   @ST-]               ST=ST-1:LOOP
          parse();
          if (_a != '(') { errout("Syntax error"); }
          int n = findexpr(",", 0);
          if (n == -1) {
            errout("Syntax error");
          }
          String expr = (_sourceLine.substring(0, n)).trim();
          _sourceLine = (_sourceLine.substring(n + 1)).trim();
          n = findexpr("", 1);
          if (n == -1) { errout("Syntax error"); }
          String expr2 = (_sourceLine.substring(0, n)).trim();
          if (expr2.length() < 1 || expr2.charAt(expr2.length() - 1) != ')') {
            errout("Mismatched parenthesis");
          }
          expr2 = (expr2.substring(0, expr2.length() - 1)).trim();
          _sourceLine = (_sourceLine.substring(n)).trim();
          if (_sourceLine.length() > 0 && _sourceLine.charAt(0) == ';') {
            sameline = true;
            _sourceLine = _sourceLine.substring(1);
          }
          if (_debug) {
            o += "\n(PRINT STRING$(" + debugtext(expr + "," + expr2) + ")";
            if (sameline) { o += ";"; }
            o += ")\n";
          }
          AlgebraicExpression ae = new AlgebraicExpression(expr);
          o += ae.parse() + "@_T0[@_ST+@_T0-]";
          ae = new AlgebraicExpression(expr2);
          o += ae.parse() + "@_ST[@_T0.@_ST-]";
          if (!sameline) { o += newline(); }
        } else {
          // PRINT expr
          //   (...)              TEMP0=expr
          //   (...)              PRINT TEMP0;
          _sourceLine = (_a + _sourceLine).trim();
          int n = findexpr("", 0);
          if (n == -1) { errout("Syntax error"); }
          String expr = (_sourceLine.substring(0, n)).trim();
          _sourceLine = (_sourceLine.substring(n)).trim();
          if (_sourceLine.length() > 0 && _sourceLine.charAt(0) == ';') {
            sameline = true;
            _sourceLine = _sourceLine.substring(1);
          }
          AlgebraicExpression ae = new AlgebraicExpression(expr);
          if (_debug) {
            o += "\n(PRINT " + debugtext(expr);
            if (sameline) { o += ";"; }
            o += ")\n";
          }
          o += ae.parse() + itoa();
          if (!sameline) { o += newline(); }
        }
        _sourceLine = _sourceLine.trim();
      }
      if (!o.equals("")) {
        o = pre() + o;
        if (_debug) {
          o = "\n{PRINT}\n" + o;
        }
        o += post();
        writeTemp(o); //arrows(o);
      }
    }
  }

  //------------------------------------------------------------------
  // bf_randomize -- writes out BF code for the RANDOMIZE statement
  //------------------------------------------------------------------
  public static void bf_randomize() {
    parse();
    if (_p.equalsIgnoreCase("KEY")) {
      //RANDOMIZE KEY
      //  @T0(...)                      PRINT "Random seed?"
      //  +                             T0=1
      //  [                             DO WHILE T0<>0
      //    @T0-                          T0=0
      //    @T1,-------------             T1=INKEY:T1=T1-13 or 10
      //    [                             DO WHILE T1<>0
      //      @T0+                          T0=1
      //      @T1[@RH+@T1-]                 RH=RH+T1:T1=0
      //      @RH[@RL+@T1+@RH-]             RL=RL+RH:T1=RH:RH=0
      //      @T1[@RH+@T1-]                 RH=T1:T1=0
      //    ]                             LOOP
      //  @T0]                          LOOP
      String o = pre() + "@_0" + bf_text("Random seed?")
                 +"+[@_0-@_1,----------" + (_crlf?"---":"") + "[@_0+@_1[@_RH+@_1-]"
                 +"@_RH[@_RL+@_1+@_RH-]@_1[@_RH+@_1-]]@_0]" + post();
      if (_debug) { o = "\n(RANDOMIZE KEY)\n" + o; }
      writeTemp(o); //arrows(o);
    } else {
      errout("Syntax error");
    }
  }

  //------------------------------------------------------------------
  // bf_rem -- handles the REM statement
  //------------------------------------------------------------------
  public static void bf_rem() {
    // REM comment
    if (_debug == true) {
      writeTemp("\n{REM}\n" + debugtext(_sourceLine) + "\n");
    }
    _sourceLine = "";
  }

  //------------------------------------------------------------------
  // bf_return -- builds BFBASIC code for the RETURN statement
  //------------------------------------------------------------------
  public static void bf_return() {
    //RETURN
    //  @_L#+@G-              LABEL[p]=1:G=0
    _needPost = true;
    String o = pre() + "@_GP-" + label("_RETURN") + "+@_G-" + post();
    if (_debug) { o = "\n(RETURN)\n" + o; }
    writeTemp(o); //arrows(o);
    _needPre = true;
  }

  //------------------------------------------------------------------
  // bf_stop -- writes out BF code for the STOP statement
  //------------------------------------------------------------------
  public static void bf_stop() {
    //STOP
    //  (...)                   builds PRINT "Stopped":END:
    if (_debug) { writeTemp("\n{STOP}\n"); }
    _sourceLine = "PRINT \"Stopped\":END:" + _sourceLine;
  }

  //------------------------------------------------------------------
  // bf_swap -- writes out BF code for the SWAP statement
  //------------------------------------------------------------------
  public static void bf_swap() {
    //SWAP V1,V2
    //  (@T0[-])                (T0=0)
    //  @V1[@T0+@V1-]           T0=V1:V1=0
    //  @V2[@V1+@V2-]           V1=V2:V2=0
    //  @T0[@V2+@T0-]           V2=T0:T0=0
    parse();
    String p0 = _p;
    if (_a != ',') { errout("Syntax error"); }
    parse();
    String o = pre() + "@" + p0 + "[@_0+@" + p0 + "-]@" + _p
               + "[@" + p0 + "+@" + _p + "-]@_0[@" + _p + "+@_0-]" + post();
    if (_debug) { o = "\n(SWAP " + p0 + ", " + _p + ")\n" + o; }
    writeTemp(o); //arrows(o);
  }

  //------------------------------------------------------------------
  // bf_system -- writes out BF code for the SYSTEM statement
  //------------------------------------------------------------------
  public static void bf_system() {
    //SYSTEM
    //  @G-                   G=0 (no more basic statements execute)
    //  @Q-                   Q=0 (quit)
    _needPost = true;
    _sourceLine = (_p + _a + _sourceLine).trim();
    String o = pre() + "@_G-@_Q-" + post();
    if (_debug) { o = "\n(SYSTEM)\n" + o; }
    writeTemp(o); //arrows(o);
    _needPre = true;
  }

  //------------------------------------------------------------------
  // bf_text -- convert ascii string to BF program
  // Programmed by Jeffry Johnston, July 25, 2001
  //------------------------------------------------------------------
  public static String bf_text(String ascii) {
    char[] firstch = new char[7];      // first character used in each range
    int[] f = new int[7];
    int[] value = new int[7];          // current value of each cell
    int[] low = { 15, 27, 43, 65, 78, 94, 110 };
    int[] high = { 26, 42, 64, 77, 93, 109, 126 };
    int ranges = 0;                    // number of text ranges used
    String code = "";                  // for bf code

    // determine which characters start in each range
    boolean[] used = new boolean[7]; // if each range is used
    for(int n = 0; n < ascii.length(); n++) {
      char b = ascii.charAt(n);
      if (b < 15 || b > 126) { errout("Invalid ascii character: " + (int) b); }
      for (int c = 0; c <= 6; c++) {
        // figure out if range already init'd and if char is in range
        if (!used[c] && b >= low[c] && b <= high[c]) {
          used[c] = true;      // mark range as used
          firstch[ranges] = b; // save 1st char used in this range
          ranges++;            // count number of used ranges
        }
      }
    }
    ranges--;

    // find the best multiplicands
    int best = 1000, mult = 0, sample, g;
    for (int n = 1; n <= 50; n++) {
      g = n;
      sample = 0;
      for(int b = 0; b <= ranges; b++) {
        f[b] = Math.round((float) firstch[b] / (float) n);
        g += f[b];
        sample += Math.abs(firstch[b] - n * f[b]);
      }
      sample += g;
      if (sample < best) {
        best = sample;
        mult = n;
        for (int b = 0; b <= 6; b++) {
          value[b] = f[b];
        }
      }
    }

    // build the multiplication string
    // ++++++++++[>+++++++>++++++++++>+++>+<<<<-]>
    code += string(mult, '+') + "[";
    for (int c = 0; c <= ranges; c++) {
      code += ">" + string(value[c], '+');
      value[c] *= mult;
    }

    // position mp to get started
    int mp = 0, mpnew;
    code += string(ranges + 1, '<') + "-]>";

    // output the rest
    for(int n = 0; n < ascii.length(); n++) {
      char b = ascii.charAt(n);
      mpnew = 0;

      // determine the best variable to use (find closest value)
      best = 255; // worst possible match, will certainly beat this
      for (int c = 0; c <= ranges + 1; c++) {
        sample = Math.abs(value[c] - b);
        if (sample < best || (sample <= best && c == mp)) {
          mpnew = c;
          best = sample;
        }
      }

      // move to new memory cell as decided above
      if (mpnew < mp) { code += string(mp - mpnew, '<'); }
      if (mpnew > mp) { code += string(mpnew - mp, '>'); }
      mp = mpnew;

      // adjust cell value as decided above
      if (b < value[mp]) { code += string(value[mp] - b, '-'); }
      if (b > value[mp]) { code += string(b - value[mp], '+'); }
      value[mp] = b;
      code += ".";
    }

    // zero used cells and position mp back to where it started from
    code += string(ranges - mp, '>');
    for (int c = 0; c <= ranges; c++) {
      code += "[-]<";
    }
    return code;
  }

  //------------------------------------------------------------------
  // debugtext -- removes BF commands from debug text
  // On entry:
  //   String text      text
  // Returns:
  //   String           fixed text
  //------------------------------------------------------------------
  public static String debugtext(String text) {
    String debug = "";
    for (int d = 0; d < text.length(); d++) {
      switch (text.charAt(d)) {
      case '+':
        debug += "(plus)"; break;
      case '-':
        debug += "(minus)"; break;
      case ',':
        debug += "(comma)"; break;
      case '.':
        debug += "(dot)"; break;
      case '<':
        debug += "(lt)"; break;
      case '>':
        debug += "(gt)"; break;
      case '[':
        debug += "(lsb)"; break;
      case ']':
        debug += "(rsb)"; break;
      case '@':
        debug += "(at)"; break;
      case 27:
        debug += "(esc)"; break;
      case '#':
        debug += "(hash)"; break;
      default:
        debug += text.substring(d, d + 1); break;
      }
    }
    return debug;
  }

  //------------------------------------------------------------------
  // errout -- prints error message then terminates program execution
  //------------------------------------------------------------------
  public static void errout(String e) {
    System.out.println();
    System.out.print(e);
    if (_line > 0) { System.out.print(", line " + _line); }
    System.out.println(".");
    System.exit(1);
  }

  //------------------------------------------------------------------
  // findexpr -- find expression in sourceline
  // Returns:
  //   int              last character position of expression + 1
  //------------------------------------------------------------------
  public static int findexpr(String endexpr, int startdepth) {
    endexpr = endexpr.toUpperCase();
    boolean q = false;
    // : ' ; [end]
    // ""
    //
    int ee = -1, depth = startdepth;
    for (int n = 0; n < _sourceLine.length(); n++) {
      char c = _sourceLine.charAt(n);
      if (c == '\"') {
        q = !q;
      }
      if (!q) {
        if (c == '(') {
          depth++;
        } else if (c == ')') {
          depth--;
          if (depth < 0) { errout ("Mismatched parenthesis"); }
        }
        if (depth == 0) {
          if (endexpr.equals("")) {
            if (c == ':' || c == '\''|| c == ';') {
              ee = n;
              break;
            }
          } else if (n == (_sourceLine.toUpperCase()).indexOf(endexpr, n)) {
            ee = n;
            break;
          }
        }
      }
    }
    if (ee == -1 && endexpr.equals("")) { ee = _sourceLine.length(); }
    return ee;
  }

  //------------------------------------------------------------------
  // first -- output header code for BFBASIC program
  //------------------------------------------------------------------
  public static void first() {
    //(top of program)
    //  (@G[-]@Q[-])            (G=0:Q=0)
    //  @G+@Q+                  G=1:Q=1
    //  [                       DO WHILE Q<>0
    String o = "@_G+@_Q+[";
    if (_debug) { o = "\n(FIRST)\n    (code) " + o; }
    writeTemp(o); //arrows(o);
    _needPre = true;
  }

  //------------------------------------------------------------------
  // isnum -- decides whether current token is a number or not
  // Parameters:
  //   none
  // Returns:
  //   boolean          true=number, false=text
  //------------------------------------------------------------------
  public static boolean isnum() {
    boolean ans = false;
    if ((_p.length() >= 2 && (_p.substring(0, 2)).equalsIgnoreCase("&H")) ||
        (_p.length() > 0 && _p.charAt(0) >= '0' && _p.charAt(0) <= '9')) {
      ans = true;
    }
    return ans;
  }

  //------------------------------------------------------------------
  // itoa -- convert number at TEMP0 to ascii and display it
  // Returns:
  //  String            BF code
  //------------------------------------------------------------------
  public static String itoa() {
    //  (@T0[-])@T1[-]@T2[-]@T3[-]@T4[-]        HUN=0:TEN=0:ONE=0
    //  @TEMP0[                                 DO WHILE N<>0
    //    @T3+                                    ONE=ONE+1
    //    [@T4+@T0+@T3-]                          IF ONE=10 THEN
    //    @T0[@T3+@T0-]+

    //    @T4----------[
    //      @T0-
    //    @T4[-]]
    //    @T0[
    //      @T2+@T3[-]                              TEN=TEN+1:ONE=0
    //    @T0-]                                   END IF

    //    @T2[@T4+@T0+@T2-]                       IF TEN=10 THEN
    //    @T0[@T2+@T0-]+

    //    @T4----------[
    //      @T0-
    //    @T4[-]]
    //    @T0[
    //      @T1+@T2[-]                              HUN=HUN+1:TEN=0
    //    @T0-]                                   END IF

    //  @TEMP0-]                                N=N-1:LOOP
    //  @T1[                                    IF HUN<>0 THEN
    //    @T0++++++++[@T1++++++@T2++++++@T0-]     HUN=HUN+48:TEN=TEN+48

    //    @T1.@T2.[-]                             PRINT CHR$(HUN);CHR$(TEN);:TEN=0
    //  @T1[-]]                                 HUN=0:END IF
    //  @T2[                                    IF TEN<>0 THEN
    //    @T0++++++++[@T2++++++@T0-]              TEN=TEN+48

    //    @T2.                                    PRINT CHR$(TEN);
    //  [-]]                                    TEN=0:END IF
    //  @T0++++++++[@T3++++++@T0-]              ONE=ONE+48
    //  @T3.                                    PRINT CHR$(ONE);
    String o = "@_1[-]@_2[-]@_3[-]@_4[-]@_T0[@_3+[@_4+@_0+@_3-]@_0[@_3+@_0-]+"
               + "@_4----------[@_0-@_4[-]]@_0[@_2+@_3[-]@_0-]"
               + "@_2[@_4+@_0+@_2-]@_0[@_2+@_0-]+"
               + "@_4----------[@_0-@_4[-]]@_0[@_1+@_2[-]@_0-]"
               + "@_T0-]@_1[@_0++++++++[@_1++++++@_2++++++@_0-]"
               + "@_1.@_2.[-]@_1[-]]@_2[@_0++++++++[@_2++++++@_0-]"
               + "@_2.[-]]@_0++++++++[@_3++++++@_0-]@_3.[-]";
    return o;
  }

  //------------------------------------------------------------------
  // label -- Converts a user's label name to an internal @_LABEL name
  // Parameters:
  //  userLabel         line label to convert
  // Returns
  //  String            @ label name
  //------------------------------------------------------------------
  public static String label(String userLabel) {
    // validate label
    userLabel = (userLabel.toUpperCase()).trim();
    for (int n = 0; n < userLabel.length(); n++) {
      char c = userLabel.charAt(n);
      if (c != '_' && (c < 'A' || c > 'Z') && (c < '0' || c > '9')) {
        errout("Label \'" + _p + "\' contains the invalid " +
            "character \'" + c + "\'");
      }
      if (c >= '0' && c <= '9' && n == 0) {
        errout("Label cannot begin with a number");
      }
    }

    // find label and associated number
    int n = 0;
    if (_label.containsKey(userLabel)) {
      // entry already exists, look up line number
      n = ((Integer) _label.get(userLabel)).intValue();
    } else {
      // doesn't exist, create new entry
      _label.put(userLabel, new Integer(_annex));
      n = _annex++;
    }
    
    // add prefix and return the result
    return "@_L" + n;
  }
 
  //------------------------------------------------------------------
  // last -- trailing code for BFBASIC program
  //------------------------------------------------------------------
  public static void last() {
    //(bottom of program)
    //    (@Q-)                   (END (Q=0))
    //  @Q]                     LOOP
    _needPost = true;
    String o = pre() + "@_Q-" + post();
    if (_debug) { o = "\n(END)\n" + o; }
    if (_gosubAnnex > 0) {
      _needPre = true;
      _needPost = true;
      if (_debug) { o += "\n(VECTORS)\n    (code) "; }
      o += label("_RETURN") + "[@_G+" + label("_RETURN") + "-]";
      AlgebraicExpression ae = new AlgebraicExpression("_GS(_GP)");
      if (_debug) { o += "\n"; } 
      o += pre() + ae.parse() + "@_T1[-]";
      for (int n = 0; n < _gosubAnnex; n++) {
        if (_debug) { o += "\n(IF _T0=" + n + " THEN GOTO _G" + n + ")\n"; }
        o += "@_T2+@_T0[@_T1+@_T2[-]@_T0-]-@_T1[@_T0+@_T1-]@_T2[" + 
             label("_G" + n) + "+@_G-@_T2-]";
      }
      o += "@_T0[-]" + post();
    }
    
    if (_debug) { o += "\n(LAST)\n    (code) "; }
    o += "@_Q]";
    writeTemp(o); //arrows(o);
  }

  //------------------------------------------------------------------
  // newline -- returns BF code to print a newline
  //------------------------------------------------------------------
  public static String newline() {
    //PRINT (cr) (lf)
    //  @T0+++++++++++++.     T0=13:PRINT CHR$(T0);
    //  ---.                  T0=10:PRINT CHR$(T0);
    //  [-]                   T0=0

    //PRINT (lf)
    //  @T0++++++++++.          T0=10:PRINT CHR$(T0);
    //  [-]                     T0=0
    return "@_0++++++++++" + (_crlf?"+++":"") + "." + (_crlf?"---.":"") + "[-]";
  }

  //------------------------------------------------------------------
  // parse -- parses the next token (program statement/element)
  // Parameters:
  //   none
  // Changes:
  //   p                statement
  //   a                character that caused the parse to stop:
  //                    ":" end of statement ("" and "'" are given as ":")
  //                    "(" open parenthesis
  //                    "+" addition operator
  //                    (etc)
  //   sourceline       remaining line
  // Returns:
  //   void
  //------------------------------------------------------------------
  public static void parse() {
    _sourceLine = _sourceLine.trim();
    boolean q = false;
    int n;
    _p = "";
    for (n = 0; n < _sourceLine.length(); n++) {
      _a = _sourceLine.charAt(n);
      if (_a == '\"') {
        q = !q;
      } else if (": \'()=+-<>*/,.[];".indexOf(_a) != -1) {
        if (!q) { break; }
      }
    }
    _p = (_sourceLine.substring(0, n)).trim();
    if (n == _sourceLine.length() || _a == '\'') {
      // comment or end of line
      _sourceLine = "";
      _a = ':';
    } else if (n < _sourceLine.length()) {
      _sourceLine = (_sourceLine.substring(n)).trim();
    }
    // if V1 = ..., we want p="V1":a="=", not p="V1";a=" "
    if (_sourceLine.length() > 0 && "=()<+-*/,;".indexOf(_sourceLine.charAt(0)) != -1) {
      _a = _sourceLine.charAt(0);
      _sourceLine = (_sourceLine.substring(1)).trim();
    }
  }

  //------------------------------------------------------------------
  // parseif -- are we are at the end of the current statement?
  // On entry:
  //   sourceline       remaining line
  // Returns:
  //   boolean          true = yes, false = no
  //------------------------------------------------------------------
  public static boolean parseif() {
    boolean ans = true;
    if (_sourceLine.length() > 0 && _sourceLine.charAt(0) != '\''
        && _sourceLine.charAt(0) != ':') {
      ans = false;
    }
    return ans;
  }

  //------------------------------------------------------------------
  // pre -- code put before each block of commands, 
  //        except labels & DIM's
  // Returns:
  //  String            BF code
  //------------------------------------------------------------------
  public static String pre() {
    //  (@T[-])                 (T=0)
    //  @G[@T+@G-]              T=G:G=0
    //  @T[                     IF T<>0 THEN
    //    @G+                     G=1 
    String o = "";
    if (_needPre) {
      o = "@_G[@_T[-]+@_G-]@_T[@_G+";
      if (_debug) { o = "    (pre)  \t" + o + "\t\n    (code) \t"; }
      _needPre = false;
    } else if (_debug) {
      o = "    (code) \t";
    }
    return o;
  }

  //------------------------------------------------------------------
  // post -- code put after each command, except labels & DIM's
  // Returns:
  //  String            BF code
  //------------------------------------------------------------------
  public static String post() {
    //  @T-]                    END IF
    String o = "";
    if (_needPost) {
      if (_debug) { o = "\t\n    (post) "; }
      o += "@_T-]";
      _needPost = false;
    } else if (_debug) {
      o = "\t\n";
    }
    return o;
  }

  //------------------------------------------------------------------
  // string -- generate a string of identical characters
  //------------------------------------------------------------------
  public static String string(int len, char ch) {
    char[] c = new char[len];
    Arrays.fill(c, ch);
    return new String(c);
  }

  //------------------------------------------------------------------
  // usage -- prints usage information
  //------------------------------------------------------------------
  public static void usage() {
    System.out.println();
    System.out.println("Usage:");
    System.out.println("    bfbasic [-c] [-d | -D] [-O#] [-w [#]] FILE[.bas] [[-o] FILE] [-?]");
    System.out.println();
    System.out.println("Where: ");
    System.out.println("    -c           Treat newline as CRLF, default: LF");
    System.out.println("    -d           Debug output");
    System.out.println("    -D           Verbose debug output");
    System.out.println("    -Olevel      Optimization level, default: 2");
    System.out.println("    -w [column]  Wraps output at the given column, default: 72");
    System.out.println("    FILE         Input filename");
    System.out.println("    -o outfile   Specify output filename, default: FILE.bf");
    System.out.println("    -?           Display usage information");
  }

  //------------------------------------------------------------------
  // writeTemp
  //------------------------------------------------------------------
  static int _linePos = 1;
  public static void writeTemp(String text) {
    if (_debug) {
      for (int n = 0; n < text.length(); n++) {
        if (text.charAt(n) == '\t') {
          _insert = !_insert;
          continue;
        }
        if (text.charAt(n) == '\n') {
          if (!_insert && _linePos != 1) {
            _tempOut.println();
            _linePos = 1;
          } else if (_insert && _linePos != 12) {
            _tempOut.println();
            _tempOut.print("           ");
            _linePos = 12;
          }
          continue;
        }
        _tempOut.print(text.charAt(n));
        _linePos++;
      }
    } else {
      _tempOut.print(text);
    }
  }

  //------------------------------------------------------------------
  // write
  //------------------------------------------------------------------
  public static void write(String text) {
    int linePos = 1;
    if (_debug) {
      for (int n = 0; n < text.length(); n++) {
        if (text.charAt(n) == '\n') {
          _out.println();
          linePos = 1;
          continue;
        }
        if (linePos > _wrapWidth) {
          _out.println();
          _out.print("           ");
          linePos = 12;
        }
        _out.print(text.charAt(n));
        linePos++;
      }
    } else if (_wrapWidth > 0) { 
      for (int n = 0; n < text.length(); n++) {
        _out.print(text.charAt(n));
        linePos++;
        if (linePos > _wrapWidth) {
          _out.println();
          linePos = 1;
        }
      }
    } else {
      _out.print(text);
    }
  }

}
