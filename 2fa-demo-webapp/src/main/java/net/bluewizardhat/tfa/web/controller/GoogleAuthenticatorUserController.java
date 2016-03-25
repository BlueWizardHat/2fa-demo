/*
 * Copyright (C) 2014-2016 BlueWizardHat
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

package net.bluewizardhat.tfa.web.controller;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import net.bluewizardhat.googleauth.GoogleAuth;
import net.bluewizardhat.googleauth.qr.GoogleQrWriter;
import net.bluewizardhat.tfa.web.data.dao.UserJpaDao;
import net.bluewizardhat.tfa.web.data.entities.User;
import net.bluewizardhat.tfa.web.exception.ForbiddenException;
import net.bluewizardhat.tfa.web.exception.NotFoundException;
import net.bluewizardhat.tfa.web.util.SessionData;

/**
 * Implements the flow to attach a Google Authenticator to the account.
 *
 * <p>Attaching a Google Authenticator it is a multiple step process
 * <ol>
 *    <li>The user requests attaching a Google Authenticator
 *    <li>A shared secret is generated on the server.
 *    <li>The user inputs the shared secret in his/her Google Authenticator app (manually or by scanning a QR code)
 *    <li>The user submits a sample OTP code for validation
 *    <li>If the OTP code is valid the shared secret is stored with the users credentials
 * </ol>
 *
 * This flow is necessary to ensure that the user has successfully setup Google Authenticator as otherwise we risk
 * locking the user out.
 *
 * @author bluewizardhat
 */
@Slf4j
@RestController
@RequestMapping("/user/googleauth")
public class GoogleAuthenticatorUserController extends AbstractBaseController {

	@Value("${application.name}")
	private String applicationName;

	@Autowired
	private UserJpaDao userDao;

	/**
	 * Begin the flow for attaching a Google Authenticator to the account
	 * At this point we don't actually attach the authenticator, we just generate the secret.
	 */
	@RequestMapping(value = "/requestAttachAuthenticator", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody Map<String, String> requestAttachGoogleAuth(HttpSession session) {
		SessionData sessionData = SessionData.from(session).requireLoggedIn();
		if (sessionData.getUser().getGoogleSecret() != null) {
			throw new ForbiddenException("Authenticator already attached");
		}

		log.debug("Request to attach google authenticator to user {}", sessionData.getUser());

		String secret = GoogleAuth.generate160BitSharedSecret();
		sessionData.setTempGoogleSharedSecret(secret);
		sessionData.setQrUuid(UUID.randomUUID().toString());

		return success()
				.put("sharedSecret", GoogleAuth.prettifySecret(secret))
				.put("qrUuid", sessionData.getQrUuid() + ".png")
				.build();
	}

	/**
	 * Serve an image with a QR code that can be scanned with GoogleAuthenticator
	 */
	@RequestMapping(value = "/qr/{uuid}.png", method = RequestMethod.GET, produces = MediaType.IMAGE_PNG_VALUE)
	public void serveGoogleQR(@PathVariable("uuid") String uuid, HttpServletResponse httpServletResponse, HttpSession session) throws IOException {
		SessionData sessionData = SessionData.from(session).requireLoggedIn();
		if (!uuid.equals(sessionData.getQrUuid())) {
			throw new NotFoundException();
		}
		BufferedImage image = GoogleQrWriter.generateQr(applicationName, sessionData.getUser().getUserName(), sessionData.getTempGoogleSharedSecret());
		GoogleQrWriter.writeQr(image, "PNG", httpServletResponse.getOutputStream());
	}

	/**
	 * Validate that the OTP generated by the client is valid.
	 * If the OTP is valid we can attach the GoogleAuthenticator
	 */
	@Transactional
	@RequestMapping(value = "/validateOtp", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody Map<String, String> validateGoogleOtp(@RequestParam String googleOtp, HttpSession session) {
		SessionData sessionData = SessionData.from(session).requireLoggedIn();
		if (sessionData.getUser().getGoogleSecret() != null) {
			throw new ForbiddenException("Authenticator already attached");
		}

		if (GoogleAuth.matchTimeBasedCode(sessionData.getTempGoogleSharedSecret(), googleOtp, 5)) {
			log.debug("Google Authenticator validated for user {}", sessionData.getUser());
			User user = userDao.refreshFromDb(sessionData.getUser());
			user.setGoogleSecret(sessionData.getTempGoogleSharedSecret());
			sessionData.setUser(userDao.update(user));
			sessionData.setPassedTwofactor(true);
			sessionData.setQrUuid(null);
			sessionData.setTempGoogleSharedSecret(null);
			return authenticationSuccess(user);
		}

		log.debug("Failed to validate Google Authenticator for user {}", sessionData.getUser());
		return failure("googleAuth.failed").build();
	}

	/**
	 * Detach an Authenticator only if the user can validate he has it
	 */
	@Transactional
	@RequestMapping(value = "/detachAuthenticator", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody Map<String, String> detachAuthenticator(@RequestParam String googleOtp, HttpSession session) {
		SessionData sessionData = SessionData.from(session).requireLoggedIn();
		if (sessionData.getUser().getGoogleSecret() == null) {
			throw new ForbiddenException("No Authenticator attached");
		}

		User user = userDao.refreshFromDb(sessionData.getUser());

		if (GoogleAuth.matchTimeBasedCode(user.getGoogleSecret(), googleOtp, 5)) {
			log.debug("Google Authenticator validated for user {}", user);
			user.setGoogleSecret(null);
			sessionData.setUser(userDao.update(user));
			return authenticationSuccess(user);
		}

		log.debug("Failed to validate Google Authenticator for user {}", user);
		return failure("googleAuth.failed").build();
	}
}
