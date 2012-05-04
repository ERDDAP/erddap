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
package com.sshtools.daemon.session;

import com.sshtools.daemon.terminal.*;

import java.io.*;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.12 $
 */
public class PseudoTerminalWrapper {
    private InputStream masterIn;
    private OutputStream masterOut;
    private InputStream slaveIn;
    private OutputStream slaveOut;
    private String term;
    private int cols;
    private int rows;
    private int width;
    private int height;
    private String modes;
    private TerminalIO terminal;
    private UserInput ui;

    /**
 * Creates a new PseudoTerminalWrapper object.
 *
 * @param term
 * @param cols
 * @param rows
 * @param width
 * @param height
 * @param modes
 */
    public PseudoTerminalWrapper(String term, int cols, int rows, int width,
        int height, String modes) {
        this.term = term;
        this.cols = cols;
        this.rows = rows;
        this.height = height;
        this.width = width;
    }

    /**
 *
 *
 * @param masterIn
 */
    public void bindMasterInputStream(InputStream masterIn) {
        this.masterIn = masterIn;
    }

    /**
 *
 *
 * @param masterOut
 */
    public void bindMasterOutputStream(OutputStream masterOut) {
        this.masterOut = masterOut;
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
 * @param slaveIn
 */
    public void bindSlaveInputStream(InputStream slaveIn) {
        this.slaveIn = slaveIn;
    }

    /**
 *
 *
 * @throws IOException
 */
    public void initialize() throws IOException {
        this.terminal = new TerminalIO(masterIn, masterOut, term, cols, rows);
        terminal.bindSlaveInputStream(slaveIn);
        terminal.bindSlaveOutputStream(slaveOut);
        ui = new UserInput(terminal, slaveOut);
    }

    /**
 *
 *
 * @return
 */
    public InputStream getMasterInputStream() {
        return terminal.getMasterInputStream();
    }
}
