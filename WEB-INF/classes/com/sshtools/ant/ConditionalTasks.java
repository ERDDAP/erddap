/*
 *  SSHTools - Java SSH2 API
 *
 *  Copyright (C) 2002-2003 Lee David Painter
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
package com.sshtools.ant;

import org.apache.tools.ant.*;

import java.io.*;

import java.util.*;


public class ConditionalTasks extends Task implements TaskContainer {
    private ArrayList tasks = new ArrayList();
    private String dirs;
    private String files;
    private String name;
    private String family;

    public ConditionalTasks() {
    }

    public void setFamily(String family) {
        this.family = family;
    }

    public void setDirs(String dirs) {
        this.dirs = dirs;
    }

    public void setFiles(String files) {
        this.files = files;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addTask(Task task) {
        tasks.add(tasks.size(), task);
    }

    public void execute() {
        if ((dirs == null) && (files == null)) {
            throw new BuildException(
                "ConditionalTasks: You must supply at least one of either the files or dirs properties");
        }

        if (name == null) {
            throw new BuildException(
                "ConditionalTasks: You must supply a name for these conditional tasks!");
        }

        log("Verifying conditions for " + name);

        if (family != null) {
            StringTokenizer tokenizer = new StringTokenizer(dirs, ",");
            boolean familyMatch = false;

            while (tokenizer.hasMoreElements() && !familyMatch) {
                String condition = (String) tokenizer.nextElement();

                if (condition.equals(family)) {
                    familyMatch = true;
                }
            }

            if (!familyMatch) {
                log("ConditionalTasks: OS Family '" + family +
                    "' does not match; " + name + " will not be performed");

                return;
            }
        }

        File basedir = getProject().getBaseDir();

        if (dirs != null) {
            StringTokenizer tokenizer = new StringTokenizer(dirs, ",");
            File f;

            while (tokenizer.hasMoreElements()) {
                String condition = (String) tokenizer.nextElement();
                f = new File(basedir, condition);

                if (!f.exists()) {
                    f = new File(condition);

                    if (!f.exists()) {
                        log("ConditionalTasks: Directory '" + condition +
                            "' does not exist; " + name +
                            " will not be performed");

                        return;
                    }
                }
            }
        }

        if (files != null) {
            StringTokenizer tokenizer = new StringTokenizer(files, ",");
            File f;

            while (tokenizer.hasMoreElements()) {
                String condition = (String) tokenizer.nextElement();
                f = new File(basedir, condition);

                if (!f.exists()) {
                    log("ConditionalTasks: File '" + condition +
                        "' does not exist; " + name + " will not be performed");

                    return;
                }
            }
        }

        System.out.println("Executing Conditional Tasks");

        Iterator it = tasks.iterator();
        Task task;

        while (it.hasNext()) {
            task = (Task) it.next();
            task.setProject(getProject());
            task.setOwningTarget(getOwningTarget());
            task.setLocation(getLocation());
            task.perform();
        }
    }
}
