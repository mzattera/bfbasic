# bfbasic
This is a GitHub repository for [BFBASIC](https://esolangs.org/wiki/BFBASIC).

I created this repository because the original project on SourceForge seems to be closed and its [CVS repository](http://brainfuck.cvs.sourceforge.net/brainfuck/) being available only in read-only mode.

## Usage
Unzip the `.zip` release file. It includes `bfbasic.jar` which is an executable Java JAR that contains the BFBASIC compiler.

The compiler can be executed with (assuming Java is in your execution path):

```
java -jar bfbasic.jar <parameters>
```

Where `<parameters>` are:

```
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

The .zip file contains also batch and shell files for Windows an Unix to execute the compiler directly:

```
bfbasic [-c] [-d[d[d]]] [-O#] [-t] [-w [#]] FILE[.bas] [[-o] FILE] [-?]
```
