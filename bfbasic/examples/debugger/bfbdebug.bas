'=====================================================================
'BFBDEBUG -- BFBASIC Debugger
'Copyright (C) 2003 by Jeffry Johnston
'Version 0.20
'
'This program is free software; you can redistribute it and/or modify
'it under the terms of the GNU General Public License as published by
'the Free Software Foundation. See the file LICENSE for more details.
'=====================================================================
CONST VERSION$ = "0.20"

'Variable list
'-------------
'PROGRAM        BF Program
'MEMORY         Memory
'IP             Instruction Pointer
'IPVALUE        Value at Instruction Pointer, [IP]
'MP             Memory Pointer
'MPVALUE        Value at Memory Pointer, [MP]
'A              (Temp) Keystroke
'LEVEL          (Temp) Depth of square brackets
'QUIT           Exit flag

'---------------------------------------------------------------------
' PROGRAM
'---------------------------------------------------------------------
DEFINT A-Z
CONST FALSE = 0, TRUE = NOT FALSE
REDIM MEMORY(30000), TLINE(1000, 1)

'display copyright text
SCREEN , , 0, 0
PRINT "BF Basic Debugger, version " + VERSION$ + ".  Copyright 2003 by Jeffry Johnston."
PRINT

'process command line
F$ = COMMAND$
IF INSTR(F$, "-?") THEN GOSUB USAGE: END
IF F$ = "" THEN
  GOSUB USAGE
  PRINT : PRINT "Error: No input file given"
  END
END IF
IF INSTR(F$, ".") = 0 THEN F$ = F$ + ".BF"
PRINT "Input file: "; F$

'read program
OPEN F$ FOR BINARY AS #1
PROGRAM$ = INPUT$(LOF(1), 1) + " "
CLOSE #1

'find text lines
S = 0: TLINE(0, 0) = 1: T = 0: O = 0
FOR N = 1 TO LEN(PROGRAM$)
  C = ASC(MID$(PROGRAM$, N, 1))
  IF S = 1 AND C <> 13 AND C <> 10 THEN T = T + 1: TLINE(T, 0) = N: S = 0
  IF S = 0 THEN
    IF C = 13 OR C = 10 THEN S = 1: O = 0 ELSE O = O + 1: TLINE(T, 1) = O
  END IF
NEXT N
MAXLINE = T

'set up screen
SCREEN , , 1, 1: COLOR 7, 0: LOCATE , , 1, 0, 31: CLS
COLOR , 5: LOCATE 24, 1: PRINT " "; : COLOR , 0: ST$ = ""
MP = 0: IP = 1
GOSUB IPFIX: GOSUB IPINFO: CURLINE = IPLINE: GOSUB DRAWPROG: GOSUB STATUS

'main input loop
DO
  A$ = INKEY$
  SELECT CASE A$
  CASE CHR$(27), CHR$(17), CHR$(24), CHR$(0) + "-", CHR$(0) + CHR$(16) 'Exit
    EXIT DO
  CASE CHR$(0) + "H" 'Scroll up
    IF CURLINE > 0 THEN CURLINE = CURLINE - 1: GOSUB DRAWPROG
  CASE CHR$(0) + "P" 'Scroll down
    IF CURLINE < MAXLINE THEN CURLINE = CURLINE + 1: GOSUB DRAWPROG
  CASE CHR$(0) + ">" 'F4, show output screen
    SCREEN , , 0, 0
    DO WHILE INKEY$ = "": LOOP
    SCREEN , , 1, 1
  CASE CHR$(0) + "?" 'F5, step
    ONCE = TRUE: GOSUB EXECUTE: ONCE = FALSE
    GOSUB IPFIX: GOSUB IPINFO: CURLINE = IPLINE: GOSUB DRAWPROG: GOSUB STATUS
  CASE CHR$(0) + "X" 'Shift-F5, run
    SCREEN , , 0
    GOSUB EXECUTE
    PRINT : PRINT "Press any key to continue..."
    DO WHILE INKEY$ = "": LOOP
    SCREEN , , 1, 1
    GOSUB IPFIX: GOSUB IPINFO: CURLINE = IPLINE: GOSUB DRAWPROG: GOSUB STATUS
  CASE CHR$(0) + "l" 'Alt-F5, proceed past loop
    SCREEN , , 0
    PROCEED = TRUE: GOSUB EXECUTE: PROCEED = FALSE
    SCREEN , , 1, 1
    GOSUB IPFIX: GOSUB IPINFO: CURLINE = IPLINE: GOSUB DRAWPROG: GOSUB STATUS
  CASE CHR$(0) + "<" 'F2 - Reset
    REDIM MEMORY(30000): IP = 1: MP = 0: ST$ = ""
    GOSUB IPFIX: GOSUB IPINFO: CURLINE = IPLINE: GOSUB DRAWPROG: GOSUB STATUS
  CASE CHR$(0) + "=" 'F3 - Memory map
    GOSUB MEMMAP
  END SELECT
LOOP
SCREEN , , 0, 0
END

'---------------------------------------------------------------------
'EXECUTE -- runs all or part of the BF program
'On entry:
'ONCE=TRUE      Only execute one BF instruction
'PROCEED=TRUE   Execute instructions until past the next loop
'---------------------------------------------------------------------
EXECUTE:
  QUIT = FALSE: ST$ = ""
  DO
    IF IP < 1 OR IP > LEN(PROGRAM$) THEN
      ST$ = "err": IF IP < 1 THEN IP = 1 ELSE IP = LEN(PROGRAM$)
      EXIT DO
    END IF
    IPVALUE = ASC(MID$(PROGRAM$, IP, 1))
    MPVALUE = MEMORY(MP)
    SELECT CASE IPVALUE
    CASE 64 '@
      ST$ = "end"
      QUIT = TRUE
    CASE 60 '<
      MP = MP - 1: IP = IP + 1
      IF MP < 0 THEN
        ST$ = "err": MP = MP + 30000
        QUIT = TRUE
      END IF
      IF ONCE = TRUE THEN QUIT = TRUE
    CASE 62 '>
      MP = MP + 1: IP = IP + 1
      IF MP > 29999 THEN
        ST$ = "err": MP = MP - 30000
        QUIT = TRUE
      END IF
      IF ONCE = TRUE THEN QUIT = TRUE
    CASE 43 '+
      IF ONCE = TRUE AND MPVALUE = 255 THEN ST$ = "err"
      MEMORY(MP) = (MPVALUE + 1) MOD 256: IP = IP + 1
      IF ONCE = TRUE THEN QUIT = TRUE
    CASE 45 '-
      IF ONCE = TRUE AND MPVALUE = 0 THEN ST$ = "err"
      MEMORY(MP) = (MPVALUE + 255) MOD 256: IP = IP + 1
      IF ONCE = TRUE THEN QUIT = TRUE
    CASE 46 '.
      SCREEN , , 0
      PRINT CHR$(MPVALUE);
      IP = IP + 1
      IF ONCE = TRUE THEN SCREEN , , 1: QUIT = TRUE
    CASE 44 ',
      IF ONCE = TRUE THEN
        COLOR 31, 4: LOCATE 24, 4: PRINT "inp"; : LOCATE 24, 1
      END IF
A:  A$ = MID$(INKEY$, 1, 1): IF A$ = "" THEN GOTO A
      MEMORY(MP) = ASC(A$)
      IF ONCE = TRUE THEN
        COLOR 7, 3: LOCATE 24, 4: PRINT "   "; : COLOR 7, 0
        IF MEMORY(MP) > 31 THEN COLOR , 5: LOCATE 24, 1: PRINT A$; : COLOR , 0
        QUIT = TRUE
      END IF
      IP = IP + 1
    CASE 93 ']
      DIRECTION = -1: GOSUB MATCH
      IF ONCE = TRUE THEN QUIT = TRUE
    CASE 91 '[
      IF MPVALUE = 0 THEN
        DIRECTION = 1: GOSUB MATCH
        IF PROCEED = TRUE THEN QUIT = TRUE
      END IF
      IP = IP + 1
      IF ONCE = TRUE THEN QUIT = TRUE
    CASE ELSE
      IP = IP + 1
    END SELECT
  LOOP UNTIL QUIT = TRUE
  IF IP > LEN(PROGRAM$) THEN IP = LEN(PROGRAM$)
RETURN

'---------------------------------------------------------------------
' MATCH
'---------------------------------------------------------------------
MATCH:
  LEVEL = 1
  DO
    IP = IP + DIRECTION: IPVALUE = ASC(MID$(PROGRAM$, IP, 1))
    IF IPVALUE = 91 THEN LEVEL = LEVEL + DIRECTION
    IF IPVALUE = 93 THEN LEVEL = LEVEL - DIRECTION
  LOOP UNTIL LEVEL <= 0
RETURN

'---------------------------------------------------------------------
' MEMMAP -- displays memory map
'---------------------------------------------------------------------
MEMMAP:
  SCREEN , , 2, 2: CLS
  MPCURR = ((MP - 176) \ 16) * 16: IF MPCURR < 0 THEN MPCURR = 0
  LOCATE 25, 1: COLOR 14, 3: PRINT " F3=Code"; SPACE$(72); : COLOR 7, 0
  GOSUB MEMDRAW
  DO
    A$ = INKEY$
    SELECT CASE A$
    CASE CHR$(0) + "H" 'Scroll up one line
      IF MPCURR >= 16 THEN MPCURR = MPCURR - 16: GOSUB MEMDRAW
    CASE CHR$(0) + "I" 'Scroll up one page
      IF MPCURR >= 384 THEN MPCURR = MPCURR - 16 ELSE MPCURR = 0
      GOSUB MEMDRAW
    CASE CHR$(0) + "P" 'Scroll down one line
      IF MPCURR <= 29999 - 367 THEN MPCURR = MPCURR + 16: GOSUB MEMDRAW
    CASE CHR$(0) + "Q" 'Scroll down one page
      IF MPCURR <= 29999 - 767 THEN MPCURR = MPCURR + 384 ELSE MPCURR = 29999 - 383
      GOSUB MEMDRAW
    CASE CHR$(27), CHR$(0) + "=" 'F3 - Exit memory map
      EXIT DO
    END SELECT
  LOOP
  SCREEN , , 1, 1
RETURN

'---------------------------------------------------------------------
' MEMDRAW
'---------------------------------------------------------------------
MEMDRAW:
  MPT = MPCURR
  LOCATE 1, 1
  FOR N = 1 TO 24
    PRINT SPACE$(79); : LOCATE , 1
    IF MPT < 30000 THEN PRINT LTRIM$(STR$(MPT));
    LOCATE , 6: PRINT CHR$(179);
    FOR B = 0 TO 15
      IF MPT + B <= 29999 THEN
        B$ = HEX$(MEMORY(MPT + B)): B$ = STRING$(2 - LEN(B$), "0") + B$
        IF MPT + B = MP THEN COLOR , 1
        PRINT B$; " ";
        COLOR , 0
      ELSE
        PRINT "   ";
      END IF
    NEXT B
    PRINT CHR$(179);
    FOR B = 0 TO 15
      IF MPT + B <= 29999 THEN
        IF MPT + B = MP THEN COLOR , 1
        IF MEMORY(MPT + B) < 32 THEN PRINT ".";  ELSE PRINT CHR$(MEMORY(MPT + B));
        COLOR , 0
      ELSE
        PRINT " ";
      END IF
    NEXT B: MPT = MPT + B
    IF N <> 24 THEN PRINT
  NEXT N
  LOCATE 1, 1
RETURN

'---------------------------------------------------------------------
' IPFIX
'---------------------------------------------------------------------
IPFIX:
  DO WHILE INSTR("+-<>.,[]@", MID$(PROGRAM$, IP, 1)) = 0 AND IP < LEN(PROGRAM$)
    IP = IP + 1
  LOOP
RETURN

'---------------------------------------------------------------------
' IPINFO -- return line and character position of IP
'---------------------------------------------------------------------
IPINFO:
  FOR N = 0 TO MAXLINE
    IF N = MAXLINE THEN EXIT FOR
    IF TLINE(N + 1, 0) > IP THEN EXIT FOR
  NEXT N
  IPLINE = N
  IPCOL = IP - TLINE(IPLINE, 0) + 1
RETURN

'---------------------------------------------------------------------
'DRAWPROG -- show program in window
'---------------------------------------------------------------------
DRAWPROG:
  'decide how many lines to place before and after current line
  IF CURLINE <= 11 THEN
    V0 = 0: V1 = 22: IF V1 > MAXLINE THEN V1 = MAXLINE
  ELSEIF MAXLINE - CURLINE <= 11 THEN
    V1 = MAXLINE: V0 = V1 - 22: IF V0 < 0 THEN V0 = 0
  ELSE
    V0 = CURLINE - 11: IF V0 < 0 THEN V0 = 0
    V1 = CURLINE + 11: IF V1 > MAXLINE THEN V1 = MAXLINE
  END IF

  'display the lines, except IP line
  LOCATE 1, 1: HILINE = 0
  FOR L = V0 TO V1
    IF L = CURLINE THEN CLINE = CSRLIN
    IF L = IPLINE THEN
      HILINE = CSRLIN: PRINT
    ELSE
      PRINT MID$(PROGRAM$, TLINE(L, 0), TLINE(L, 1));
      PRINT SPACE$(80 - TLINE(L, 1))
    END IF
  NEXT L

  'display IP line, if visible
  IF HILINE > 0 THEN
    COLOR , 1: LOCATE HILINE, 1
    PRINT MID$(PROGRAM$, TLINE(IPLINE, 0), IPCOL - 1);
    COLOR 15, 4
    PRINT MID$(PROGRAM$, TLINE(IPLINE, 0) + IPCOL - 1, 1);
    COLOR 7, 1
    PRINT MID$(PROGRAM$, TLINE(IPLINE, 0) + IPCOL, TLINE(IPLINE, 1) - IPCOL);
    PRINT SPACE$(80 - TLINE(IPLINE, 1))
    COLOR , 0
  END IF
  LOCATE CLINE, 1
RETURN

'---------------------------------------------------------------------
' STATUS -- redraw status bar
'---------------------------------------------------------------------
STATUS:
  LOCATE 24, 2: COLOR 8, 3: PRINT CHR$(196); "[";
  IF ST$ = "" THEN
    PRINT "   ";
  ELSE
    COLOR 31, 4: PRINT ST$;
  END IF
  COLOR 8, 3: PRINT "]"; CHR$(196);
  PRINT "[I:    G:    T:    0:    1:    2:    3:    4:    5:    6:    MP:     ]";
  PRINT STRING$(2, 196);
  FOR M = 0 TO 9
    LOCATE 24, M * 6 + 12
    PRINT LTRIM$(STR$(MEMORY(M)));
  NEXT M
  LOCATE 24, 73
  IF MP < 10 THEN
    PRINT "_"; MID$("IGT0123456", MP + 1, 1);
  ELSE
    PRINT LTRIM$(STR$(MP));
  END IF
  LOCATE 25, 1: COLOR 14
  PRINT " Esc=Exit  F2=Reset  F3=Memory  F4=Output  F5=Step  Alt-F5=Loop pass  Sh-F5=Run ";
  LOCATE HILINE, 1
  COLOR 7, 0
RETURN

'---------------------------------------------------------------------
' USAGE -- print usage information
'---------------------------------------------------------------------
USAGE:
  PRINT "Usage: BFBDEBUG file[.bf] [-?]"
  PRINT "Where: file     Input filename"
  PRINT "       -?       Display usage information"
RETURN

