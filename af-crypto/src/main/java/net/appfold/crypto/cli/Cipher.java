package net.appfold.crypto.cli;

import java.io.BufferedReader;
import java.io.Console;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Locale;

import static java.lang.System.getProperty;
import static java.lang.System.setProperty;
import static java.util.Locale.US;

/**
 * Simple <a href="https://en.wikipedia.org/wiki/Command-line_interface">CLI</a> to encrypt / decrypt information from
 * the {@link Console character-based console device, if any, associated with the current Java virtual machine}.
 *
 * @author Octavian Theodor NITA (https://github.com/octavian-nita/)
 * @version 1.0, 2018/05/02
 * @see <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/net/proxies.html">Java Networking and Proxies</a>
 */
public class Cipher {

    public static void main(String[] args) throws Exception {
        Locale.setDefault(US);
        setProperty("java.util.logging.SimpleFormatter.format", "%1$tY/%1$tm/%1$td %4$s %5$s%6$s%n");

        Authenticator.setDefault(new Authenticator() {

            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                String protocol = getRequestingProtocol();
                if (protocol == null) {
                    return super.getPasswordAuthentication();
                }

                int protoVerPos = protocol.indexOf('/');
                if (protoVerPos >= 0) {
                    protocol = protocol.substring(0, protoVerPos);
                }

                return new PasswordAuthentication(getProperty(protocol + ".proxyUser"),
                                                  getProperty(protocol + ".proxyPassword", "").toCharArray());
            }
        });

        try (final InputStream is = new URL("https://google.ro").openStream();
             final BufferedReader rd = new BufferedReader(new InputStreamReader(is))) {
            rd.lines().forEach(System.out::println);
        }
    }
}
