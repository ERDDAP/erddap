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
 * @version $Revision: 1.11 $
 */
public interface Terminal {
    //Constants

    /**  */
    public static final byte EOT = 4;

    /**  */
    public static final byte BS = 8;

    /**  */
    public static final byte DEL = 127;

    /**  */
    public static final byte HT = 9;

    /**  */
    public static final byte FF = 12;

    /**  */
    public static final byte SGR = 1;

    /**  */
    public static final byte CAN = 24;

    /**  */
    public static final byte ESC = 27;

    /**  */
    public static final byte LSB = 91;

    /**  */
    public static final byte SEMICOLON = 59;

    /**  */
    public static final byte A = 65;

    /**  */
    public static final byte B = 66;

    /**  */
    public static final byte C = 67;

    /**  */
    public static final byte D = 68;

    /**  */
    public static final byte E = 69; // for next Line (like CR/LF)

    /**  */
    public static final byte H = 72; // for Home and Positionsetting or f

    /**  */
    public static final byte f = 102;

    /**  */
    public static final byte r = 114;

    /**  */
    public static final byte LE = 75; // K...line erase actions related

    /**  */
    public static final byte SE = 74; // J...screen erase actions related

    /**
 *
 *
 * @return
 */
    public String getName();

    /**
 *
 *
 * @param byteread
 *
 * @return
 */
    public int translateControlCharacter(int byteread);

    /**
 *
 *
 * @param buffer
 *
 * @return
 */
    public int translateEscapeSequence(int[] buffer);

    /**
 *
 *
 * @param eraseFunc
 *
 * @return
 */
    public byte[] getEraseSequence(int eraseFunc);

    /**
 *
 *
 * @param dir
 * @param times
 *
 * @return
 */
    public byte[] getCursorMoveSequence(int dir, int times);

    /**
 *
 *
 * @param pos
 *
 * @return
 */
    public byte[] getCursorPositioningSequence(int[] pos);

    /**
 *
 *
 * @param sequence
 *
 * @return
 */
    public byte[] getSpecialSequence(int sequence);

    /**
 *
 *
 * @param topmargin
 * @param bottommargin
 *
 * @return
 */
    public byte[] getScrollMarginsSequence(int topmargin, int bottommargin);

    /**
 *
 *
 * @param type
 * @param param
 *
 * @return
 */
    public byte[] getGRSequence(int type, int param);

    /**
 *
 *
 * @param str
 *
 * @return
 */
    public String format(String str);

    /**
 *
 *
 * @return
 */
    public byte[] getInitSequence();

    /**
 *
 *
 * @return
 */
    public boolean supportsSGR();

    /**
 *
 *
 * @return
 */
    public boolean supportsScrolling();

    /**
 *
 *
 * @return
 */
    public int getAtomicSequenceLength();
}


//interface Terminal
