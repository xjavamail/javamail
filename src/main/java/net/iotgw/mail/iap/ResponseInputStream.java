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

package net.iotgw.mail.iap;

import java.io.*;

import net.iotgw.mail.iap.ByteArray;
import net.iotgw.mail.util.ASCIIUtility;

/**
 *
 * Inputstream that is used to read a Response.
 *
 * @author  Arun Krishnan
 * @author  Bill Shannon
 */

public class ResponseInputStream {

    private static final int minIncrement = 256;
    private static final int maxIncrement = 256 * 1024;
    private static final int incrementSlop = 16;

    // where we read from
    private BufferedInputStream bin;

    /**
     * Constructor.
     *
     * @param	in	the InputStream to wrap
     */
    public ResponseInputStream(InputStream in) {
	bin = new BufferedInputStream(in, 2 * 1024);
    }

    /**
     * Read a Response from the InputStream.
     *
     * @return		ByteArray that contains the Response
     * @exception	IOException	for I/O errors
     */
    public ByteArray readResponse() throws IOException {
	return readResponse(null);
    }

    /**
     * Read a Response from the InputStream.
     *
     * @param	ba	the ByteArray in which to store the response, or null
     * @return		ByteArray that contains the Response
     * @exception	IOException	for I/O errors
     */
    public ByteArray readResponse(ByteArray ba) throws IOException {
	if (ba == null)
	    ba = new ByteArray(new byte[128], 0, 128);

	byte[] buffer = ba.getBytes();
	int idx = 0;
	for (;;) {	// read until CRLF with no preceeding literal
	    // XXX - b needs to be an int, to handle bytes with value 0xff
	    int b = 0;
	    boolean gotCRLF=false;

	    // Read a CRLF terminated line from the InputStream
	    while (!gotCRLF &&
		   ((b =  bin.read()) != -1)) {
		if (b == '\n') {
		    if ((idx > 0) && buffer[idx-1] == '\r')
			gotCRLF = true;
		}
		if (idx >= buffer.length) {
		    int incr = buffer.length;
		    if (incr > maxIncrement)
			incr = maxIncrement;
		    ba.grow(incr);
		    buffer = ba.getBytes();
		}
		buffer[idx++] = (byte)b;
	    }

	    if (b == -1)
		throw new IOException("Connection dropped by server?");

	    // Now lets check for literals : {<digits>}CRLF
	    // Note: index needs to >= 5 for the above sequence to occur
	    if (idx < 5 || buffer[idx-3] != '}')
		break;

	    int i;
	    // look for left curly
	    for (i = idx - 4; i >= 0; i--)
		if (buffer[i] == '{')
		    break;

	    if (i < 0) // Nope, not a literal ?
		break;

	    int count = 0;
	    // OK, handle the literal ..
	    try {
		count = ASCIIUtility.parseInt(buffer, i+1, idx-3);
	    } catch (NumberFormatException e) {
		break;
	    }

	    // Now read 'count' bytes. (Note: count could be 0)
	    if (count > 0) {
		int avail = buffer.length - idx; // available space in buffer
		if (count + incrementSlop > avail) {
		    // need count-avail more bytes
		    ba.grow(minIncrement > count + incrementSlop - avail ? 
			    minIncrement : count + incrementSlop - avail);
		    buffer = ba.getBytes();
		}

		/*
		 * read() might not return all the bytes in one shot,
		 * so call repeatedly till we are done
		 */
		int actual;
		while (count > 0) {
		    actual = bin.read(buffer, idx, count);
		    if (actual == -1)
			throw new IOException("Connection dropped by server?");
		    count -= actual;
		    idx += actual;
		}
	    }
	    // back to top of loop to read until CRLF
	}
	ba.setCount(idx);
	return ba;
    }

    /**
     * How much buffered data do we have?
     *
     * @return	number of bytes available
     * @exception	IOException	if the stream has been closed
     * @since	JavaMail 1.5.4
     */
    public int available() throws IOException {
	return bin.available();
    }
}
