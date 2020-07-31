'Draw -- Copyright 2003 by Jeffry Johnston
'move           2468
'pen up/down    space
'exit           esc
'color +/-      +-
'erase on/off   0
'new            backspace

CLS
Y = 12: X = 40: PEN = 0: C = 37: D = 219
DO
  LOCATE Y, X
  IF PEN = 1 THEN 
    COLOR C
    PRINT CHR$(D);
    LOCATE Y, X
  END IF
  A = INKEY                       
  IF A = 56 AND Y <> 1 THEN     'up
    Y = Y - 1
  END IF
  IF A = 50 AND Y <> 24 THEN    'down
    Y = Y + 1
  END IF
  IF A = 52 AND X <> 1 THEN     'left
    X = X - 1 
  END IF
  IF A = 54 AND X <> 80 THEN    'right
    X = X + 1
  END IF
  IF A = 32 THEN                'pen up/down
    PEN = 1 - PEN
  END IF
  IF A = 43 AND C <> 37 THEN    'color +
    C = C + 1
  END IF
  IF A = 45 AND C <> 31 THEN    'color -
    C = C - 1
  END IF
  IF A = 48 THEN                'erase mode on/off
    D = 251 - D
  END IF
  IF A = 8 THEN                 'new
    CLS: PEN = 0
  END IF
LOOP UNTIL A = 27               'exit
COLOR 0

