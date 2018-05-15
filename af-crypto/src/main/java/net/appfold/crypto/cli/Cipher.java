package net.appfold.crypto.cli;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.io.Console;

/**
 * Simple <a href="https://en.wikipedia.org/wiki/Command-line_interface">CLI</a> to encrypt / decrypt information from
 * the {@link Console character-based console device, if any, associated with the current Java virtual machine}.
 *
 * @author Octavian Theodor NITA (https://github.com/octavian-nita/)
 * @version 1.0, 2018/05/02
 */
public class Cipher {

    public static void main(String[] args) {
        final OptionParser optsParser = new OptionParser();

        optsParser.accepts("key", "encryption key to use").withRequiredArg();
        optsParser.accepts("help", "display usage information").forHelp();

        final OptionSet opts = optsParser.parse("--help"/*, "-k", "bobo"*/);

        System.out.println(opts.asMap());
    }
}
