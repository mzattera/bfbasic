'99 bottles of beer
N = 99
DO
  GOSUB LYRIC1: GOSUB LYRIC2
  GOSUB LYRIC1: PRINT
  PRINT "Take one down and pass it around"
  N = N - 1
  GOSUB LYRIC1: GOSUB LYRIC2: PRINT
LOOP UNTIL N = 0
END

LYRIC1:
  IF N <> 0 THEN
    PRINT N;
  ELSE
    PRINT "No more";
  END IF
  PRINT " bottle";
  IF N <> 1 THEN
    PRINT "s";
  END IF
  PRINT " of beer";
RETURN

LYRIC2:
  PRINT " on the wall"
RETURN
