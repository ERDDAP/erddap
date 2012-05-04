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

import java.io.*;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.12 $
 */
public class Editline {
    //Aggregations (inner class!)
    private Buffer buf;

    //Members
    private TerminalIO myIO;
    private int Cursor = 0;
    private boolean InsertMode = true;
    private int lastSize = 0;
    private boolean hardwrapped = false;
    private char lastread;
    private int lastcurspos = 0;
    private boolean maskInput = false;
    private char mask = '*';

    /**
 * Creates a new Editline object.
 *
 * @param io
 */
    public Editline(TerminalIO io) {
        myIO = io;

        //allways full length
        buf = new Buffer(myIO.getColumns() - 1);
        Cursor = 0;
        InsertMode = true;
    }

    //constructor
    public int size() {
        return buf.size();
    }

    //getSize
    public String getValue() {
        return buf.toString();
    }

    //getValue
    public void setValue(String str)
        throws BufferOverflowException, IOException {
        storeSize();

        //buffer
        buf.clear();

        //cursor
        Cursor = 0;

        //screen
        myIO.moveLeft(lastSize);
        myIO.eraseToEndOfLine();
        append(str);
    }

    //setValue
    public void maskInput(boolean maskInput) {
        this.maskInput = maskInput;
    }

    /**
 *
 *
 * @param mask
 */
    public void setMask(char mask) {
        this.mask = mask;
    }

    /**
 *
 *
 * @throws IOException
 */
    public void clear() throws IOException {
        storeSize();

        //Buffer
        buf.clear();

        //Cursor
        Cursor = 0;

        //Screen
        draw();
    }

    //clear
    public String getSoftwrap() throws IndexOutOfBoundsException, IOException {
        //Wrap from Buffer
        String content = buf.toString();
        int idx = content.lastIndexOf(" ");

        if (idx == -1) {
            content = "";
        } else {
            //System.out.println("Line:softwrap:lastspace:"+idx);
            content = content.substring(idx + 1, content.length());

            //System.out.println("Line:softwrap:wraplength:"+content.length());
            //Cursor
            //remeber relative cursor pos
            Cursor = size();
            Cursor = Cursor - content.length();

            //buffer
            for (int i = 0; i < content.length(); i++) {
                buf.removeCharAt(Cursor);
            }

            //screen
            myIO.moveLeft(content.length());
            myIO.eraseToEndOfLine();

            //System.out.println("Line:softwrap:buffercontent:"+buf.toString());
        }

        return content + getLastRead();
    }

    //getSoftWrap
    public String getHardwrap() throws IndexOutOfBoundsException, IOException {
        //Buffer
        String content = buf.toString();
        content = content.substring(Cursor, content.length());

        //System.out.println("buffer:tostring:"+buf.toString()+":");
        //System.out.println("buffer:size:"+buf.size());
        int lastsize = buf.size();

        for (int i = Cursor; i < lastsize; i++) {
            buf.removeCharAt(Cursor);

            //System.out.println("buffer:removing char #"+i);
        }

        //System.out.println("buffer:tostring:"+buf.toString()+":");
        //cursor stays
        //screen
        myIO.eraseToEndOfLine();

        return content;
    }

    //getHardWrap
    private void setCharAt(int pos, char ch)
        throws IndexOutOfBoundsException, IOException {
        //buffer
        buf.setCharAt(pos, ch);

        //cursor
        //implements overwrite mode no change
        //screen
        draw();
    }

    //setCharAt
    private void insertCharAt(int pos, char ch)
        throws BufferOverflowException, IndexOutOfBoundsException, IOException {
        storeSize();

        //buffer
        buf.ensureSpace(1);
        buf.insertCharAt(pos, ch);

        //cursor adjustment (so that it stays in "same" pos)
        if (Cursor >= pos) {
            Cursor++;
        }

        //screen
        draw();
    }

    //insertCharAt
    private void removeCharAt(int pos)
        throws IndexOutOfBoundsException, IOException {
        storeSize();

        //buffer
        buf.removeCharAt(pos);

        //cursor
        if (Cursor > pos) {
            Cursor--;
        }

        //screen
        draw();
    }

    //removeChatAt
    private void insertStringAt(int pos, String str)
        throws BufferOverflowException, IndexOutOfBoundsException, IOException {
        storeSize();

        //buffer
        buf.ensureSpace(str.length());

        for (int i = 0; i < str.length(); i++) {
            buf.insertCharAt(pos, str.charAt(i));

            //Cursor
            Cursor++;
        }

        //screen
        draw();
    }

    //insertStringAt
    public void append(char ch) throws BufferOverflowException, IOException {
        storeSize();

        //buffer
        buf.ensureSpace(1);
        buf.append(ch);

        //cursor
        Cursor++;

        //screen
        if (!maskInput) {
            myIO.write(ch);
        } else {
            myIO.write(mask);
        }
    }

    //append(char)
    public void append(String str) throws BufferOverflowException, IOException {
        storeSize();

        //buffer
        buf.ensureSpace(str.length());

        for (int i = 0; i < str.length(); i++) {
            buf.append(str.charAt(i));

            //Cursor
            Cursor++;
        }

        //screen
        if (!maskInput) {
            myIO.write(str);
        } else {
            for (int i = 0; i < str.length(); i++) {
                myIO.write(mask);
            }
        }
    }

    //append(String)
    public int getCursorPosition() {
        return Cursor;
    }

    //getCursorPosition
    public void setCursorPosition(int pos) {
        if (buf.size() < pos) {
            Cursor = buf.size();
        } else {
            Cursor = pos;
        }

        //System.out.println("Editline:cursor:"+Cursor);
    }

    //setCursorPosition
    private char getLastRead() {
        return lastread;
    }

    //getLastRead
    private void setLastRead(char ch) {
        lastread = ch;
    }

    //setLastRead
    public boolean isInInsertMode() {
        return InsertMode;
    }

    //isInInsertMode
    public void setInsertMode(boolean b) {
        InsertMode = b;
    }

    //setInsertMode
    public boolean isHardwrapped() {
        return hardwrapped;
    }

    //isHardwrapped
    public void setHardwrapped(boolean b) {
        hardwrapped = b;
    }

    //setHardwrapped
    public int run() {
        try {
            int in = 0;

            do {
                //get next key
                in = myIO.read();

                //store cursorpos
                lastcurspos = Cursor;

                switch (in) {
                case TerminalIO.LEFT:

                    if (!moveLeft()) {
                        return in;
                    }

                    break;

                case TerminalIO.RIGHT:

                    if (!moveRight()) {
                        return in;
                    }

                    break;

                case TerminalIO.BACKSPACE:

                    try {
                        if (Cursor == 0) {
                            return in;
                        } else {
                            removeCharAt(Cursor - 1);
                        }
                    } catch (IndexOutOfBoundsException ioobex) {
                        myIO.bell();
                    }

                    break;

                case TerminalIO.DELETE:

                    try {
                        removeCharAt(Cursor);
                    } catch (IndexOutOfBoundsException ioobex) {
                        myIO.bell();
                    }

                    break;

                case TerminalIO.ENTER:
                case TerminalIO.UP:
                case TerminalIO.DOWN:
                case TerminalIO.TABULATOR:
                    return in;

                default:

                    try {
                        handleCharInput(in);
                    } catch (BufferOverflowException boex) {
                        setLastRead((char) in);

                        return in;
                    }
                }

                myIO.flush();
            } while (true);
        } catch (IOException ioe) {
            return TerminalIO.IOERROR;
        }
    }

    //run
    public void draw() throws IOException {
        myIO.moveLeft(lastcurspos);
        myIO.eraseToEndOfLine();

        if (!maskInput) {
            myIO.write(buf.toString());
        } else {
            for (int i = 0; i < buf.size(); i++) {
                myIO.write(mask);
            }
        }

        //adjust screen cursor hmm
        if (Cursor < buf.size()) {
            myIO.moveLeft(buf.size() - Cursor);
        }
    }

    private boolean moveRight() throws IOException {
        //cursor
        if (Cursor < buf.size()) {
            Cursor++;

            //screen
            myIO.moveRight(1);

            return true;
        } else {
            return false;
        }
    }

    private boolean moveLeft() throws IOException {
        //cursor
        if (Cursor > 0) {
            Cursor--;

            //screen
            myIO.moveLeft(1);

            return true;
        } else {
            return false;
        }
    }

    private boolean isCursorAtEnd() {
        return (Cursor == buf.size());
    }

    private void handleCharInput(int ch)
        throws BufferOverflowException, IOException {
        if (isCursorAtEnd()) {
            append((char) ch);
        } else {
            if (isInInsertMode()) {
                try {
                    insertCharAt(Cursor, (char) ch);
                } catch (BufferOverflowException ex) {
                    //ignore buffer overflow on insert
                    myIO.bell();
                }
            } else {
                setCharAt(Cursor, (char) ch);
            }
        }
    }

    private void storeSize() {
        lastSize = buf.size();
    }

    class Buffer extends CharBuffer {
        public Buffer(int size) {
            super(size);
        }
    }
}
