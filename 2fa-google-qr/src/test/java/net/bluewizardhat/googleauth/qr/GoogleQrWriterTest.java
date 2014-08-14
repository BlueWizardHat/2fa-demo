package net.bluewizardhat.googleauth.qr;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import org.junit.Ignore;
import org.junit.Test;

public class GoogleQrWriterTest {
	private String secret = "72NSHUOFUBTZU25KWAHUWUBUFABU34I2";

	@Test
	@Ignore
	public void testQr() throws IOException {
		//String secret = GoogleAuth.generate160BitSharedSecret();
		System.out.println(secret);
		BufferedImage image = GoogleQrWriter.generateQr("Test", "test@test.com", secret);
		File file = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString() + ".png");
		System.out.println("Generating qr code to " + file.getCanonicalPath());
		try (OutputStream out = new FileOutputStream(file)) {
			GoogleQrWriter.writeQr(image, "PNG", out);
		}
	}

}
