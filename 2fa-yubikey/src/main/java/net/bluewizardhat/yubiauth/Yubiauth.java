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

package net.bluewizardhat.yubiauth;

import com.yubico.client.v2.ResponseStatus;
import com.yubico.client.v2.VerificationResponse;
import com.yubico.client.v2.YubicoClient;
import com.yubico.client.v2.exceptions.YubicoValidationFailure;
import com.yubico.client.v2.exceptions.YubicoVerificationException;

import lombok.AllArgsConstructor;
import lombok.NonNull;

/**
 * Wrapper around a {@link YubicoClient} for simplicity.
 *
 * <p>The Yubico API documentation is somewhat vague and throws exceptions for some validations, this class
 * simplifies the API a little and gives nice status codes instead of Yubico's exceptions.
 *
 * @author bluewizardhat
 */
@AllArgsConstructor
public class Yubiauth {
	@NonNull
	private YubicoClient client;

	/**
	 * Verifies that an OTP is valid, recognized by yubico servers and is associated with the expected yubikey public id
	 */
	public boolean verifyOtp(@NonNull String otp, @NonNull String yubicoPublicId) {
		YubiVerifyResponse verifyResponse = verifyOtp(otp);

		if (verifyResponse.getVerifyStatus() == YubiVerifyResponse.VerifyStatus.OK &&
			yubicoPublicId.equals(verifyResponse.getYubicoResponse().getPublicId())) {
				return true;
		}

		return false;
	}

	/**
	 * Verifies that an otp is valid and recognized by yubico servers.
	 */
	public YubiVerifyResponse verifyOtp(@NonNull String otp) {
		if (!YubicoClient.isValidOTPFormat(otp)) {
			return YubiVerifyResponse.badOtp();
		}

		try {
			VerificationResponse response = client.verify(otp);
			if (response.getStatus() == ResponseStatus.OK) {
				return YubiVerifyResponse.ok(response);
			}
			return YubiVerifyResponse.failed(response);
		} catch (YubicoValidationFailure e) {
			return YubiVerifyResponse.error(e);
		} catch (YubicoVerificationException e) {
			return YubiVerifyResponse.error(e);
		}
	}
}
