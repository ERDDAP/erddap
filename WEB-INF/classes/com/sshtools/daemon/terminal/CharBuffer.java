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

import java.util.*;


class CharBuffer {
    //Members
    private Vector myBuffer;
    private int mySize;

    /**
 * Creates a new CharBuffer object.
 *
 * @param size
 */
    public CharBuffer(int size) {
        myBuffer = new Vector(size);
        mySize = size;
    }

    //constructor
    public char getCharAt(int pos) throws IndexOutOfBoundsException {
        return ((Character) myBuffer.elementAt(pos)).charValue();
    }

    //getCharAt
    public void setCharAt(int pos, char ch) throws IndexOutOfBoundsException {
        myBuffer.setElementAt(new Character(ch), pos);
    }

    //setCharAt
    public void insertCharAt(int pos, char ch)
        throws BufferOverflowException, IndexOutOfBoundsException {
        myBuffer.insertElementAt(new Character(ch), pos);
    }

    //insertCharAt
    public void append(char aChar) throws BufferOverflowException {
        myBuffer.addElement(new Character(aChar));
    }

    //append
    public void removeCharAt(int pos) throws IndexOutOfBoundsException {
        myBuffer.removeElementAt(pos);
    }

    //removeCharAt
    public void clear() {
        myBuffer.removeAllElements();
    }

    //clear
    public int size() {
        return myBuffer.size();
    }

    //size
    public String toString() {
        StringBuffer sbuf = new StringBuffer();

        for (int i = 0; i < myBuffer.size(); i++) {
            sbuf.append(((Character) myBuffer.elementAt(i)).charValue());
        }

        return sbuf.toString();
    }

    //toString
    public void ensureSpace(int chars) throws BufferOverflowException {
        if (chars > (mySize - myBuffer.size())) {
            throw new BufferOverflowException();
        }
    }

    //ensureSpace
}


//class CharBuffer
