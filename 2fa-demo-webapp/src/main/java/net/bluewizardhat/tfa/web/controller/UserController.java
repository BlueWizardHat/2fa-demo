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

import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpSession;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.ImmutableMap;

import lombok.extern.slf4j.Slf4j;
import net.bluewizardhat.googleauth.GoogleAuth;
import net.bluewizardhat.tfa.web.data.dao.UserJpaDao;
import net.bluewizardhat.tfa.web.data.entities.User;
import net.bluewizardhat.tfa.web.util.SessionData;
import net.bluewizardhat.tfa.web.util.YubiauthFactory;
import net.bluewizardhat.yubiauth.Yubiauth;

/**
 * User controller for creating users and logging in
 *
 * @author bluewizardhat
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController extends AbstractBaseController {

	@Autowired
	private UserJpaDao userDao;

	@Autowired
	private YubiauthFactory yubiauthFactory;

	private Yubiauth yubiAuth;

	@PostConstruct
	public void initialize() {
		yubiAuth = yubiauthFactory.getYubiauth();
	}

	@RequestMapping(value = "/currentUser", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody Map<String, String> currentUser(HttpSession session) {
		SessionData sessionData = SessionData.from(session);

		User user = sessionData.getUser();
		if (user != null) {
			return authenticationSuccess(user);
		}

		return failure("not.logged.in").build();
	}

	@Transactional
	@RequestMapping(value = "/createUser", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody Map<String, String> createUser(@RequestParam String userName, @RequestParam String displayName, @RequestParam String password, HttpSession session) {
		SessionData sessionData = SessionData.newSession(session);

		if (!StringUtils.hasText(userName) || !StringUtils.hasText(displayName) || !StringUtils.hasText(password)) {
			return failure("some.arguments.are.empty").build();
		}

		User user = new User().setUserName(userName.toLowerCase()).setDisplayName(displayName).setHashedPassword(hashPassword(password)).setCreated(System.currentTimeMillis());
		log.debug("Creating new user {}", user);

		// Since we are logging in the user immediately also set last login
		user.setLastLogin(System.currentTimeMillis());

		userDao.persist(user);
		sessionData.setUser(user);

		return authenticationSuccess(user);
	}

	/**
	 * Login method. The user must pass the password check. In case any of the twofactor methods are available
	 * the user must pass at least one twofactor check.
	 */
	@Transactional
	@RequestMapping(value = "/login", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody Map<String, String> login(@RequestParam String userName, @RequestParam String password, @RequestParam(required=false) String authenticatorOtp, @RequestParam(required=false) String yubiOtp, HttpSession session) {
		SessionData sessionData = SessionData.newSession(session);

		log.debug("Login attempt as userName={}", userName);
		User user = userDao.findByUserName(userName.toLowerCase());

		// User and password check
		if (user == null || !passwordMatch(user, password)) {
			// In case user or password fail don't tell the client any details
			log.debug("Login failed as userName={}", userName);
			return failure("authentication.failed").build();
		}

		// Passed the password check, now error messages can be a little more detailed

		if (user.getGoogleSecret() == null && user.getYubicoPublicId() == null) {
			user.setLastLogin(System.currentTimeMillis());
			user = userDao.update(user);
			sessionData.setUser(user);
			log.debug("Successful login by user {}; Twofactor not enabled", user);
			return authenticationSuccess(user);
		}

		if (StringUtils.hasText(authenticatorOtp) && user.getGoogleSecret() != null) {
			if (GoogleAuth.matchTimeBasedCode(user.getGoogleSecret(), authenticatorOtp, 5)) {
				user.setLastLogin(System.currentTimeMillis());
				user = userDao.update(user);
				sessionData.setUser(user).setPassedTwofactor(true);
				log.debug("Successful login using Google Authenticator; user={}", user);
				return authenticationSuccess(user);
			} else {
				// If the GoogleAuth is attempted it must pass
				log.debug("Login failed Google Authenticator validation; user={}", user);
				return authenticationFailure("googleAuth.failed", user);
			}
		}

		if (StringUtils.hasText(yubiOtp) && user.getYubicoPublicId() != null) {
			if (yubiAuth.verifyOtp(yubiOtp, user.getYubicoPublicId())) {
				user.setLastLogin(System.currentTimeMillis());
				user = userDao.update(user);
				sessionData.setUser(user).setPassedTwofactor(true);
				log.debug("Successful login using Yubikey: user={}", user);
				return authenticationSuccess(user);
			} else {
				// If the YubiAuth is attempted it must pass
				log.debug("Login failed Yubikey validation; user={}", user);
				return authenticationFailure("yubiAuth.failed", user);
			}
		}

		log.debug("Login failed; No Twofactor credentials provided; user={}", user);
		return authenticationFailure("twofactor.required", user);
	}

	@Transactional
	@RequestMapping(value = "/changePassword", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody Map<String, String> changePassword(@RequestParam String oldPassword, @RequestParam String newPassword, HttpSession session) {
		SessionData sessionData = SessionData.from(session).requireLoggedIn();

		if (!StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
			return failure("some.arguments.are.empty").build();
		}

		User user = userDao.refreshFromDb(sessionData.getUser());

		if (!passwordMatch(user, oldPassword)) {
			log.debug("Failed to change password for userName={}", user.getUserName());
			return failure("authentication.failed").build();
		}

		user.setHashedPassword(hashPassword(newPassword));
		sessionData.setUser(userDao.update(user));

		log.debug("Updated password for userName={}", user.getUserName());
		return authenticationSuccess(user);
	}

	/**
	 * Logs out by overwriting all session data.
	 */
	@RequestMapping(value = "/logout", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody Map<String, String> logout(HttpSession session) {
		SessionData.newSession(session);
		return success().build();
	}

	private Map<String, String> authenticationFailure(String reason, User user) {
		ImmutableMap.Builder<String,String> response = failure(reason);

		if (user.getGoogleSecret() != null) {
			response.put("googleAuthAvailable", "true");
		}
		if (user.getYubicoPublicId() != null) {
			response.put("yubiAuthAvailable", "true");
		}

		return response.build();
	}

	private String hashPassword(String unhashedPassword) {
		return BCrypt.hashpw(unhashedPassword, BCrypt.gensalt(12));
	}

	private boolean passwordMatch(User user, String passwordCandidate) {
		return BCrypt.checkpw(passwordCandidate, user.getHashedPassword());
	}
}
