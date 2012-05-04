/* 
 * SSR Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.util;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

/**
 * This class is needed by SSR.sendEmail.
 */
public class PasswordAuthenticator extends Authenticator {
    private String userName;
    private String password;

    /**
     * The constructor.
     */
    public PasswordAuthenticator(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    /**
     * This returns a passwordAuthenticaion object with 
     * the userName and password.
     */
    public PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(userName, password);
    } 
}