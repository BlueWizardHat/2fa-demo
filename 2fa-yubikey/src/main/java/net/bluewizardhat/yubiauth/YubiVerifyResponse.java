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

import lombok.Value;
import lombok.Builder;

import com.yubico.client.v2.YubicoResponse;
import com.yubico.client.v2.exceptions.YubicoValidationException;
import com.yubico.client.v2.exceptions.YubicoValidationFailure;

/**
 * Response object
 *
 * @author bluewizardhat
 */
@Value
@Builder
public class YubiVerifyResponse {
	public enum VerifyStatus {
		/**
		 * The OTP verifies
		 */
		OK,
		/**
		 * The OTP failed verification
		 */
		FAILED,
		/**
		 * The OTP is not valid
		 */
		BAD_OTP,
		/**
		 * An error occured trying to communicate with Yubico servers (YubicoValidationException)
		 */
		ERROR,
		/**
		 * An error occured trying to communicate with Yubico servers (YubicoValidationFailure)
		 * Possible man-in-the-middle attack
		 */
		VALIDATION_ERROR;
	}

	/**
	 * Status of a verify
	 */
	private VerifyStatus verifyStatus;

	/**
	 * Response status object from the YubicoClient if available
	 */
	private YubicoResponse yubicoResponse;

	/**
	 * Cause of error
	 */
	private Exception errorCause;

	static YubiVerifyResponse ok(YubicoResponse yubicoResponse) {
		return builder().verifyStatus(VerifyStatus.OK).yubicoResponse(yubicoResponse).build();
	}

	static YubiVerifyResponse failed(YubicoResponse status) {
		return builder().verifyStatus(VerifyStatus.FAILED).yubicoResponse(status).build();
	}

	static YubiVerifyResponse error(YubicoValidationException errorCause) {
		return builder().verifyStatus(VerifyStatus.ERROR).errorCause(errorCause).build();
	}

	static YubiVerifyResponse error(YubicoValidationFailure errorCause) {
		return builder().verifyStatus(VerifyStatus.VALIDATION_ERROR).errorCause(errorCause).build();
	}

	static YubiVerifyResponse badOtp() {
		return builder().verifyStatus(VerifyStatus.BAD_OTP).build();
	}
}
