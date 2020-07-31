'BFBASIC BF Compiler
'For Intel 8088 or compatible with IBM compatible BIOS
'Copyright (C) 2003 by Jeffry Johnston

DIM CMD
DIM TEST

'header
PRINT CHR$(&H31);CHR$(&HC9);CHR$(&H4B);CHR$(&H88);CHR$(&H2F);CHR$(&H80);CHR$(&HFF);CHR$(&HEF);
PRINT CHR$(&H75);CHR$(&HF8);CHR$(&HE9);CHR$(&H27);CHR$(&H00);CHR$(&H5E);CHR$(&H80);CHR$(&H3F);
PRINT CHR$(&H00);CHR$(&H74);CHR$(&H0A);CHR$(&H89);CHR$(&HF2);CHR$(&H81);CHR$(&HEA);CHR$(&H05);
PRINT CHR$(&H00);CHR$(&H52);CHR$(&HFF);CHR$(&HE6);CHR$(&H90);CHR$(&HB9);CHR$(&H01);CHR$(&H00);
PRINT CHR$(&H80);CHR$(&H3C);CHR$(&HB9);CHR$(&H75);CHR$(&H01);CHR$(&H41);CHR$(&H80);CHR$(&H3C);
PRINT CHR$(&HC3);CHR$(&H75);CHR$(&H01);CHR$(&H49);CHR$(&HE3);CHR$(&H03);CHR$(&H46);CHR$(&HEB);
PRINT CHR$(&HEF);CHR$(&H46);CHR$(&HFF);CHR$(&HE6);

DO
  CMD=INKEY
  IF CMD=43 THEN '+
    'INC    BYTE PTR[BX]
    PRINT CHR$(&HFE);CHR$(&H07);
  END IF
  IF CMD=44 THEN ',
    'MOV    AH,08h
    'INT    21h
    'MOV    [BX],AL
    PRINT CHR$(&HB4);CHR$(&H08);CHR$(&HCD);CHR$(&H21);CHR$(&H88);CHR$(&H07);
  END IF
  IF CMD=45 THEN '-
    'DEC    BYTE PTR[BX]
    PRINT CHR$(&HFE);CHR$(&H0F);
  END IF
  IF CMD=46 THEN '.
    'MOV    AH,02h
    'MOV    DL,[BX]
    'INT    21h
    PRINT CHR$(&HB4);CHR$(&H02);CHR$(&H8A);CHR$(&H17);CHR$(&HCD);CHR$(&H21);
  END IF
  IF CMD=60 THEN '<
    'DEC    BX
    PRINT CHR$(&H4B);
  END IF
  IF CMD=62 THEN '>
    'INC    BX
    PRINT CHR$(&H43);
  END IF
  IF CMD=91 THEN '[
    'MOV    CX,010Dh
    'CALL   CX
    PRINT CHR$(&HB9);CHR$(&H0D);CHR$(&H01);CHR$(&HFF);CHR$(&HD1);
  END IF
  IF CMD=93 THEN ']
    'RET
    PRINT CHR$(&HC3);
  END IF
LOOP UNTIL CMD=64 '@
'@ (end)
'INT 20h
PRINT CHR$(&HCD);CHR$(&H20);
