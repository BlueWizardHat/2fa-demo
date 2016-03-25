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

package net.bluewizardhat.googleauth.qr;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import net.bluewizardhat.googleauth.GoogleAuth;

import com.google.common.base.Throwables;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/**
 * Class to generate and write out Google Authenticator QR codes using the zxing qr code generator
 *
 * @author bluewizardhat
 */
public class GoogleQrWriter {
	/**
	 * Generates the image containing a QR Code.
	 */
	public static BufferedImage generateQr(String issuer, String accountName, String secret) {
		try {
			String qrUri = GoogleAuth.makeTimeBasedQrUri(issuer, accountName, secret);
			BitMatrix bitMatrix = new QRCodeWriter().encode(qrUri, BarcodeFormat.QR_CODE, 250, 250);
			return MatrixToImageWriter.toBufferedImage(bitMatrix);
		} catch (WriterException e) {
			throw Throwables.propagate(e);
		}
	}

	/**
	 * Writes out the image to the OutputStream. (Basically wraps {@link ImageIO#write(BufferedImage,String,OutputStream)}
	 */
	public static void writeQr(BufferedImage image, String imageFormat, OutputStream out) throws IOException {
		ImageIO.write(image, imageFormat, out);
		out.flush();
	}

}
