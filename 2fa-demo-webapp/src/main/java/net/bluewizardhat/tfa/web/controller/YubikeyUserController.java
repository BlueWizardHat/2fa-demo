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

package net.bluewizardhat.tfa.web.controller;

import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpSession;
import javax.transaction.Transactional;

import lombok.extern.slf4j.Slf4j;
import net.bluewizardhat.tfa.web.data.dao.UserJpaDao;
import net.bluewizardhat.tfa.web.data.entities.User;
import net.bluewizardhat.tfa.web.exception.ForbiddenException;
import net.bluewizardhat.tfa.web.util.SessionData;
import net.bluewizardhat.tfa.web.util.YubiauthFactory;
import net.bluewizardhat.yubiauth.YubiVerifyResponse;
import net.bluewizardhat.yubiauth.Yubiauth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Implements the flow to attach a Yubikey to the account.
 *
 * @author bluewizardhat
 */
@Slf4j
@RestController
@RequestMapping("/user/yubiauth")
public class YubikeyUserController extends AbstractBaseController {

	@Autowired
	private UserJpaDao userDao;

	@Autowired
	private YubiauthFactory yubiauthFactory;

	private Yubiauth yubiAuth;

	@PostConstruct
	public void initialize() {
		yubiAuth = yubiauthFactory.getYubiauth();
	}

	@Transactional
	@RequestMapping(value = "/attachYubikey", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody Map<String, String> attachYubikey(@RequestParam String yubiOtp, HttpSession session) {
		SessionData sessionData = SessionData.from(session).requireLoggedIn();
		if (sessionData.getUser().getYubicoPublicId() != null) {
			throw new ForbiddenException("Yubikey already attached");
		}

		YubiVerifyResponse verifyResponse = yubiAuth.verifyOtp(yubiOtp);
		if (verifyResponse.getVerifyStatus() == YubiVerifyResponse.VerifyStatus.OK) {
			log.debug("Yubikey {} validated for user {}", verifyResponse.getYubicoResponse().getPublicId(), sessionData.getUser());
			User user = userDao.refreshFromDb(sessionData.getUser());
			user.setYubicoPublicId(verifyResponse.getYubicoResponse().getPublicId());
			sessionData.setUser(userDao.update(user));
			sessionData.setPassedTwofactor(true);
			return authenticationSuccess(user);
		}

		log.debug("Failed to validate Yubikey for user {}: {}", sessionData.getUser(), verifyResponse);
		return failure("yubiAuth.failed").build();
	}

	/**
	 * Detach a yubikey only if the user can validate he has it
	 */
	@Transactional
	@RequestMapping(value = "/detachYubikey", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody Map<String, String> detachYubikey(@RequestParam String yubiOtp, HttpSession session) {
		SessionData sessionData = SessionData.from(session).requireLoggedIn();
		if (sessionData.getUser().getYubicoPublicId() == null) {
			throw new ForbiddenException("No Yubikey attached");
		}

		User user = userDao.refreshFromDb(sessionData.getUser());

		if (yubiAuth.verifyOtp(yubiOtp, user.getYubicoPublicId())) {
			log.debug("Yubikey {} validated for user {}", user.getYubicoPublicId(), user);
			user.setYubicoPublicId(null);
			sessionData.setUser(userDao.update(user));
			return authenticationSuccess(user);
		}

		log.debug("Failed to validate Yubikey for user {}", user);
		return failure("yubiAuth.failed").build();
	}
}
