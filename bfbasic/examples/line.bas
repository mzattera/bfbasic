'parse file for first line
'Copyright 2003 by Jeffry Johnston

DIM B
DIM TEST

DO
  B = INKEY
  IF B = 13 OR B = 10 THEN EXIT DO
  PRINT CHR$(B);
LOOP
