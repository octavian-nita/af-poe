package net.appfold.crypto.cli;

import org.apache.commons.cli.*;

import javax.validation.constraints.NotNull;
import java.io.Console;
import java.io.PrintWriter;
import java.io.StringWriter;

import static java.lang.System.*;
import static org.apache.commons.cli.HelpFormatter.*;
import static org.apache.commons.cli.Option.builder;

/**
 * Simple <a href="https://en.wikipedia.org/wiki/Command-line_interface">CLI</a> to encrypt / decrypt information from
 * the {@link Console character-based console device, if any, associated with the current Java virtual machine}.
 *
 * @author Octavian Theodor NITA (https://github.com/octavian-nita/)
 * @version 1.0, 2018/05/02
 */
public class Cipher {

    private final Options options = new Options();

    public Cipher() {
        final OptionGroup encryptDecryptOpt = new OptionGroup();
        encryptDecryptOpt //@fmt:off
            .addOption(builder("p").longOpt("plain").desc("the information to encrypt (i.e. assume encrypt mode); " +
                                                          "if argument not provided, prompt the user")
                                   .hasArg().optionalArg(true).argName("plaintext").build())
            .addOption(builder("c").longOpt("cipher").desc("the information to decrypt (i.e. assume decrypt mode); " +
                                                           "if argument not provided, prompt the user")
                                   .hasArg().optionalArg(true).argName("ciphertext").build()); //@fmt:on

        options //@fmt:off
            .addOption(builder("h").longOpt("help").desc("display usage and exit")
                                   .hasArg(false).build())
            .addOption(builder("k").longOpt("key").desc("the key to use; " +
                                                        "if the argument is not provided, prompt the user for the key")
                                   .hasArg().optionalArg(true).argName("secret-key").build())
            .addOptionGroup(encryptDecryptOpt)
            .addOption(builder("a").longOpt("associated-data").desc("associated data for authentication; " +
                                                                    "if option present, the argument is required")
                                   .hasArg().optionalArg(false).argName("data").build()); //@fmt:on
    }

    public String usage() {
        final StringWriter usage = new StringWriter();

        final String nl = getProperty("line.separator", "\n");
        final HelpFormatter help = new HelpFormatter();
        help.setOptionComparator(null);
        help.setSyntaxPrefix("Usage: ");
        help.printHelp(new PrintWriter(usage), DEFAULT_WIDTH, "java -cp ... " + Cipher.class.getName(), nl + //@fmt:off
                       "Encrypts plaintext (-p) or decrypts ciphertext (-c) employing a" + nl +
                       "user-provided master key (-k). Uses authenticated encryption if" + nl +
                       "additional data (-a) is provided." +
                       nl + nl, options, //@fmt:on
                       DEFAULT_LEFT_PAD, DEFAULT_DESC_PAD,
                       nl + "If neither -p not -c are provided, -p (i.e. encrypt mode) is assumed.", true);

        return usage.toString();
    }

    @NotNull
    public char[] run(String... args) throws ParseException {
        final CommandLine commandLine = new DefaultParser().parse(options, args);

        if (commandLine.hasOption("h")) {
            out.printf("%n%s%n", usage());
            return new char[]{0};
        }

        return new char[]{0};
    }

    public static void main(String[] args) {
        final Cipher cipher = new Cipher();
        try {
            final char[] result = cipher.run(args);
            if (result.length > 0) {
                out.println(new String(result));
            }
            out.println();
        } catch (ParseException e) {
            err.printf("%n%s%n%n%s%n", e.getMessage(), cipher.usage());
            exit(1);
        }
    }
}
