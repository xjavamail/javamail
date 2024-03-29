/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2017 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.mail.imap.protocol;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import net.iotgw.mail.iap.ParsingException;
import net.iotgw.mail.imap.protocol.IMAPResponse;
import net.iotgw.mail.imap.protocol.Namespaces;

/**
 * Test the Namespaces class.
 */
public class NamespacesTest {
    private static final String utf8Folder = "#public\u03b1/";
    private static final String utf7Folder = "#public&A7E-/";

    /**
     * Test an example NAMESPACE response.
     */
    @Test
    public void testAll() throws Exception {
	IMAPResponse response = new IMAPResponse(
	    "* NAMESPACE ((\"\" \"/\")) " +	// personal
	    "((\"~\" \"/\")) " +		// other users
	    "((\"#shared/\" \"/\")" +		// shared
		"(\"#public/\" \"/\")" +
		"(\"#ftp/\" \"/\")" +
		"(\"#news.\" \".\"))");
	Namespaces ns = new Namespaces(response);
	assertEquals(1, ns.personal.length);
	assertEquals("", ns.personal[0].prefix);
	assertEquals('/', ns.personal[0].delimiter);
	assertEquals(1, ns.otherUsers.length);
	assertEquals("~", ns.otherUsers[0].prefix);
	assertEquals('/', ns.otherUsers[0].delimiter);
	assertEquals(4, ns.shared.length);
	assertEquals("#shared/", ns.shared[0].prefix);
	assertEquals('/', ns.shared[0].delimiter);
	assertEquals("#public/", ns.shared[1].prefix);
	assertEquals('/', ns.shared[1].delimiter);
	assertEquals("#ftp/", ns.shared[2].prefix);
	assertEquals('/', ns.shared[2].delimiter);
	assertEquals("#news.", ns.shared[3].prefix);
	assertEquals('.', ns.shared[3].delimiter);
    }

    /**
     * Test an example NAMESPACE response with unnecessary spaces.
     */
    @Test
    public void testSpaces() throws Exception {
	IMAPResponse response = new IMAPResponse(
	    "* NAMESPACE ((\"\" \"/\")) " +	// personal
	    "( ( \"~\" \"/\" ) ) " +		// other users
	    "(( \"#shared/\" \"/\" )" +		// shared
		"( \"#public/\" \"/\" )" +
		"( \"#ftp/\" \"/\" )" +
		" (\"#news.\" \".\" ))");
	Namespaces ns = new Namespaces(response);
	assertEquals(1, ns.personal.length);
	assertEquals("", ns.personal[0].prefix);
	assertEquals('/', ns.personal[0].delimiter);
	assertEquals(1, ns.otherUsers.length);
	assertEquals("~", ns.otherUsers[0].prefix);
	assertEquals('/', ns.otherUsers[0].delimiter);
	assertEquals(4, ns.shared.length);
	assertEquals("#shared/", ns.shared[0].prefix);
	assertEquals('/', ns.shared[0].delimiter);
	assertEquals("#public/", ns.shared[1].prefix);
	assertEquals('/', ns.shared[1].delimiter);
	assertEquals("#ftp/", ns.shared[2].prefix);
	assertEquals('/', ns.shared[2].delimiter);
	assertEquals("#news.", ns.shared[3].prefix);
	assertEquals('.', ns.shared[3].delimiter);
    }

    /**
     * Test a NAMESPACE response with a UTF-7 folder name.
     */
    @Test
    public void testUtf7() throws Exception {
	IMAPResponse response = new IMAPResponse(
	    "* NAMESPACE ((\"\" \"/\")) " +	// personal
	    "((\"~\" \"/\")) " +		// other users
	    "((\"#shared/\" \"/\")" +		// shared
		"(\"" + utf7Folder + "\" \"/\")" +
		"(\"#ftp/\" \"/\")" +
		"(\"#news.\" \".\"))",
	    false);
	Namespaces ns = new Namespaces(response);
	assertEquals(1, ns.personal.length);
	assertEquals("", ns.personal[0].prefix);
	assertEquals('/', ns.personal[0].delimiter);
	assertEquals(1, ns.otherUsers.length);
	assertEquals("~", ns.otherUsers[0].prefix);
	assertEquals('/', ns.otherUsers[0].delimiter);
	assertEquals(4, ns.shared.length);
	assertEquals("#shared/", ns.shared[0].prefix);
	assertEquals('/', ns.shared[0].delimiter);
	assertEquals(utf8Folder, ns.shared[1].prefix);
	assertEquals('/', ns.shared[1].delimiter);
	assertEquals("#ftp/", ns.shared[2].prefix);
	assertEquals('/', ns.shared[2].delimiter);
	assertEquals("#news.", ns.shared[3].prefix);
	assertEquals('.', ns.shared[3].delimiter);
    }

    /**
     * Test a NAMESPACE response with a UTF-8 folder name.
     */
    @Test
    public void testUtf8() throws Exception {
	IMAPResponse response = new IMAPResponse(
	    "* NAMESPACE ((\"\" \"/\")) " +	// personal
	    "((\"~\" \"/\")) " +		// other users
	    "((\"#shared/\" \"/\")" +		// shared
		"(\"" + utf8Folder + "\" \"/\")" +
		"(\"#ftp/\" \"/\")" +
		"(\"#news.\" \".\"))",
	    true);
	Namespaces ns = new Namespaces(response);
	assertEquals(1, ns.personal.length);
	assertEquals("", ns.personal[0].prefix);
	assertEquals('/', ns.personal[0].delimiter);
	assertEquals(1, ns.otherUsers.length);
	assertEquals("~", ns.otherUsers[0].prefix);
	assertEquals('/', ns.otherUsers[0].delimiter);
	assertEquals(4, ns.shared.length);
	assertEquals("#shared/", ns.shared[0].prefix);
	assertEquals('/', ns.shared[0].delimiter);
	assertEquals(utf8Folder, ns.shared[1].prefix);
	assertEquals('/', ns.shared[1].delimiter);
	assertEquals("#ftp/", ns.shared[2].prefix);
	assertEquals('/', ns.shared[2].delimiter);
	assertEquals("#news.", ns.shared[3].prefix);
	assertEquals('.', ns.shared[3].delimiter);
    }
}
