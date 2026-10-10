package com.joshdreagan.bmv;

import com.joshdreagan.bmv.engine.ActionType;
import com.joshdreagan.bmv.engine.BulkMvEngine;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.concurrent.Callable;

@TopCommand
@Command(
        name = "bulk-mv",
        mixinStandardHelpOptions = true,
        version = "bulk-mv 1.0.0",
        description = "Move, copy or link files using pattern matching mimicking zshell zmv."
)
public class BulkMvCommand implements Callable<Integer> {

    @Option(names = {"-f", "--force"}, description = "Force overwriting of destination files.")
    boolean force;

    @Option(names = {"-i", "--interactive"}, description = "Interactive: prompt before executing each move.")
    boolean interactive;

    @Option(names = {"-n", "--dry-run"}, description = "No execution: print what would happen, but don't do it.")
    boolean dryRun;

    @Option(names = {"-v", "--verbose"}, description = "Verbose: print each command as it's executed.")
    boolean verbose;

    @Option(names = {"-Q"}, description = "Force bare glob qualifiers on.")
    boolean bareGlobQualifiersOn;

    @Option(names = {"-s", "--symbolic"}, description = "Symbolic, passed down to ln; only works with -L.")
    boolean symbolic;

    @Option(names = {"-w"}, description = "Pick out wildcard parts of pattern and automatically parenthesise them.")
    boolean withWildcards;

    @Option(names = {"-W"}, description = "Like -w, and turn wildcards in replacement into sequential $1..$N references.")
    boolean withWildcardsAndReplacement;

    @Option(names = {"-C"}, description = "Force cp (copy) instead of mv.")
    boolean actionCopy;

    @Option(names = {"-L"}, description = "Force ln (link) instead of mv.")
    boolean actionLink;

    @Option(names = {"-p"}, description = "Call program instead of cp, ln or mv (supports --).")
    String program;

    @Option(names = {"-P"}, description = "Call program without -- delimiter.")
    String programNoDashes;

    @Option(names = {"-o"}, description = "Options passed down verbatim to the command.")
    String optString;

    @Parameters(arity = "0..*", description = "oldpattern newpattern")
    List<String> patterns;

    @Override
    public Integer call() {
        if (patterns == null || patterns.size() != 2) {
            System.err.println("""
Usage:
  bulk-mv [OPTIONS] oldpattern newpattern
where oldpattern contains parenthesis surrounding patterns which will
be replaced in turn by $1, $2, ... in newpattern.  For example,
  bulk-mv '(*).lis' '$1.txt'
renames 'foo.lis' to 'foo.txt', 'my.old.stuff.lis' to 'my.old.stuff.txt',
and so on.  Something simpler (for basic commands) is the -W option:
  bulk-mv -W '*.lis' '*.txt'
This does the same thing as the first command, but with automatic conversion
of the wildcards into the appropriate syntax.  If you combine this with
noglob, you don't even need to quote the arguments.  For example,
  alias mmv='noglob bulk-mv -W'
  mmv *.c.orig orig/*.c""");
            return 1;
        }

        BulkMvEngine.Options options = new BulkMvEngine.Options();
        options.force = force;
        options.interactive = interactive;
        options.dryRun = dryRun;
        options.verbose = verbose;
        options.symbolic = symbolic;
        options.withWildcards = withWildcards;
        options.withWildcardsAndReplacement = withWildcardsAndReplacement;
        options.bareGlobQualifiers = bareGlobQualifiersOn;
        options.optString = optString;

        if (program != null) {
            options.action = ActionType.CUSTOM;
            options.program = program;
            options.programNoDashes = false;
        } else if (programNoDashes != null) {
            options.action = ActionType.CUSTOM;
            options.program = programNoDashes;
            options.programNoDashes = true;
        } else if (actionCopy) {
            options.action = ActionType.CP;
        } else if (actionLink) {
            options.action = ActionType.LN;
        } else {
            options.action = ActionType.MV;
        }

        return BulkMvEngine.run(options, patterns.get(0), patterns.get(1));
    }
}
