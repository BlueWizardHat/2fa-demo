/*
 * Copyright (C) 2014 BlueWizardHat
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.bluewizardhat.tfa.web.util;

import java.io.Serializable;

import javax.servlet.http.HttpSession;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.bluewizardhat.tfa.web.data.entities.User;
import net.bluewizardhat.tfa.web.exception.ForbiddenException;

/**
 * Session data
 *
 * @author bluewizardhat
 */
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SessionData implements Serializable {
	private static final long serialVersionUID = -5062567800589797021L;

	private static final String SESSION_KEY = "sessionData";

	/**
	 * Authenticated User
	 */
	private User user;
	/**
	 * If the User passed twofactor authentication
	 */
	private boolean passedTwofactor;
	/**
	 * Temporary holder for the Google Authenticator shared secret while setting up the authenticator app.
	 */
	private String tempGoogleSharedSecret;
	/**
	 * To provide a unique url for the QR Code
	 */
	private String qrUuid;

	public static SessionData newSession(HttpSession session) {
		synchronized (session) {
			SessionData userSession = new SessionData();
			session.setAttribute(SESSION_KEY, userSession);
			return userSession;
		}
	}

	public static SessionData from(HttpSession session) {
		synchronized (session) {
			SessionData userSession = (SessionData) session.getAttribute(SESSION_KEY);

			if (userSession == null) {
				userSession = new SessionData();
				session.setAttribute(SESSION_KEY, userSession);
			}

			return userSession;
		}
	}

	/**
	 * Throws an exception if the user is not logged in.
	 */
	public SessionData requireLoggedIn() {
		if (user == null) {
			throw new ForbiddenException("Login required");
		}

		if ((user.getGoogleSecret() != null || user.getYubicoPublicId() != null) && !passedTwofactor) {
			throw new ForbiddenException("Login required");
		}

		return this;
	}
}
