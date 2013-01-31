/*
 * Copyright (c) 2009-2010 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.gde.core.scene;

import com.jme3.gde.core.util.notify.MessageType;
import com.jme3.gde.core.util.notify.NotifyUtil;
import com.jme3.util.JmeFormatter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.Callable;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import javax.swing.JButton;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;

/**
 *
 * @author normenhansen
 */
public class ApplicationLogHandler extends Handler implements Callable<JButton> {

    InputOutput io = IOProvider.getDefault().getIO("Application", true);
    JmeFormatter formatter = new JmeFormatter();
    ActionListener listener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            io.select();
        }
    };

    public ApplicationLogHandler() {
        io.setErrSeparated(true);
    }

    @Override
    public void publish(LogRecord record) {
        if (record.getLevel().equals(Level.SEVERE)) {
            Throwable thrown = record.getThrown();
            if (thrown != null) {
                NotifyUtil.error(thrown);
                thrown.printStackTrace(io.getErr());
            } else {
                NotifyUtil.show("Error", formatter.format(record), MessageType.ERROR, listener, 10000);
                io.getErr().println(formatter.formatMessage(record));
            }
        } else if (record.getLevel().equals(Level.WARNING)) {
            io.getErr().println(formatter.formatMessage(record));
            NotifyUtil.show("Warning", formatter.formatMessage(record), MessageType.WARNING, listener, 5000);
        } else if (record.getLevel().equals(Level.INFO)) {
            io.getOut().println(formatter.formatMessage(record));
        } else if (record.getLevel().intValue()>800) {
            //larger than INFO:
            NotifyUtil.show("Info", formatter.formatMessage(record), MessageType.INFO, listener, 3000);
            io.getOut().println(formatter.formatMessage(record));
        } else {
            io.getOut().println(formatter.formatMessage(record));
        }
    }

    public JButton call() throws Exception {
        return new JButton("Report");
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
        io.getOut().close();
        io.getErr().close();
    }
}
