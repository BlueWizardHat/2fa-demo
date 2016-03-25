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

package net.bluewizardhat.googleauth;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import lombok.NonNull;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.io.BaseEncoding;

/**
 * Class to generate and match OTP codes compatible with <a href="http://en.wikipedia.org/wiki/Google_Authenticator">Google Authenticator</a>.
 *
 * @author bluewizardhat
 */
public class GoogleAuth {

	private static SecureRandom random = new SecureRandom();

	/**
	 * Utility class. Don't instantiate it.
	 */
	private GoogleAuth() { }

	/**
	 * Generates an 80-bit shared secret and returns a string that can be used as input when setting up a new account
	 * in the Google Authenticator app. In general {@link #generate160BitSharedSecret} should be used instead of this.
	 * @see #prettifySecret
	 * @see #generate160BitSharedSecret
	 */
	public static String generate80BitSharedSecret() {
		byte[] secret = new byte[10];
		random.nextBytes(secret);
		return BaseEncoding.base32().encode(secret);
	}

	/**
	 * Generates an 160-bit shared secret and returns a string that can be used as input when setting up a new account
	 * in the Google Authenticator app.
	 * @see #prettifySecret
	 */
	public static String generate160BitSharedSecret() {
		byte[] secret = new byte[20];
		random.nextBytes(secret);
		return BaseEncoding.base32().encode(secret);
	}

	/**
	 * Makes the shared secret looks more pretty to humans and thus easier to type. Google Authenticator is compatible with
	 * both the pretty and non-pretty format and so is this class.
	 */
	public static String prettifySecret(@NonNull String secret) {
		int length = secret.length();
		if (length % 4 != 0) {
			throw new IllegalArgumentException("secret should be divisible by 4");
		}

		StringBuilder buf = new StringBuilder((5 * (length / 4)) - 1);
		int index = 0;
		while (index < length) {
			if (index != 0) {
				buf.append(' ');
			}
			buf.append(secret.substring(index, index + 4).toLowerCase());
			index += 4;
		}
		return buf.toString();
	}

	/**
	 * Matches a counter based code
	 * @param secret the shared secret
	 * @param code code to match
	 * @param counter current counter
	 */
	public static boolean matchCounterBasedCode(@NonNull String secret, @NonNull String code, long counter) {
		String expectedCode = calculateCounterBasedCode(secret, counter);
		return expectedCode.equals(code);
	}

	/**
	 * Matches a time based code, not allowing for drift.
	 * @param secret the shared secret
	 * @param code code to match
	 */
	public static boolean matchTimeBasedCode(@NonNull String secret, @NonNull String code) {
		return matchTimeBasedCode(secret, code, 0);
	}

	/**
	 * Matches a time based code, allowing for a few seconds of drift. It is recommended to only allow a few seconds drift.
	 * @param secret the shared secret
	 * @param code code to match
	 * @param secondsDrift how many seconds of drift is allowed, must be between 0 and 29 (inclusive).
	 */
	public static boolean matchTimeBasedCode(@NonNull String secret, @NonNull String code, int secondsDrift) {
		if (secondsDrift < 0 || secondsDrift > 29) {
			throw new IllegalArgumentException("secondsDrift must be between 0 and 29");
		}

		long currentTimeMillis = System.currentTimeMillis();
		long counter = currentTimeMillis / 30000;

		if (matchCounterBasedCode(secret, code, counter)) {
			return true;
		}
		if (secondsDrift == 0) {
			return false;
		}

		long millisDrift = secondsDrift * 1000;

		// try to match against a clock that's ahead of this system's clock
		counter = (currentTimeMillis + millisDrift) / 30000;
		if (matchCounterBasedCode(secret, code, counter)) {
			return true;
		}

		// try to match against a clock that's behind this system's clock
		counter = (currentTimeMillis - millisDrift) / 30000;
		return matchCounterBasedCode(secret, code, counter);
	}

	/**
	 * Calculates a time based code, based on the current system time.
	 * @param secret the shared secret
	 */
	public static String calculateTimeBasedCode(@NonNull String secret) {
		long counter = System.currentTimeMillis() / 30000;
		return calculateCounterBasedCode(secret, counter);
	}

	/**
	 * Calculate a counter based code.
	 * @param secret the shared secret
	 * @param counter current counter
	 */
	public static String calculateCounterBasedCode(@NonNull String secret, long counter) {
		byte[] key = BaseEncoding.base32().decode(secret.replace(" ", "").toUpperCase());
		byte[] hash = hmacSha1(key, counter);
		int offset = hash[hash.length - 1] & 0x0f;
		byte[] truncatedHash = Arrays.copyOfRange(hash, offset, offset + 4);
		truncatedHash[0] = (byte) (truncatedHash[0] & 0x7f);
		int code = ByteBuffer.allocate(4).put(truncatedHash).getInt(0) % 1000000;
		return new DecimalFormat("000000").format(code);
	}

	/**
	 * Generates an URI that can be used with your favorite QR encoding software to generate
	 * a QR code that the Google Authenticator app will accept. The format is documented
	 * <a href="https://code.google.com/p/google-authenticator/wiki/KeyUriFormat">here</a>.
	 * Note that Google Authenticator will overwrite secrets with the same issuerName and acccountName
	 * so choose something relatively unique.
	 * @param issuerName name of issuer (eg. company or product name)
	 * @param accountName name of the account, usually the login or email
	 * @param secret shared secret generated by {@link #generateSharedSecret}
	 */
	public static String makeTimeBasedQrUri(@NonNull String issuerName, @NonNull String accountName, @NonNull String secret) {
		return makeQrUri("totp", issuerName, accountName, secret).toString();
	}

	/**
	 * Generates an URI that can be used with your favorite QR encoding software to generate
	 * a QR code that the Google Authenticator app will accept. The format is documented
	 * <a href="https://code.google.com/p/google-authenticator/wiki/KeyUriFormat">here</a>.
	 * Note that Google Authenticator will overwrite secrets with the same issuerName and acccountName
	 * so choose something relatively unique.
	 * @param issuerName name of issuer (eg. company or product name)
	 * @param accountName name of the account, usually the login or email
	 * @param secret shared secret generated by {@link #generateSharedSecret}
	 * @param counterValue value of the counter
	 */
	public static String makeCounterBasedQrUri(@NonNull String issuerName, @NonNull String accountName, @NonNull String secret, long counterValue) {
		return makeQrUri("hotp", issuerName, accountName, secret).append("&counter=").append(counterValue).toString();
	}

	private static StringBuilder makeQrUri(String type, String issuerName, String accountName, String secret) {
		if (issuerName.contains(":")) {
			throw new IllegalArgumentException("issuerName may not contain colon");
		}
		if (accountName.contains(":")) {
			throw new IllegalArgumentException("accountName may not contain colon");
		}
		try {
			return new StringBuilder("otpauth://").append(type).append('/')
				.append(encode(issuerName)).append(encode(":"))
				.append(encode(accountName)).append("?secret=").append(encode(secret.replace(" ", "").toUpperCase()))
				.append("&issuer=").append(encode(issuerName));

		} catch (UnsupportedEncodingException e) {
			throw Throwables.propagate(e);
		}
	}

	private static String encode(String s) throws UnsupportedEncodingException {
		return URLEncoder.encode(s, Charsets.UTF_8.name()).replace("+", "%20");
	}

	private static byte[] hmacSha1(byte[] key, long counter) {
		try {
			Mac mac = Mac.getInstance("HmacSHA1");
			mac.init(new SecretKeySpec(key, "HmacSHA1"));
			mac.update(ByteBuffer.allocate(8).putLong(counter).array());
			return mac.doFinal();
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			throw Throwables.propagate(e);
		}
	}
}
