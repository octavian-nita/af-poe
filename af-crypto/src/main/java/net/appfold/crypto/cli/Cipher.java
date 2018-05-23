package net.appfold.crypto.cli;

import net.appfold.crypto.CryptoException;
import org.apache.commons.cli.*;

import javax.validation.constraints.NotNull;
import java.io.Console;
import java.io.PrintWriter;
import java.io.StringWriter;

import static java.lang.System.*;
import static java.util.Arrays.asList;
import static org.apache.commons.cli.HelpFormatter.DEFAULT_DESC_PAD;
import static org.apache.commons.cli.HelpFormatter.DEFAULT_LEFT_PAD;
import static org.apache.commons.cli.Option.builder;

/**
 * Simple <a href="https://en.wikipedia.org/wiki/Command-line_interface">CLI</a> to encrypt / decrypt information from
 * the {@link Console character-based console device, if any, associated with the current Java virtual machine}.
 *
 * @author Octavian Theodor NITA (https://github.com/octavian-nita/)
 * @version 1.0, 2018/05/02
 */
public class Cipher {

    private final String usage;

    private final Options options = new Options();

    public Cipher() {
        options //@fmt:off
            .addOption(builder("k")
                           .longOpt("key")
                           .hasArg().optionalArg(true).argName("master-key")
                           .desc("the master key to use; " +
                                 "if no argument is provided (recommended!), prompt the user")
                           .build())
            .addOptionGroup(new OptionGroup()
                                .addOption(builder("p")
                                               .longOpt("plain")
                                               .hasArg().optionalArg(true).argName("plaintext")
                                               .desc("the information to encrypt (i.e. assume encrypt mode); " +
                                                     "if no argument is provided (recommended!), prompt the user")
                                               .build())
                                .addOption(builder("c")
                                               .longOpt("cipher")
                                               .hasArg().optionalArg(true).argName("ciphertext")
                                               .desc("the information to decrypt (i.e. assume decrypt mode); " +
                                                     "if no argument is provided, prompt the user")
                                               .build()))
            .addOption(builder("a")
                           .longOpt("associated-data")
                           .hasArg().optionalArg(false).argName("data")
                           .desc("associated data to be used for authentication; " +
                                 "if present, the argument is required; " +
                                 "multiple occurrences are allowed")
                           .build())
            .addOption(builder("e")
                           .longOpt("echo")
                           .desc("echo the user input when prompting for master key or plaintext")
                           .build())
            .addOption(builder("h")
                           .longOpt("help")
                           .desc("display usage message and exit")
                           .build()); //@fmt:on

        // Cache the usage message:
        final StringWriter usage = new StringWriter();

        final String nl = getProperty("line.separator", "\n");
        final HelpFormatter fmt = new HelpFormatter();
        fmt.setOptionComparator(null); // keep definition order for options
        fmt.setSyntaxPrefix("Usage: ");
        fmt.printHelp(new PrintWriter(usage), 72, "java [-options] " + Cipher.class.getName(), nl + //@fmt:off
                      "Encrypts plaintext (-p) or decrypts ciphertext (-c) employing a"      + nl +
                      "user-provided master key (-k). If neither -p nor -c are provided"     + nl +
                      "-p (i.e. encrypt mode) is assumed. Uses authenticated encryption"     + nl +
                      "if additional data (-a) is specified."                                + nl + nl, //@fmt:on
                      options, DEFAULT_LEFT_PAD, DEFAULT_DESC_PAD, null, true);

        this.usage = usage.toString();
    }

    public static void main(String[] args) {
        final Cipher cipher = new Cipher();
        try {
            final char[] result = cipher.run(args);
            if (result.length > 0) {
                out.println(new String(result));
                out.println();
            }
        } catch (ParseException | CryptoException e) {
            err.printf("%n%s%n%n%s%n", e.getMessage(), cipher.usage());
            exit(1);
        }
    }

    public String usage() { return usage; }

    @Override
    public String toString() { return usage(); }

    /** @return an array of length 0 if the <em>help</em> option was provided */
    @NotNull
    public char[] run(String... args) throws ParseException, CryptoException {
        final CommandLine commandLine = new DefaultParser().parse(options, args);

        if (commandLine.hasOption("h")) {
            out.printf("%n%s%n", usage());
            return new char[]{};
        }

        validate(commandLine); // multiple arguments, etc.

        char[] key = getKey(commandLine);

        return new char[]{};
    }

    private void validate(CommandLine commandLine) throws ParseException {
        if (commandLine == null) {
            return;
        }

        for (String opt : asList("k", "p", "c")) {
            final String[] val = commandLine.getOptionValues(opt);
            if (val != null && val.length > 1) {
                throw new ParseException("The option '" + opt + "' can only be specified once");
            }
        }
    }

    private char[] getKey(CommandLine commandLine) {
        if (commandLine == null) {
            return null;
        }

        if (!commandLine.hasOption('k')) {
            // generate random
        } else {
            if (commandLine.getOptionValue('k') == null) {
                // read from console
            } else {
                // return char array
            }
        }

        return new char[]{};
    }
}
