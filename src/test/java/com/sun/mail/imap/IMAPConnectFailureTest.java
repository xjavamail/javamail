/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.mail.imap;

import java.io.*;
import java.util.Properties;
import java.net.ServerSocket;

import javax.mail.Session;
import javax.mail.Store;
import javax.mail.MessagingException;

import com.sun.mail.test.TestServer;

import net.iotgw.mail.iap.ConnectionException;
import net.iotgw.mail.util.MailConnectException;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test connect failure.
 */
public final class IMAPConnectFailureTest {

    private static final String HOST = "localhost";
    private static final int CTO = 20;

    @Test
    public void testNoServer() {
	try {
	    // verify that port is not being used
	    ServerSocket ss = new ServerSocket(0);
	    int port = ss.getLocalPort();
	    ss.close();

	    Properties properties = new Properties();
            properties.setProperty("mail.imap.host", HOST);
            properties.setProperty("mail.imap.port", "" + port);
            properties.setProperty("mail.imap.connectiontimeout", "" + CTO);
	    Session session = Session.getInstance(properties);
	    //session.setDebug(true);

	    Store store = session.getStore("imap");
	    try {
		store.connect("test", "test");
		fail("Connected!");
		// failure!
	    } catch (MailConnectException mcex) {
		// success!
		assertEquals(HOST, mcex.getHost());
		assertEquals(port, mcex.getPort());
		assertEquals(CTO, mcex.getConnectionTimeout());
	    } catch (Exception ex) {
		System.out.println(ex);
		//ex.printStackTrace();
		fail(ex.toString());
	    } finally {
		if (store.isConnected())
		    store.close();
	    }

	} catch (Exception e) {
	    e.printStackTrace();
	    fail(e.getMessage());
	}
    }

    /**
     * Test that a disconnect after issuing the CAPABILITY command
     * results in a ConnectionException.
     */
    @Test
    public void testCapabilityDisconnect() {
	TestServer server = null;
	try {
	    final IMAPHandler handler = new IMAPHandler() {
		@Override
		public void sendGreetings() throws IOException {
		    untagged("OK IMAPHandler");
		}

		@Override
		public void capability() throws IOException {
		    exit();
		}
	    };
	    server = new TestServer(handler);
	    server.start();

	    Properties properties = new Properties();
	    properties.setProperty("mail.imap.host", "localhost");
	    properties.setProperty("mail.imap.port", "" + server.getPort());
	    final Session session = Session.getInstance(properties);
	    //session.setDebug(true);

	    final Store store = session.getStore("imap");
	    try {
		store.connect("test", "test");
		fail("connect did not fail");
	    } catch (MessagingException mex) {
		// this is what we expect, now check that it was caused by
		// the right exception
		assertTrue(mex.getCause() instanceof ConnectionException);
	    } catch (Exception ex) {
		System.out.println(ex);
		//ex.printStackTrace();
		fail(ex.toString());
	    } finally {
		store.close();
	    }

	} catch (final Exception e) {
	    e.printStackTrace();
	    fail(e.getMessage());
	} finally {
	    if (server != null) {
		server.quit();
	    }
	}
    }
}
