# bfbasic
This is a port to Eclipse and GitHub of [BFBASIC](https://esolangs.org/wiki/BFBASIC), a compiler that compiles BASIC into [BrainFuck](https://esolangs.org/wiki/Brainfuck) code.

I created this repository because the original project on SourceForge seems to be closed and its [CVS repository](http://brainfuck.cvs.sourceforge.net/brainfuck/) being available only in read-only mode. Please notice that there is [another repository](https://github.com/rdebath/bfbasic) that contains a 1:1 copy of the CVS contents I used to start this repo.

Currently, I only made very minor changes to the original code (starting from version 1.50 rc3).

## Usage
Unzip the `.zip` release file. It includes `bfbasic.jar` which is an executable Java JAR that contains the BFBASIC compiler.

The .zip file contains also batch and shell files for Windows and \*nix to execute the compiler directly (assuming Java is in your execution path) with:

```
bfbasic [-c] [-d[d[d]]] [-O#] [-t] [-w [#]] FILE[.bas] [[-o] FILE] [-?]

    -c           Treat newline as CRLF, default: LF
    -d           Debug output
    -dd          Verbose debug output
    -ddd         Only debug output, no > or < generated
    -Olevel      Optimization level, default: 2
    -t           Write variable table
    -w [column]  Wraps output at the given column, default: 72
    FILE         Input filename
    -o outfile   Specify output filename, default: FILE.b
    -?           Display usage information
```

Please notce that you need [Java](https://java.com/en/download/) installed on your machine in order to run BFBASIC.
In addition, you will need a [BrainFuck compiler or iterpreter](https://esolangs.org/wiki/Brainfuck_implementations) to execute the generated BrainFuck code.
