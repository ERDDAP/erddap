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

import com.sshtools.j2ssh.io.*;
import com.sshtools.j2ssh.session.*;

import java.io.*;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.13 $
 */
public class TerminalIO implements PseudoTerminal {
    // implements BasicTerminalIO {

    /**  */
    public static final int EOL_CRLF = 1;

    /**  */
    public static final int EOL_CR = 2;

    /**  */
    public static final int[] HOME = { 0, 0 };

    /**  */
    public static final int IOERROR = -1; //CTRL-D beim login

    /**  */
    public static final int 
    // Positioning 10xx
    UP = 1001; //CTRL-D beim login

    /**  */
    public static final int DOWN = 1002; //CTRL-D beim login

    /**  */
    public static final int RIGHT = 1003; //CTRL-D beim login

    /**  */
    public static final int LEFT = 1004; //CTRL-D beim login

    /**  */
    public static final int STORECURSOR = 1051; //CTRL-D beim login

    /**  */
    public static final int RESTORECURSOR = 1052; //CTRL-D beim login

    /**  */
    public static final int 
    // Erasing 11xx
    EEOL = 1100; //CTRL-D beim login

    /**  */
    public static final int EBOL = 1101; //CTRL-D beim login

    /**  */
    public static final int EEL = 1103; //CTRL-D beim login

    /**  */
    public static final int EEOS = 1104; //CTRL-D beim login

    /**  */
    public static final int EBOS = 1105; //CTRL-D beim login

    /**  */
    public static final int EES = 1106; //CTRL-D beim login

    /**  */
    public static final int 
    // Escape Sequence-ing 12xx
    ESCAPE = 1200; //CTRL-D beim login

    /**  */
    public static final int BYTEMISSING = 1201; //CTRL-D beim login

    /**  */
    public static final int UNRECOGNIZED = 1202; //CTRL-D beim login

    /**  */
    public static final int 
    // Control Characters 13xx
    ENTER = 10; //CTRL-D beim login

    /**  */
    public static final int 
    //ENTER = 1300, //LF is ENTER at the moment
    TABULATOR = 1301; //CTRL-D beim login

    /**  */
    public static final int DELETE = 1302; //CTRL-D beim login

    /**  */
    public static final int BACKSPACE = 1303; //CTRL-D beim login

    /**  */
    public static final int COLORINIT = 1304; //CTRL-D beim login

    /**  */
    public static final int HANDLED = 1305; //CTRL-D beim login

    /**  */
    public static final int LOGOUTREQUEST = 1306; //CTRL-D beim login

    /**  */
    public static final int LineUpdate = 475;

    /**  */
    public static final int CharacterUpdate = 476;

    /**  */
    public static final int ScreenpartUpdate = 477;

    /**  */
    public static final int EditBuffer = 575;

    /**  */
    public static final int LineEditBuffer = 576;

    /**  */
    public static final int BEL = 7;

    /**  */
    public static final int BS = 8;

    /**  */
    public static final int DEL = 127;

    /**  */
    public static final int CR = 13;

    /**  */
    public static final int LF = 10;

    /**  */
    public static final int FCOLOR = 10001;

    /**  */
    public static final int BCOLOR = 10002;

    /**  */
    public static final int STYLE = 10003;

    /**  */
    public static final int RESET = 10004;

    /**  */
    public static final int BOLD = 1;

    /**  */
    public static final int BOLD_OFF = 22;

    /**  */
    public static final int ITALIC = 3;

    /**  */
    public static final int ITALIC_OFF = 23;

    /**  */
    public static final int BLINK = 5;

    /**  */
    public static final int BLINK_OFF = 25;

    /**  */
    public static final int UNDERLINED = 4;

    /**  */
    public static final int UNDERLINED_OFF = 24;

    //Constants

    /**  */
    public static final int BLACK = 30;

    /**  */
    public static final int RED = 31;

    /**  */
    public static final int GREEN = 32;

    /**  */
    public static final int YELLOW = 33;

    /**  */
    public static final int BLUE = 34;

    /**  */
    public static final int MAGENTA = 35;

    /**  */
    public static final int CYAN = 36;

    /**  */
    public static final int white = 37;

    /**  */
    public static final String CRLF = "\r\n";
    private Terminal terminal;
    private DataInputStream in;
    private DataOutputStream out;
    private boolean closing;
    private boolean cr;
    private boolean nl;
    private boolean acousticSignalling; //flag for accoustic signalling
    private boolean autoflush; //flag for autoflushing mode
    private int eol = EOL_CRLF;
    private int lastByte;
    private boolean uselast = false;
    private Colorizer color = Colorizer.getReference();
    private String term;
    private int cols;
    private int rows;
    private PipedInputStream masterIn;
    private PipedOutputStream masterOut;
    private InputStream slaveIn;
    private OutputStream slaveOut;
    private IOStreamConnector ios;

    //private OutputStream masterOut;
    // private OutputStream slaveOut = new SlaveOutputStream();
    public TerminalIO(InputStream in, OutputStream out, String term, int cols,
        int rows) throws IOException {
        attachStreams(in, out);
        this.term = term;
        this.rows = rows;
        this.cols = cols;
        acousticSignalling = true;
        masterOut = new PipedOutputStream();
        masterIn = new PipedInputStream(masterOut);
        autoflush = true;
        closing = false;
        cr = false;

        //set default terminal
        setDefaultTerminal();
    }

    /**
 *
 *
 * @return
 */
    public InputStream getMasterInputStream() {
        return masterIn;
    }

    /**
 *
 *
 * @param slaveIn
 */
    public void bindSlaveInputStream(InputStream slaveIn) {
        this.slaveIn = slaveIn;
        this.ios = new IOStreamConnector(slaveIn, masterOut);
    }

    /**
 *
 *
 * @param slaveOut
 */
    public void bindSlaveOutputStream(OutputStream slaveOut) {
        this.slaveOut = slaveOut;
    }

    /**
 *
 *
 * @return
 */
    public OutputStream getSlaveOutputStream() {
        return slaveOut;
    }

    /**
 *
 *
 * @return
 */
    public int getWidth() {
        return 0;
    }

    /**
 *
 *
 * @return
 */
    public int getHeight() {
        return 0;
    }

    /**
 *
 *
 * @return
 */
    public String getTerm() {
        return terminal.getName();
    }

    /**
 *
 *
 * @return
 */
    public String getEncodedTerminalModes() {
        return "";
    }

    /* public void setMasterOutputStream(OutputStream masterOut) {
     this.masterOut = masterOut;
   }*/
    public InputStream getAttachedInputStream() throws IOException {
        if (in == null) {
            throw new IOException(
                "The teminal is not attached to an InputStream");
        }

        return in;
    }

    /**
 *
 *
 * @return
 *
 * @throws IOException
 */
    public OutputStream getAttachedOutputStream() throws IOException {
        if (out == null) {
            throw new IOException(
                "The terminal is not attached to an OutputStream");
        }

        return out;
    }

    /**
 *
 */
    public void detachStreams() {
        this.in = null;
        this.out = null;
    }

    /**
 *
 *
 * @return
 */
    public int getEOL() {
        return eol;
    }

    /**
 *
 *
 * @return
 */
    public String getEOLString() {
        return ((eol == EOL_CR) ? "\r" : "\r\n");
    }

    /**
 *
 *
 * @param eol
 */
    public void setEOL(int eol) {
        this.eol = eol;
    }

    /**
 *
 *
 * @param in
 * @param out
 */
    public void attachStreams(InputStream in, OutputStream out) {
        this.in = new DataInputStream(new BufferedInputStream(in));
        this.out = new DataOutputStream(new BufferedOutputStream(out));
    }

    /**
 *
 *
 * @return
 *
 * @throws IOException
 */
    public int read() throws IOException {
        int i = stripCRSeq(rawread());

        //translate possible control sequences
        i = terminal.translateControlCharacter(i);

        if ((i > 256) && (i == ESCAPE)) {
            i = handleEscapeSequence(i);
        }

        return i;
    }

    /**
 *
 *
 * @param ch
 *
 * @throws IOException
 */
    public void write(char ch) throws IOException {
        write((byte) ch);

        if (autoflush) {
            flush();
        }
    }

    /**
 *
 *
 * @param str
 *
 * @throws IOException
 */
    public void write(String str) throws IOException {
        write((color.colorize(str, terminal.supportsSGR())).getBytes());

        if (autoflush) {
            flush();
        }
    }

    /**
 *
 *
 * @param str
 *
 * @throws IOException
 */
    public void println(String str) throws IOException {
        write(str);
        write(getEOLString().getBytes());

        if (autoflush) {
            flush();
        }
    }

    /**
 *
 *
 * @throws IOException
 */
    public void println() throws IOException {
        write(getEOLString().getBytes());

        if (autoflush) {
            flush();
        }
    }

    /**
 *
 *
 * @throws IOException
 */
    public void eraseToEndOfLine() throws IOException {
        doErase(EEOL);
    }

    /**
 *
 *
 * @throws IOException
 */
    public void eraseToBeginOfLine() throws IOException {
        doErase(EBOL);
    }

    /**
 *
 *
 * @throws IOException
 */
    public void eraseLine() throws IOException {
        doErase(EEL);
    }

    /**
 *
 *
 * @throws IOException
 */
    public void eraseToEndOfScreen() throws IOException {
        doErase(EEOS);
    }

    /**
 *
 *
 * @throws IOException
 */
    public void eraseToBeginOfScreen() throws IOException {
        doErase(EBOS);
    }

    /**
 *
 *
 * @throws IOException
 */
    public void eraseScreen() throws IOException {
        doErase(EES);
    }

    private void doErase(int funcConst) throws IOException {
        write(terminal.getEraseSequence(funcConst));

        if (autoflush) {
            flush();
        }
    }

    /**
 *
 *
 * @param direction
 * @param times
 *
 * @throws IOException
 */
    public void moveCursor(int direction, int times) throws IOException {
        write(terminal.getCursorMoveSequence(direction, times));

        if (autoflush) {
            flush();
        }
    }

    /**
 *
 *
 * @param times
 *
 * @throws IOException
 */
    public void moveLeft(int times) throws IOException {
        moveCursor(LEFT, times);
    }

    /**
 *
 *
 * @param times
 *
 * @throws IOException
 */
    public void moveRight(int times) throws IOException {
        moveCursor(RIGHT, times);
    }

    /**
 *
 *
 * @param times
 *
 * @throws IOException
 */
    public void moveUp(int times) throws IOException {
        moveCursor(UP, times);
    }

    /**
 *
 *
 * @param times
 *
 * @throws IOException
 */
    public void moveDown(int times) throws IOException {
        moveCursor(DOWN, times);
    }

    /**
 *
 *
 * @param row
 * @param col
 *
 * @throws IOException
 */
    public void setCursor(int row, int col) throws IOException {
        int[] pos = new int[2];
        pos[0] = row;
        pos[1] = col;
        write(terminal.getCursorPositioningSequence(pos));

        if (autoflush) {
            flush();
        }
    }

    /**
 *
 *
 * @throws IOException
 */
    public void homeCursor() throws IOException {
        write(terminal.getCursorPositioningSequence(HOME));

        if (autoflush) {
            flush();
        }
    }

    /**
 *
 *
 * @throws IOException
 */
    public void storeCursor() throws IOException {
        write(terminal.getSpecialSequence(STORECURSOR));
    }

    /**
 *
 *
 * @throws IOException
 */
    public void restoreCursor() throws IOException {
        write(terminal.getSpecialSequence(RESTORECURSOR));
    }

    /**
 *
 *
 * @throws IOException
 */
    public void closeInput() throws IOException {
        if (in == null) {
            throw new IOException(
                "The terminal is not attached to an inputstream");
        }

        in.close();
    }

    private int read16int() throws IOException {
        if (in == null) {
            throw new IOException(
                "The terminal is not attached to an inputstream");
        }

        return in.readUnsignedShort();
    }

    private int rawread() throws IOException {
        if (in == null) {
            throw new IOException(
                "The terminal is not attached to an inputstream");
        }

        return in.readUnsignedByte();
    }

    private int stripCRSeq(int input) throws IOException {
        if (in == null) {
            throw new IOException(
                "The terminal is not attached to an inputstream");
        }

        // Check for CR
        if (input == CR) {
            // Mark the current position and test for LF
            in.mark(1);

            // If there is more data to read, check for LF
            if (in.available() > 0) {
                int next = in.readUnsignedByte();

                // If we don't have LF then reset back to the mark position
                if (next != LF) {
                    in.reset();
                }
            }

            return TerminalIO.ENTER;
        }

        return input;
    }

    /**
 *
 *
 * @param b
 *
 * @throws IOException
 */
    public void write(byte b) throws IOException {
        if (out == null) {
            throw new IOException(
                "The terminal is not attached to an outputstream");
        }

        if (eol == EOL_CRLF) {
            if (!cr && (b == 10)) {
                out.write(13);

                //if(masterOut!=null)
                //  masterOut.write(13);
            }

            //ensure CRLF(\r\n) is written for CR(\r) to adhere
            //to the telnet protocol.
            if (cr && (b != 10)) {
                out.write(10);

                // if(masterOut!=null)
                //   masterOut.write(10);
            }

            out.write(b);

            // if(masterOut!=null)
            //   masterOut.write(b);
            if (b == 13) {
                cr = true;
            } else {
                cr = false;
            }
        } else {
            out.write(b);

            // if(masterOut!=null)
            //   masterOut.write(b);
        }
    }

    /**
 *
 *
 * @param i
 *
 * @throws IOException
 */
    public void write(int i) throws IOException {
        write((byte) i);
    }

    /**
 *
 *
 * @param sequence
 *
 * @throws IOException
 */
    public void write(byte[] sequence) throws IOException {
        for (int z = 0; z < sequence.length; z++) {
            write(sequence[z]);
        }
    }

    /**
 *
 *
 * @param sequence
 *
 * @throws IOException
 */
    public void write(int[] sequence) throws IOException {
        for (int j = 0; j < sequence.length; j++) {
            write((byte) sequence[j]);
        }
    }

    /**
 *
 *
 * @throws IOException
 */
    public void flush() throws IOException {
        if (out == null) {
            throw new IOException(
                "The terminal is not attached to an outputstream");
        }

        // If were attached then flush, else ignore
        out.flush();
    }

    /**
 *
 *
 * @throws IOException
 */
    public void closeOutput() throws IOException {
        if (out == null) {
            throw new IOException(
                "The terminal is not attached to an outputstream");
        }

        closing = true;
        out.close();
    }

    /**
 *
 *
 * @param bool
 */
    public void setSignalling(boolean bool) {
        acousticSignalling = bool;
    }

    /**
 *
 *
 * @return
 */
    public boolean isSignalling() {
        return acousticSignalling;
    }

    /**
 *
 *
 * @throws IOException
 */
    public void bell() throws IOException {
        if (acousticSignalling) {
            write(BEL);
        }

        if (autoflush) {
            flush();
        }
    }

    /**
 *
 *
 * @param topmargin
 * @param bottommargin
 *
 * @return
 *
 * @throws IOException
 */
    public boolean defineScrollRegion(int topmargin, int bottommargin)
        throws IOException {
        if (terminal.supportsScrolling()) {
            write(terminal.getScrollMarginsSequence(topmargin, bottommargin));
            flush();

            return true;
        } else {
            return false;
        }
    }

    /**
 *
 *
 * @param color
 *
 * @throws IOException
 */
    public void setForegroundColor(int color) throws IOException {
        if (terminal.supportsSGR()) {
            write(terminal.getGRSequence(FCOLOR, color));

            if (autoflush) {
                flush();
            }
        }
    }

    /**
 *
 *
 * @param color
 *
 * @throws IOException
 */
    public void setBackgroundColor(int color) throws IOException {
        if (terminal.supportsSGR()) {
            //this method adds the offset to the fg color by itself
            write(terminal.getGRSequence(BCOLOR, color + 10));

            if (autoflush) {
                flush();
            }
        }
    }

    /**
 *
 *
 * @param b
 *
 * @throws IOException
 */
    public void setBold(boolean b) throws IOException {
        if (terminal.supportsSGR()) {
            if (b) {
                write(terminal.getGRSequence(STYLE, BOLD));
            } else {
                write(terminal.getGRSequence(STYLE, BOLD_OFF));
            }

            if (autoflush) {
                flush();
            }
        }
    }

    /**
 *
 *
 * @param b
 *
 * @throws IOException
 */
    public void setUnderlined(boolean b) throws IOException {
        if (terminal.supportsSGR()) {
            if (b) {
                write(terminal.getGRSequence(STYLE, UNDERLINED));
            } else {
                write(terminal.getGRSequence(STYLE, UNDERLINED_OFF));
            }

            if (autoflush) {
                flush();
            }
        }
    }

    /**
 *
 *
 * @param b
 *
 * @throws IOException
 */
    public void setItalic(boolean b) throws IOException {
        if (terminal.supportsSGR()) {
            if (b) {
                write(terminal.getGRSequence(STYLE, ITALIC));
            } else {
                write(terminal.getGRSequence(STYLE, ITALIC_OFF));
            }

            if (autoflush) {
                flush();
            }
        }
    }

    /**
 *
 *
 * @param b
 *
 * @throws IOException
 */
    public void setBlink(boolean b) throws IOException {
        if (terminal.supportsSGR()) {
            if (b) {
                write(terminal.getGRSequence(STYLE, BLINK));
            } else {
                write(terminal.getGRSequence(STYLE, BLINK_OFF));
            }

            if (autoflush) {
                flush();
            }
        }
    }

    /**
 *
 *
 * @throws IOException
 */
    public void resetAttributes() throws IOException {
        if (terminal.supportsSGR()) {
            write(terminal.getGRSequence(RESET, 0));
        }
    }

    private int handleEscapeSequence(int i) throws IOException {
        if (i == ESCAPE) {
            int[] bytebuf = new int[terminal.getAtomicSequenceLength()];

            //fill atomic length
            //FIXME: ensure CAN, broken Escapes etc.
            for (int m = 0; m < bytebuf.length; m++) {
                bytebuf[m] = read();
            }

            return terminal.translateEscapeSequence(bytebuf);
        }

        if (i == BYTEMISSING) {
            //FIXME:longer escapes etc...
        }

        return HANDLED;
    }

    /**
 *
 *
 * @return
 */
    public boolean isAutoflushing() {
        return autoflush;
    }

    /**
 *
 *
 * @param b
 */
    public void setAutoflushing(boolean b) {
        autoflush = b;
    }

    /**
 *
 *
 * @throws IOException
 */
    public void close() throws IOException {
        closeOutput();
    }

    /**
 *
 *
 * @return
 */
    public Terminal getTerminal() {
        return terminal;
    }

    //getTerminal
    public void setDefaultTerminal() throws IOException {
        //set the terminal passing the negotiated string
        setTerminal(term);
    }

    /**
 *
 *
 * @param terminalName
 *
 * @throws IOException
 */
    public void setTerminal(String terminalName) throws IOException {
        terminal = TerminalFactory.newInstance(terminalName);

        //Terminal is set we init it....
        initTerminal();
    }

    private void initTerminal() throws IOException {
        write(terminal.getInitSequence());
        flush();
    }

    /**
 *
 *
 * @return
 */
    public int getRows() {
        return rows;
    }

    /**
 *
 *
 * @return
 */
    public int getColumns() {
        return cols;
    }
}


//class TerminalIO
