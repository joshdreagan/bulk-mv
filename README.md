# bulk-mv

Move, copy or link files using pattern matching mimicking zshell zmv.

## Documentation

```
Usage: bulk-mv [-CfhiLnQsvVwW] [-o=<optString>] [-p=<program>]
               [-P=<programNoDashes>] [<patterns>...]
Move, copy or link files using pattern matching mimicking zshell zmv.
      [<patterns>...]     oldpattern newpattern
  -C                      Force cp (copy) instead of mv.
  -f, --force             Force overwriting of destination files.
  -h, --help              Show this help message and exit.
  -i, --interactive       Interactive: prompt before executing each move.
  -L                      Force ln (link) instead of mv.
  -n, --dry-run           No execution: print what would happen, but don't do
                            it.
  -o=<optString>          Options passed down verbatim to the command.
  -p=<program>            Call program instead of cp, ln or mv (supports --).
  -P=<programNoDashes>    Call program without -- delimiter.
  -Q                      Force bare glob qualifiers on.
  -s, --symbolic          Symbolic, passed down to ln; only works with -L.
  -v, --verbose           Verbose: print each command as it's executed.
  -V, --version           Print version information and exit.
  -w                      Pick out wildcard parts of pattern and automatically
                            parenthesise them.
  -W                      Like -w, and turn wildcards in replacement into
                            sequential $1..$N references.
```
