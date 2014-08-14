package net.bluewizardhat.googleauth;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;

public class GoogleAuthTest {
	private String secret = "72NSHUOFUBTZU25KWAHUWUBUFABU34I2";

	@Test
	public void prettifySecret_80() {
		String secret = GoogleAuth.generate80BitSharedSecret();
		String pretty = GoogleAuth.prettifySecret(secret);

		assertEquals(19, pretty.length());
	}

	@Test
	public void prettifySecret_160() {
		String secret = GoogleAuth.generate160BitSharedSecret();
		String pretty = GoogleAuth.prettifySecret(secret);

		assertEquals(39, pretty.length());
	}

	@Test
	@Ignore
	public void testCodes() {
		System.out.println(GoogleAuth.calculateTimeBasedCode(secret));
	}

}
