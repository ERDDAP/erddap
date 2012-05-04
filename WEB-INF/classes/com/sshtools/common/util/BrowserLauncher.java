/*
 *  SSHTools - Java SSH2 API
 *
 *  Copyright (C) 2002-2003 Lee David Painter and Contributors.
 *
 *  Contributions made by:
 *
 *  Brett Smith
 *  Richard Pernavas
 *  Erwin Bolwidt
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Library General Public License
 *  as published by the Free Software Foundation; either version 2 of
 *  the License, or (at your option) any later version.
 *
 *  You may also distribute it and/or modify it under the terms of the
 *  Apache style J2SSH Software License. A copy of which should have
 *  been provided with the distribution.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  License document supplied with your distribution for more details.
 *
 */
package com.sshtools.common.util;

import java.io.*;

import java.lang.reflect.*;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.14 $
 */
public class BrowserLauncher {
    private static int jvm;
    private static Object browser;
    private static boolean loadedWithoutErrors;
    private static Class mrjFileUtilsClass;
    private static Class mrjOSTypeClass;
    private static Class macOSErrorClass;
    private static Class aeDescClass;
    private static Constructor aeTargetConstructor;
    private static Constructor appleEventConstructor;
    private static Constructor aeDescConstructor;
    private static Method findFolder;
    private static Method getFileType;
    private static Method makeOSType;
    private static Method putParameter;
    private static Method sendNoReply;
    private static Object kSystemFolderType;
    private static Integer keyDirectObject;
    private static Integer kAutoGenerateReturnID;
    private static Integer kAnyTransactionID;
    private static final int MRJ_2_0 = 0;
    private static final int MRJ_2_1 = 1;
    private static final int WINDOWS_NT = 2;
    private static final int WINDOWS_9x = 3;
    private static final int OTHER = -1;
    private static final String FINDER_TYPE = "FNDR";
    private static final String FINDER_CREATOR = "MACS";
    private static final String GURL_EVENT = "GURL";
    private static final String FIRST_WINDOWS_PARAMETER = "/c";
    private static final String SECOND_WINDOWS_PARAMETER = "start";
    private static final String NETSCAPE_OPEN_PARAMETER_START = " -remote 'openURL(";
    private static final String NETSCAPE_OPEN_PARAMETER_END = ")'";
    private static String errorMessage;

    static {
        loadedWithoutErrors = true;

        String osName = System.getProperty("os.name");

        if ("Mac OS".equals(osName)) {
            String mrjVersion = System.getProperty("mrj.version");
            String majorMRJVersion = mrjVersion.substring(0, 3);

            try {
                double version = Double.parseDouble(majorMRJVersion);

                if (version == 2) {
                    jvm = MRJ_2_0;
                } else if (version >= 2.1) {
                    // For the time being, assume that all post-2.0 versions of MRJ work the same
                    jvm = MRJ_2_1;
                } else {
                    loadedWithoutErrors = false;
                    errorMessage = "Unsupported MRJ version: " + version;
                }
            } catch (NumberFormatException nfe) {
                loadedWithoutErrors = false;
                errorMessage = "Invalid MRJ version: " + mrjVersion;
            }
        } else if (osName.startsWith("Windows")) {
            if (osName.indexOf("9") != -1) {
                jvm = WINDOWS_9x;
            } else {
                jvm = WINDOWS_NT;
            }
        } else {
            jvm = OTHER;
        }

        if (loadedWithoutErrors) { // if we haven't hit any errors yet
            loadedWithoutErrors = loadClasses();
        }
    }

    private BrowserLauncher() {
    }

    private static boolean loadClasses() {
        switch (jvm) {
        case MRJ_2_0:

            try {
                Class aeTargetClass = Class.forName("com.apple.MacOS.AETarget");
                macOSErrorClass = Class.forName("com.apple.MacOS.MacOSError");

                Class osUtilsClass = Class.forName("com.apple.MacOS.OSUtils");
                Class appleEventClass = Class.forName(
                        "com.apple.MacOS.AppleEvent");
                Class aeClass = Class.forName("com.apple.MacOS.ae");
                aeDescClass = Class.forName("com.apple.MacOS.AEDesc");
                aeTargetConstructor = aeTargetClass.getDeclaredConstructor(new Class[] {
                            int.class
                        });
                appleEventConstructor = appleEventClass.getDeclaredConstructor(new Class[] {
                            int.class, int.class, aeTargetClass, int.class,
                            int.class
                        });
                aeDescConstructor = aeDescClass.getDeclaredConstructor(new Class[] {
                            String.class
                        });
                makeOSType = osUtilsClass.getDeclaredMethod("makeOSType",
                        new Class[] { String.class });
                putParameter = appleEventClass.getDeclaredMethod("putParameter",
                        new Class[] { int.class, aeDescClass });
                sendNoReply = appleEventClass.getDeclaredMethod("sendNoReply",
                        new Class[] {  });

                Field keyDirectObjectField = aeClass.getDeclaredField(
                        "keyDirectObject");
                keyDirectObject = (Integer) keyDirectObjectField.get(null);

                Field autoGenerateReturnIDField = appleEventClass.getDeclaredField(
                        "kAutoGenerateReturnID");
                kAutoGenerateReturnID = (Integer) autoGenerateReturnIDField.get(null);

                Field anyTransactionIDField = appleEventClass.getDeclaredField(
                        "kAnyTransactionID");
                kAnyTransactionID = (Integer) anyTransactionIDField.get(null);
            } catch (ClassNotFoundException cnfe) {
                errorMessage = cnfe.getMessage();

                return false;
            } catch (NoSuchMethodException nsme) {
                errorMessage = nsme.getMessage();

                return false;
            } catch (NoSuchFieldException nsfe) {
                errorMessage = nsfe.getMessage();

                return false;
            } catch (IllegalAccessException iae) {
                errorMessage = iae.getMessage();

                return false;
            }

            break;

        case MRJ_2_1:

            try {
                mrjFileUtilsClass = Class.forName("com.apple.mrj.MRJFileUtils");
                mrjOSTypeClass = Class.forName("com.apple.mrj.MRJOSType");

                Field systemFolderField = mrjFileUtilsClass.getDeclaredField(
                        "kSystemFolderType");
                kSystemFolderType = systemFolderField.get(null);
                findFolder = mrjFileUtilsClass.getDeclaredMethod("findFolder",
                        new Class[] { mrjOSTypeClass });
                getFileType = mrjFileUtilsClass.getDeclaredMethod("getFileType",
                        new Class[] { File.class });
            } catch (ClassNotFoundException cnfe) {
                errorMessage = cnfe.getMessage();

                return false;
            } catch (NoSuchFieldException nsfe) {
                errorMessage = nsfe.getMessage();

                return false;
            } catch (NoSuchMethodException nsme) {
                errorMessage = nsme.getMessage();

                return false;
            } catch (SecurityException se) {
                errorMessage = se.getMessage();

                return false;
            } catch (IllegalAccessException iae) {
                errorMessage = iae.getMessage();

                return false;
            }

            break;
        }

        return true;
    }

    private static Object locateBrowser() {
        if (browser != null) {
            return browser;
        }

        switch (jvm) {
        case MRJ_2_0:

            try {
                Integer finderCreatorCode = (Integer) makeOSType.invoke(null,
                        new Object[] { FINDER_CREATOR });
                Object aeTarget = aeTargetConstructor.newInstance(new Object[] {
                            finderCreatorCode
                        });
                Integer gurlType = (Integer) makeOSType.invoke(null,
                        new Object[] { GURL_EVENT });
                Object appleEvent = appleEventConstructor.newInstance(new Object[] {
                            gurlType, gurlType, aeTarget, kAutoGenerateReturnID,
                            kAnyTransactionID
                        });

                // Don't set browser = appleEvent because then the next time we call
                // locateBrowser(), we'll get the same AppleEvent, to which we'll already have
                // added the relevant parameter. Instead, regenerate the AppleEvent every time.
                // There's probably a way to do this better; if any has any ideas, please let
                // me know.
                return appleEvent;
            } catch (IllegalAccessException iae) {
                browser = null;
                errorMessage = iae.getMessage();

                return browser;
            } catch (InstantiationException ie) {
                browser = null;
                errorMessage = ie.getMessage();

                return browser;
            } catch (InvocationTargetException ite) {
                browser = null;
                errorMessage = ite.getMessage();

                return browser;
            }

        case MRJ_2_1:

            File systemFolder;

            try {
                systemFolder = (File) findFolder.invoke(null,
                        new Object[] { kSystemFolderType });
            } catch (IllegalArgumentException iare) {
                browser = null;
                errorMessage = iare.getMessage();

                return browser;
            } catch (IllegalAccessException iae) {
                browser = null;
                errorMessage = iae.getMessage();

                return browser;
            } catch (InvocationTargetException ite) {
                browser = null;
                errorMessage = ite.getTargetException().getClass() + ": " +
                    ite.getTargetException().getMessage();

                return browser;
            }

            String[] systemFolderFiles = systemFolder.list();

            // Avoid a FilenameFilter because that can't be stopped mid-list
            for (int i = 0; i < systemFolderFiles.length; i++) {
                try {
                    File file = new File(systemFolder, systemFolderFiles[i]);

                    if (!file.isFile()) {
                        continue;
                    }

                    Object fileType = getFileType.invoke(null,
                            new Object[] { file });

                    if (FINDER_TYPE.equals(fileType.toString())) {
                        browser = file.toString(); // Actually the Finder, but that's OK

                        return browser;
                    }
                } catch (IllegalArgumentException iare) {
                    browser = browser;
                    errorMessage = iare.getMessage();

                    return null;
                } catch (IllegalAccessException iae) {
                    browser = null;
                    errorMessage = iae.getMessage();

                    return browser;
                } catch (InvocationTargetException ite) {
                    browser = null;
                    errorMessage = ite.getTargetException().getClass() + ": " +
                        ite.getTargetException().getMessage();

                    return browser;
                }
            }

            browser = null;

            break;

        case WINDOWS_NT:
            browser = "cmd.exe";

            break;

        case WINDOWS_9x:
            browser = "command.com";

            break;

        case OTHER:default:

            //browser = "netscape"; surely mozilla is the thing these days
            browser = "mozilla";

            break;
        }

        return browser;
    }

    /**
*
*
* @param url
*
* @throws IOException
*/
    public static void openURL(String url) throws IOException {
        if (!loadedWithoutErrors) {
            throw new IOException("Exception in finding browser: " +
                errorMessage);
        }

        Object browser = locateBrowser();

        if (browser == null) {
            throw new IOException("Unable to locate browser: " + errorMessage);
        }

        switch (jvm) {
        case MRJ_2_0:

            Object aeDesc = null;

            try {
                aeDesc = aeDescConstructor.newInstance(new Object[] { url });
                putParameter.invoke(browser,
                    new Object[] { keyDirectObject, aeDesc });
                sendNoReply.invoke(browser, new Object[] {  });
            } catch (InvocationTargetException ite) {
                throw new IOException(
                    "InvocationTargetException while creating AEDesc: " +
                    ite.getMessage());
            } catch (IllegalAccessException iae) {
                throw new IOException(
                    "IllegalAccessException while building AppleEvent: " +
                    iae.getMessage());
            } catch (InstantiationException ie) {
                throw new IOException(
                    "InstantiationException while creating AEDesc: " +
                    ie.getMessage());
            } finally {
                aeDesc = null; // Encourage it to get disposed if it was created
                browser = null; // Ditto
            }

            break;

        case MRJ_2_1:
            Runtime.getRuntime().exec(new String[] { (String) browser, url });

            break;

        case WINDOWS_NT:
        case WINDOWS_9x:
            Runtime.getRuntime().exec(new String[] {
                    (String) browser, FIRST_WINDOWS_PARAMETER,
                    SECOND_WINDOWS_PARAMETER, url
                });

            break;

        case OTHER:

            // Assume that we're on Unix and that Netscape is installed
            // First, attempt to open the URL in a currently running session of Netscape
            Process process = Runtime.getRuntime().exec((String) browser +
                    NETSCAPE_OPEN_PARAMETER_START + url +
                    NETSCAPE_OPEN_PARAMETER_END);

            try {
                int exitCode = process.waitFor();

                if (exitCode != 0) { // if Netscape was not open
                    Runtime.getRuntime().exec(new String[] { (String) browser, url });
                }
            } catch (InterruptedException ie) {
                throw new IOException(
                    "InterruptedException while launching browser: " +
                    ie.getMessage());
            }

            break;

        default:

            // This should never occur, but if it does, we'll try the simplest thing possible
            Runtime.getRuntime().exec(new String[] { (String) browser, url });

            break;
        }
    }
}
