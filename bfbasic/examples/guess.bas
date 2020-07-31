'test
RANDOMIZE KEY
CLS
PRINT "Guess the number"
PRINT
DO
  N=RND
LOOP WHILE N>99
N=N+1
PRINT "I'm thinking of a number between 1 and 100.  Try to guess it."
G=1
DO
  PRINT "Guess ";G;": ";
  INPUT A
  IF A=0 THEN:END:END IF
  IF A=N THEN EXIT DO
  IF A>N THEN
    PRINT "Lower..."
  ELSE
    PRINT "Higher..."
  END IF
  G=G+1
LOOP
PRINT "You guessed it!"
E:
