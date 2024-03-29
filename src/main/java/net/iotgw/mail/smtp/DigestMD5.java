/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2018 Oracle and/or its affiliates. All rights reserved.
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

package net.iotgw.mail.smtp;

import java.io.*;
import java.util.*;
import java.util.logging.Level;

import net.iotgw.mail.util.ASCIIUtility;
import net.iotgw.mail.util.BASE64DecoderStream;
import net.iotgw.mail.util.BASE64EncoderStream;
import net.iotgw.mail.util.MailLogger;

import java.security.*;
import java.nio.charset.StandardCharsets;

/**
 * DIGEST-MD5 authentication support.
 *
 * @author Dean Gibson
 * @author Bill Shannon
 */

public class DigestMD5 {

    private MailLogger logger;
    private MessageDigest md5;
    private String uri;
    private String clientResponse;

    public DigestMD5(MailLogger logger) {
	this.logger = logger.getLogger(this.getClass(), "DEBUG DIGEST-MD5");
	logger.config("DIGEST-MD5 Loaded");
    }

    /**
     * Return client's authentication response to server's challenge.
     *
     * @param	host	the host name
     * @param	user	the user name
     * @param	passwd	the user's password
     * @param	realm	the security realm
     * @param	serverChallenge	the challenge from the server
     * @return byte array with client's response
     * @exception	IOException	for I/O errors
     */
    public byte[] authClient(String host, String user, String passwd,
				String realm, String serverChallenge)
				throws IOException {
	ByteArrayOutputStream bos = new ByteArrayOutputStream();
	OutputStream b64os = new BASE64EncoderStream(bos, Integer.MAX_VALUE);
	SecureRandom random;
	try {
	    //random = SecureRandom.getInstance("SHA1PRNG");
	    random = new SecureRandom();
	    md5 = MessageDigest.getInstance("MD5");
	} catch (NoSuchAlgorithmException ex) {
	    logger.log(Level.FINE, "NoSuchAlgorithmException", ex);
	    throw new IOException(ex.toString());
	}
	StringBuilder result = new StringBuilder();

	uri = "smtp/" + host;
	String nc = "00000001";
	String qop = "auth";
	byte[] bytes = new byte[32];	// arbitrary size ...
	int resp;

	logger.fine("Begin authentication ...");

	// Code based on http://www.ietf.org/rfc/rfc2831.txt
	Map<String, String> map = tokenize(serverChallenge);

	if (realm == null) {
	    String text = map.get("realm");
	    realm = text != null ? new StringTokenizer(text, ",").nextToken()
				 : host;
	}

	// server challenge random value
	String nonce = map.get("nonce");

	// Does server support UTF-8 usernames and passwords?
	String charset = map.get("charset");
	boolean utf8 = charset != null && charset.equalsIgnoreCase("utf-8");

	random.nextBytes(bytes);
	b64os.write(bytes);
	b64os.flush();

	// client challenge random value
	String cnonce = bos.toString("iso-8859-1");	// really ASCII?
	bos.reset();

	// DIGEST-MD5 computation, common portion (order critical)
	if (utf8) {
	    String up = user + ":" + realm + ":" + passwd;
	    md5.update(md5.digest(up.getBytes(StandardCharsets.UTF_8)));
	} else
	    md5.update(md5.digest(
		ASCIIUtility.getBytes(user + ":" + realm + ":" + passwd)));
	md5.update(ASCIIUtility.getBytes(":" + nonce + ":" + cnonce));
	clientResponse = toHex(md5.digest())
		+ ":" + nonce  + ":" + nc + ":" + cnonce + ":" + qop + ":";
	
	// DIGEST-MD5 computation, client response (order critical)
	md5.update(ASCIIUtility.getBytes("AUTHENTICATE:" + uri));
	md5.update(ASCIIUtility.getBytes(clientResponse + toHex(md5.digest())));

	// build response text (order not critical)
	result.append("username=\"" + user + "\"");
	result.append(",realm=\"" + realm + "\"");
	result.append(",qop=" + qop);
	result.append(",nc=" + nc);
	result.append(",nonce=\"" + nonce + "\"");
	result.append(",cnonce=\"" + cnonce + "\"");
	result.append(",digest-uri=\"" + uri + "\"");
	if (utf8)
	    result.append(",charset=\"utf-8\"");
	result.append(",response=" + toHex(md5.digest()));

	if (logger.isLoggable(Level.FINE))
	    logger.fine("Response => " + result.toString());
	b64os.write(ASCIIUtility.getBytes(result.toString()));
	b64os.flush();
	return bos.toByteArray();
    }

    /**
     * Allow the client to authenticate the server based on its
     * response.
     *
     * @param	serverResponse	the response that was received from the server
     * @return	true if server is authenticated
     * @exception	IOException	for character conversion failures
     */
    public boolean authServer(String serverResponse) throws IOException {
	Map<String, String> map = tokenize(serverResponse);
	// DIGEST-MD5 computation, server response (order critical)
	md5.update(ASCIIUtility.getBytes(":" + uri));
	md5.update(ASCIIUtility.getBytes(clientResponse + toHex(md5.digest())));
	String text = toHex(md5.digest());
	if (!text.equals(map.get("rspauth"))) {
	    if (logger.isLoggable(Level.FINE))
		logger.fine("Expected => rspauth=" + text);
	    return false;	// server NOT authenticated by client !!!
	}
	return true;
    }

    /**
     * Tokenize a response from the server.
     *
     * @return	Map containing key/value pairs from server
     */
    @SuppressWarnings("fallthrough")
    private Map<String, String> tokenize(String serverResponse)
	    throws IOException {
	Map<String, String> map	= new HashMap<>();
	byte[] bytes = serverResponse.getBytes("iso-8859-1");	// really ASCII?
	String key = null;
	int ttype;
	StreamTokenizer	tokens
		= new StreamTokenizer(
		    new InputStreamReader(
		      new BASE64DecoderStream(
			new ByteArrayInputStream(bytes, 4, bytes.length - 4)
		      ), "iso-8859-1"	// really ASCII?
		    )
		  );

	tokens.ordinaryChars('0', '9');	// reset digits
	tokens.wordChars('0', '9');	// digits may start words
	while ((ttype = tokens.nextToken()) != StreamTokenizer.TT_EOF) {
	    switch (ttype) {
	    case StreamTokenizer.TT_WORD:
		if (key == null) {
		    key = tokens.sval;
		    break;
		}
		// fall-thru
	    case '"':
		if (logger.isLoggable(Level.FINE))
		    logger.fine("Received => " +
			 	 key + "='" + tokens.sval + "'");
		if (map.containsKey(key)) {  // concatenate multiple values
		    map.put(key, map.get(key) + "," + tokens.sval);
		} else {
		    map.put(key, tokens.sval);
		}
		key = null;
		break;
	    default:	// XXX - should never happen?
		break;
	    }
	}
	return map;
    }

    private static char[] digits = {
	'0', '1', '2', '3', '4', '5', '6', '7',
	'8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    /**
     * Convert a byte array to a string of hex digits representing the bytes.
     */
    private static String toHex(byte[] bytes) {
	char[] result = new char[bytes.length * 2];

	for (int index = 0, i = 0; index < bytes.length; index++) {
	    int temp = bytes[index] & 0xFF;
	    result[i++] = digits[temp >> 4];
	    result[i++] = digits[temp & 0xF];
	}
	return new String(result);
    }
}
