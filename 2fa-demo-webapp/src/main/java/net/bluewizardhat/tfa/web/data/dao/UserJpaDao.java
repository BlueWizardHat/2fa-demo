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

package net.bluewizardhat.tfa.web.data.dao;

import static org.springframework.transaction.annotation.Propagation.MANDATORY;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import net.bluewizardhat.tfa.web.data.entities.User;

/**
 * Database access for the {@link User} entity
 *
 * @author bluewizardhat
 */
@Repository
public class UserJpaDao {
	@PersistenceContext
	private EntityManager entityManager;

	@Transactional(propagation = MANDATORY)
	public void persist(User user) {
		entityManager.persist(user);
	}

	@Transactional(propagation = MANDATORY)
	public User update(User user) {
		return entityManager.merge(user);
	}

	@Transactional(propagation = MANDATORY)
	public void remove(User user) {
		entityManager.remove(user);
	}

	public User refreshFromDb(User user) {
		return entityManager.find(User.class, user.getId());
	}

	public User findByUserName(String userName) {
		List<User> users = entityManager
				.createNamedQuery(User.FIND_BY_USERNAME, User.class)
				.setParameter("userName", userName)
				// .getSingleResult() would throw an exception if not found, we don't want that
				.getResultList();

		return users.isEmpty() ? null: users.get(0);
	}
}
