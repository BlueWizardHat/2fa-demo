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

import lombok.extern.slf4j.Slf4j;
import net.bluewizardhat.tfa.web.data.entities.User;
import net.bluewizardhat.tfa.web.exception.ForbiddenException;
import net.bluewizardhat.tfa.web.exception.NotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.google.common.collect.ImmutableMap;

/**
 * Just provides some convenience methods and some exception handling
 *
 * @author bluewizardhat
 */
@Slf4j
public abstract class AbstractBaseController {

	protected ImmutableMap.Builder<String,String> success() {
		return ImmutableMap.<String, String>builder()
				.put("status", "success");
	}

	protected ImmutableMap.Builder<String,String> failure(String reason) {
		return ImmutableMap.<String, String>builder()
			.put("status", "failure")
			.put("reason", reason);
	}

	protected Map<String, String> authenticationSuccess(User user) {
		return success()
				.put("userName", user.getUserName())
				.put("displayName", user.getDisplayName())
				.put("googleAuth", String.valueOf(user.getGoogleSecret() != null))
				.put("yubiAuth", String.valueOf(user.getYubicoPublicId() != null))
				.build();
	}

	/**
	 * Returns a 403 Forbidden HTTP error code if a controller method throws a {@link ForbiddenException}
	 */
	@ExceptionHandler(ForbiddenException.class)
	@ResponseStatus(value = HttpStatus.FORBIDDEN, reason = "Refusing request")
	public void handleForbidden(ForbiddenException e) {
		log.debug("Refusing request: {}", e.getMessage());
	}

	/**
	 * Returns a 404 Not Found HTTP error code if a controller method throws a {@link NotFoundException}
	 */
	@ExceptionHandler(NotFoundException.class)
	@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "The requested resource was not found")
	public void handleNotFound() {
	}

	/**
	 * General exceptions.
	 */
	@ExceptionHandler(Exception.class)
	@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR, reason = "Error occured on server")
	public void handleException(Exception e) {
		log.warn("Unhandled exception occured", e);
	}
}
