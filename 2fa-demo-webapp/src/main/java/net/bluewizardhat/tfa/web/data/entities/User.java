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

package net.bluewizardhat.tfa.web.data.entities;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * User entity
 *
 * @author bluewizardhat
 */
@Data
@NoArgsConstructor
@ToString(exclude = { "hashedPassword", "googleSecret" })
@EqualsAndHashCode(of = { "id", "userName" })
@Entity
@Table(name = "user_table", indexes = @Index(name = "user_table_username_key", columnList = "username", unique = true))
@NamedQueries(@NamedQuery(name = User.FIND_BY_USERNAME, query = "from User where userName = :userName"))
public class User implements Serializable {
	private static final long serialVersionUID = -3996306436865999483L;

	public static final String FIND_BY_USERNAME = "user_table.find_by_username";

	/**
	 * Database id.
	 *
	 * <p>Interestingly both the "generator" part of @GeneratedValue and @SequenceGenerator is ignored if using
	 * a mysql dialect, so this actually works with both postgres and mysql (an auto_increment field is created
	 * with mysql).
	 */
	@Id
	@GeneratedValue(generator = "users_id_generator")
	@SequenceGenerator(name = "users_id_generator", sequenceName = "users_id_seq")
	private Long id;

	/**
	 * Username/login
	 */
	@NotNull
	@Size(max = 32)
	@Column(name = "username", length = 32, nullable = false)
	private String userName;

	/**
	 * Name to display as "Logged in as &lt;displayName&gt;" when logged in
	 */
	@NotNull
	@Size(max = 64)
	@Column(name = "display_name", length = 64, nullable = false)
	private String displayName;

	/**
	 * A hashed password
	 */
	@NotNull
	@Size(max = 64)
	@Column(name = "password", length = 64, nullable = false)
	private String hashedPassword;

	/**
	 * The shared secret for Google Authenticator (if non-null the user is required to
	 * supply both password and a Google Authenticator generated code in order to log in)
	 */
	@Size(max = 32)
	@Column(name = "google_secret", length = 32)
	private String googleSecret;

	/**
	 * The public id of a Yubikey (if non-null the user is required to supply both
	 * password and a Yubikey generated code in order to log in)
	 */
	@Size(max = 12)
	@Column(name = "yubico_public_id", length = 12)
	private String yubicoPublicId;

	/**
	 * Creation time as a UNIX timestamp
	 */
	@NotNull
	@Column(name = "created", nullable = false)
	private Long created;

	/**
	 * Last time the user logged in as a UNIX timestamp
	 */
	@Column(name = "last_login")
	private Long lastLogin;

	/**
	 * Optimistic lock value used by JPA. Application code should not touch this.
	 */
	@Version
	@Column(name = "version")
	private Long version;
}
