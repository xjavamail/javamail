/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2017 Oracle and/or its affiliates. All rights reserved.
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

package net.iotgw.mail.imap.protocol;

import java.io.ByteArrayInputStream;

import net.iotgw.mail.iap.*;
import net.iotgw.mail.util.ASCIIUtility;

/**
 * The RFC822 response data item.
 *
 * @author  John Mani
 * @author  Bill Shannon
 */

public class RFC822DATA implements Item {
   
    static final char[] name = {'R','F','C','8','2','2'};
    private final int msgno;
    private final ByteArray data;
    private final boolean isHeader;

    /**
     * Constructor, header flag is false.
     *
     * @param	r	the FetchResponse
     * @exception	ParsingException	for parsing failures
     */
    public RFC822DATA(FetchResponse r) throws ParsingException {
	this(r, false);
    }

    /**
     * Constructor, specifying header flag.
     *
     * @param	r	the FetchResponse
     * @param	isHeader	just header information?
     * @exception	ParsingException	for parsing failures
     */
    public RFC822DATA(FetchResponse r, boolean isHeader)
				throws ParsingException {
	this.isHeader = isHeader;
	msgno = r.getNumber();
	r.skipSpaces();
	data = r.readByteArray();
    }

    public ByteArray getByteArray() {
	return data;
    }

    public ByteArrayInputStream getByteArrayInputStream() {
	if (data != null)
	    return data.toByteArrayInputStream();
	else
	    return null;
    }

    public boolean isHeader() {
	return isHeader;
    }
}
