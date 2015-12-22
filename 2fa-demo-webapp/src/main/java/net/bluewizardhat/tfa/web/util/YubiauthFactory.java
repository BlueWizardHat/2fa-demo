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

package net.bluewizardhat.tfa.web.util;

import javax.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;
import net.bluewizardhat.yubiauth.Yubiauth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.yubico.client.v2.YubicoClient;

/**
 * Instantiates and configures a {@link YubicoClient}
 *
 * @author bluewizardhat
 */
@Slf4j
@Component
public class YubiauthFactory {
	private Yubiauth yubiAuth;

	@Value("${yubico.clientId}")
	private Integer clientId;

	@Value("${yubico.apikey}")
	private String apiKey;

	@PostConstruct
	public void initialize() {
		YubicoClient yubicoClient = YubicoClient.getClient(clientId, apiKey);
		// could set up more stuff with the client here .. for example override the default validation servers or other stuff
		yubiAuth = new Yubiauth(yubicoClient);

		log.debug("YubicoClient initialized, clientId={}, apiKey={}", clientId, apiKey);
	}

	public Yubiauth getYubiauth() {
		return yubiAuth;
	}
}
