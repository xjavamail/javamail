/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
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

import javax.mail.internet.ParameterList;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import net.iotgw.mail.iap.Response;
import net.iotgw.mail.imap.protocol.BODYSTRUCTURE;
import net.iotgw.mail.imap.protocol.FetchResponse;
import net.iotgw.mail.imap.protocol.IMAPResponse;

/**
 * Test the BODYSTRUCTURE class.
 */
public class BODYSTRUCTURETest {
    /**
     * Test workaround for Exchange bug that returns NIL instead of ""
     * for a parameter with an empty value (name="").
     */
    @Test
    public void testExchangeEmptyParameterValueBug() throws Exception {
	IMAPResponse response = new IMAPResponse(
    "* 3 FETCH (BODYSTRUCTURE ((\"text\" \"plain\" (\"charset\" \"UTF-8\") " +
    "NIL NIL \"quoted-printable\" 512 13 NIL (\"inline\" NIL) NIL NIL)" +
    "(\"text\" \"html\" (\"charset\" \"UTF-8\") NIL NIL \"quoted-printable\" " +
    "784 11 NIL (\"inline\" NIL) NIL NIL) \"alternative\" " +
    "(\"boundary\" \"__139957996218379.example.com\" \"name\" NIL) NIL NIL))");
    // here's the incorrect NIL that should be "" ............^
	FetchResponse fr = new FetchResponse(response);
	BODYSTRUCTURE bs = fr.getItem(BODYSTRUCTURE.class);
	ParameterList p = bs.cParams;
	assertNotNull(p.get("name"));
    }

    /**
     * Test workaround for Exchange bug that returns the Content-Description
     * header value instead of the Content-Disposition for some kinds of
     * (formerly S/MIME encrypted?) messages.
     */
    @Test
    public void testExchangeBadDisposition() throws Exception {
	IMAPResponse response = new IMAPResponse(
    "* 1 FETCH (BODYSTRUCTURE (" +
	"(\"text\" \"plain\" (\"charset\" \"us-ascii\") NIL NIL \"7bit\" " +
	    "21 0 NIL (\"inline\" NIL) NIL NIL)" +
	"(\"application\" \"octet-stream\" (\"name\" \"private.txt\") " +
	    "NIL NIL \"base64\" 690 NIL " +
		"(\"attachment\" (\"filename\" \"private.txt\")) NIL NIL) " +
    "\"mixed\" (\"boundary\" \"----=_Part_0_-1731707885.1504253815584\") " +
	"\"S/MIME Encrypted Message\" NIL))");
    //    ^^^^^^^ here's the string that should be the disposition
	FetchResponse fr = new FetchResponse(response);
	BODYSTRUCTURE bs = fr.getItem(BODYSTRUCTURE.class);
	assertEquals("S/MIME Encrypted Message", bs.description);
    }
}
