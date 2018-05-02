package net.appfold.crypto.tool;

import java.io.BufferedReader;
import java.io.Console;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Locale;
import java.util.logging.Logger;

import static java.lang.System.getProperty;
import static java.lang.System.setProperty;
import static java.util.Locale.US;

/**
 * Simple <a href="https://en.wikipedia.org/wiki/Command-line_interface">CLI</a> to encrypt / decrypt information at the
 * {@link Console}.
 *
 * @author Octavian Theodor NITA (https://github.com/octavian-nita/)
 * @version 1.0, 2018/05/02
 */
public class Cipher {

    public static void main(String[] args) throws Exception {
        Locale.setDefault(US);
        setProperty("java.util.logging.SimpleFormatter.format", "%1$tY/%1$tm/%1$td %4$s %5$s%6$s%n");

        Authenticator.setDefault(new Authenticator() {

            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                String protocol = getRequestingProtocol();
                protocol = protocol == null ? "http" : protocol.toLowerCase();

                Logger.getGlobal().info("Requesting protocol: " + protocol);

                int versionPos = protocol.indexOf('/');
                if (versionPos >= 0) {
                    protocol = protocol.substring(0, versionPos);
                }

                return new PasswordAuthentication(getProperty(protocol + ".proxyUser"),
                                                  getProperty(protocol + ".proxyPassword", "").toCharArray());
            }
        });

        try (final InputStream is = new URL("https://google.be").openStream();
             final BufferedReader rd = new BufferedReader(new InputStreamReader(is))) {
            rd.lines().forEach(System.out::println);
        }
    }
}
