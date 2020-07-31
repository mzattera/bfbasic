'Copyright 2003 by Jeffry Johnston

N = 0
PRINT "Type 5 letters: ";
FOR N = 0 TO 4
  CH(N)=INKEY
  PRINT CHR$(CH(N));
NEXT N
PRINT
PRINT "Reverse: ";
DO
  N = N - 1
  PRINT CHR$(CH(N));
LOOP WHILE N
