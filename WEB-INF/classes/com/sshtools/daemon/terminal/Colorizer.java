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

/**
 * SSHTools - Java SSH API The contents of this package has been derived from
 * the TelnetD library available from http://sourceforge.net/projects/telnetd
 * The original license of the source code is as follows: TelnetD library
 * (embeddable telnet daemon) Copyright (C) 2000 Dieter Wimberger This library
 * is free software; you can either redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License version 2.1,1999 as
 * published by the Free Software Foundation (see copy received along with the
 * library), or under the terms of the BSD-style license received along with
 * this library.
 */
package com.sshtools.daemon.terminal;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.13 $
 */
public final class Colorizer {
    private static Object Self; //Singleton instance reference
    private static int testcount = 0;
    private static Colorizer myColorizer;

    //Constants
    private static final int 
    /*black*/ S = 30;

    //Constants
    private static final int s = 40;

    //Constants
    private static final int 
    /*red*/ R = 31;

    //Constants
    private static final int r = 41;

    //Constants
    private static final int 
    /*green*/ G = 32;

    //Constants
    private static final int g = 42;

    //Constants
    private static final int 
    /*yellow*/ Y = 33;

    //Constants
    private static final int y = 43;

    //Constants
    private static final int 
    /*blue*/ B = 34;

    //Constants
    private static final int b = 44;

    //Constants
    private static final int 
    /*magenta*/ M = 35;

    //Constants
    private static final int m = 45;

    //Constants
    private static final int 
    /*cyan*/ C = 36;

    //Constants
    private static final int c = 46;

    //Constants
    private static final int 
    /*white*/ W = 37;

    //Constants
    private static final int w = 47;

    //Constants
    private static final int 
    /*bold*/ f = 1;

    //Constants
    private static final int 
    /*!bold*/ d = 22;

    //Constants
    private static final int 
    /*italic*/ i = 3;

    //Constants
    private static final int 
    /*!italic*/ j = 23;

    //Constants
    private static final int 
    /*underlined*/ u = 4;

    //Constants
    private static final int 
    /*!underlined*/ v = 24;

    //Constants
    private static final int 
    /*blink*/ e = 5;

    //Constants
    private static final int 
    /*steady*/ n = 25;

    //Constants
    private static final int 
    /*hide*/ h = 8;

    //Constants
    private static final int 
    /*all out*/ a = 0;
    private int[] colortranslation; //translation table
    private int leng;

    private Colorizer() {
        colortranslation = new int[128];
        colortranslation[83] = S;
        colortranslation[82] = R;
        colortranslation[71] = G;
        colortranslation[89] = Y;
        colortranslation[66] = B;
        colortranslation[77] = M;
        colortranslation[67] = C;
        colortranslation[87] = W;
        colortranslation[115] = s;
        colortranslation[114] = r;
        colortranslation[103] = g;
        colortranslation[121] = y;
        colortranslation[98] = b;
        colortranslation[109] = m;
        colortranslation[99] = c;
        colortranslation[119] = w;
        colortranslation[102] = f;
        colortranslation[100] = d;
        colortranslation[105] = i;
        colortranslation[106] = j;
        colortranslation[117] = u;
        colortranslation[118] = v;
        colortranslation[101] = e;
        colortranslation[110] = n;
        colortranslation[104] = h;
        colortranslation[97] = a;
        Self = this;
    }

    //constructor
    public String colorize(String str, boolean support) {
        StringBuffer out = new StringBuffer(str.length() + 20);
        int parsecursor = 0;
        int foundcursor = 0;
        boolean done = false;

        while (!done) {
            foundcursor = str.indexOf(ColorHelper.MARKER_CODE, parsecursor);

            if (foundcursor != -1) {
                out.append(str.substring(parsecursor, foundcursor));

                if (support) {
                    out.append(addEscapeSequence(str.substring(foundcursor + 1,
                                foundcursor + 2)));
                }

                parsecursor = foundcursor + 2;
            } else {
                out.append(str.substring(parsecursor, str.length()));
                done = true;
            }
        }

        /*
 * This will always add a "reset all" escape sequence
 * behind the input string.
 * Basically this is a good idea, because developers tend to
 * forget writing colored strings properly.
 */
        if (support) {
            out.append(addEscapeSequence("a"));
        }

        return out.toString();
    }

    //colorize
    private String addEscapeSequence(String attribute) {
        StringBuffer tmpbuf = new StringBuffer(10);
        byte[] tmpbytes = attribute.getBytes();
        int key = (int) tmpbytes[0];
        tmpbuf.append((char) 27);
        tmpbuf.append((char) 91);
        tmpbuf.append((new Integer(colortranslation[key])).toString());
        tmpbuf.append((char) 109);

        return tmpbuf.toString();
    }

    //addEscapeSequence
    public static Colorizer getReference() {
        if (Self != null) {
            return (Colorizer) Self;
        } else {
            return new Colorizer();
        }
    }

    //getReference
    private static void announceResult(boolean res) {
        if (res) {
            System.out.println("[#" + testcount + "] ok.");
        } else {
            System.out.println("[#" + testcount +
                "] failed (see possible StackTrace).");
        }
    }

    //announceResult
    private static void announceTest(String what) {
        testcount++;
        System.out.println("Test #" + testcount + " [" + what + "]:");
    }

    //announceTest
    private static void bfcolorTest(String color) {
        System.out.println("->" +
            myColorizer.colorize(ColorHelper.boldcolorizeText("COLOR", color),
                true) + "<-");
    }

    //bfcolorTest
    private static void fcolorTest(String color) {
        System.out.println("->" +
            myColorizer.colorize(ColorHelper.colorizeText("COLOR", color), true) +
            "<-");
    }

    //fcolorTest
    private static void bcolorTest(String color) {
        System.out.println("->" +
            myColorizer.colorize(ColorHelper.colorizeBackground("     ", color),
                true) + "<-");
    }

    //bcolorTest
    public static void main(String[] args) {
        try {
            announceTest("Instantiation");
            myColorizer = Colorizer.getReference();
            announceResult(true);
            announceTest("Textcolor Tests");
            fcolorTest(ColorHelper.BLACK);
            fcolorTest(ColorHelper.RED);
            fcolorTest(ColorHelper.GREEN);
            fcolorTest(ColorHelper.YELLOW);
            fcolorTest(ColorHelper.BLUE);
            fcolorTest(ColorHelper.MAGENTA);
            fcolorTest(ColorHelper.CYAN);
            fcolorTest(ColorHelper.white);
            announceResult(true);
            announceTest("Bold textcolor Tests");
            bfcolorTest(ColorHelper.BLACK);
            bfcolorTest(ColorHelper.RED);
            bfcolorTest(ColorHelper.GREEN);
            bfcolorTest(ColorHelper.YELLOW);
            bfcolorTest(ColorHelper.BLUE);
            bfcolorTest(ColorHelper.MAGENTA);
            bfcolorTest(ColorHelper.CYAN);
            bfcolorTest(ColorHelper.white);
            announceResult(true);
            announceTest("Background Tests");
            bcolorTest(ColorHelper.BLACK);
            bcolorTest(ColorHelper.RED);
            bcolorTest(ColorHelper.GREEN);
            bcolorTest(ColorHelper.YELLOW);
            bcolorTest(ColorHelper.BLUE);
            bcolorTest(ColorHelper.MAGENTA);
            bcolorTest(ColorHelper.CYAN);
            bcolorTest(ColorHelper.white);
            announceResult(true);
            announceTest("Mixed Color Tests");
            System.out.println("->" +
                myColorizer.colorize(ColorHelper.colorizeText("COLOR",
                        ColorHelper.white, ColorHelper.BLUE), true) + "<-");
            System.out.println("->" +
                myColorizer.colorize(ColorHelper.colorizeText("COLOR",
                        ColorHelper.YELLOW, ColorHelper.GREEN), true) + "<-");
            System.out.println("->" +
                myColorizer.colorize(ColorHelper.boldcolorizeText("COLOR",
                        ColorHelper.white, ColorHelper.BLUE), true) + "<-");
            System.out.println("->" +
                myColorizer.colorize(ColorHelper.boldcolorizeText("COLOR",
                        ColorHelper.YELLOW, ColorHelper.GREEN), true) + "<-");
            announceResult(true);
            announceTest("Style Tests");
            System.out.println("->" +
                myColorizer.colorize(ColorHelper.boldText("Bold"), true) +
                "<-");
            System.out.println("->" +
                myColorizer.colorize(ColorHelper.italicText("Italic"), true) +
                "<-");
            System.out.println("->" +
                myColorizer.colorize(ColorHelper.underlinedText("Underlined"),
                    true) + "<-");
            System.out.println("->" +
                myColorizer.colorize(ColorHelper.blinkingText("Blinking"), true) +
                "<-");
            announceResult(true);
            announceTest("Visible length test");

            String colorized = ColorHelper.boldcolorizeText("STRING",
                    ColorHelper.YELLOW);
            System.out.println("->" + myColorizer.colorize(colorized, true) +
                "<-");
            System.out.println("Visible length=" +
                ColorHelper.getVisibleLength(colorized));
            colorized = ColorHelper.boldcolorizeText("BANNER",
                    ColorHelper.white, ColorHelper.BLUE) +
                ColorHelper.colorizeText("COLOR", ColorHelper.white,
                    ColorHelper.BLUE) + ColorHelper.underlinedText("UNDER");
            System.out.println("->" + myColorizer.colorize(colorized, true) +
                "<-");
            System.out.println("Visible length=" +
                ColorHelper.getVisibleLength(colorized));
            announceResult(true);

            if (false) {
                throw new Exception(); //this will shut up jikes
            }
        } catch (Exception e) {
            announceResult(false);
            e.printStackTrace();
        }
    }

    //main (test routine)
}


//class Colorizer
