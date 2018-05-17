package net.appfold.crypto.cli;

import org.apache.commons.cli.*;

import java.io.Console;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Comparator;

import static java.lang.Integer.max;
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

    public static void main(String[] args) {
        final OptionGroup encryptDecryptOpt = new OptionGroup();
        encryptDecryptOpt //@fmt:off
            .addOption(builder("p").longOpt("plain").desc("the information to encrypt; " +
                                                          "if argument not provided, prompt the user")
                                   .hasArg().optionalArg(true).argName("plaintext").build())
            .addOption(builder("c").longOpt("cipher").desc("the information to decrypt; " +
                                                           "if argument not provided, prompt the user")
                                   .hasArg().optionalArg(true).argName("ciphertext").build()); //@fmt:on

        final Options options = new Options() //@fmt:off
            .addOption(builder("h").longOpt("help").desc("display usage and exit")
                                   .hasArg(false).build())
            .addOption(builder("k").longOpt("key").desc("the key to use; " +
                                                        "if argument not provided, prompt the user")
                                   .hasArg().optionalArg(true).argName("secret-key").build())
            .addOptionGroup(encryptDecryptOpt)
            .addOption(builder("a").longOpt("associated-data").desc("associated data for authentication; " +
                                                                    "if option present, the argument is required")
                                   .hasArg().optionalArg(false).argName("data").build()); //@fmt:on

        try {
            final CommandLine commandLine = new DefaultParser().parse(options, new String[]{"-c", "123", "-c", "234"});

            out.println(commandLine.getOptionValue("c"));

            out.printf("%s%n", usage(options));
        } catch (ParseException e) {
            err.printf("%n%s%n%n%s%n", e.getMessage(), usage(options));
            exit(1);
        }
    }

    private static String usage(Options opts) {
        final StringWriter usage = new StringWriter();

        final HelpFormatter help = new HelpFormatter();
        help.setSyntaxPrefix("Usage: ");
        help.setOptionComparator(new Comparator<Option>() {

            private int rank(Option option) {
                if (option == null) {
                    return 0;
                }
                switch (option.getId()) {
                case 'k':
                    return 1;
                case 'p':
                    return 2;
                case 'c':
                    return 3;
                case 'a':
                    return 4;
                case 'h':
                    return 5;
                default:
                    return 0;
                }
            }

            @Override
            public int compare(Option o1, Option o2) { return max(rank(o1), rank(o2)); }
        });
        help.printHelp(new PrintWriter(usage), DEFAULT_WIDTH, "java -cp ... " + Cipher.class.getName(), null, opts,
                       DEFAULT_LEFT_PAD, DEFAULT_DESC_PAD, null, true);

        return usage.toString();
    }
}
