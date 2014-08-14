
(function ($, ko) {
	'use strict';

	// Define logging methods if they do not already exists
	if (!window.console) {
		var names = ["log", "debug", "info", "warn", "error", "assert", "dir", "dirxml",
			"group", "groupEnd", "time", "timeEnd", "count", "trace", "profile", "profileEnd"];
		window.console = {};
		var doNothing = function() {};
		for (var i = 0; i < names.length; ++i) {
			window.console[names[i]] = doNothing;
		}
	}

	function emptyString(s) {
		return !s || s.trim() === "";
	}

	var viewModel = {
		// Which parts of the flow to display ('login', 'userInfo', 'attachGoogle', 'attachYubi')
		pageFlow: ko.observable(),

		errorState: ko.observable(false),
		errorMsg: ko.observable(),

		formValidation: ko.observable(false),

		userInfo: {
			loggedIn: ko.observable(false),
			userName: ko.observable(),
			displayName: ko.observable(),
			googleAuth: ko.observable(false),
			yubiAuth: ko.observable(false),
			successMsg: ko.observable()
		},

		loginPage: {
			creatingUser: ko.observable(false),
			userName: ko.observable(),
			displayName: ko.observable(),
			password: ko.observable(),
			password2: ko.observable(),

			twofactorSelector: ko.observable(),
			authenticatorAvailable: ko.observable(false),
			authenticatorOtp: ko.observable(),
			yubiAvailable: ko.observable(false),
			yubiOtp: ko.observable()
		},

		changePwPage: {
			oldPassword: ko.observable(),
			newPassword: ko.observable(),
			newPassword2: ko.observable()
		},

		attachGooglePage: {
			sharedSecret: ko.observable(),
			qrName: ko.observable(),
			detach: ko.observable(false),

			showSecret: ko.observable(false),
			googleOtp: ko.observable()
		},

		attachYubiPage: {
			detach: ko.observable(false),
			yubiOtp: ko.observable()
		}

	};

	// loginPage validation rules
	viewModel.loginPage.userNameValidates = ko.computed(function () {
		return !viewModel.formValidation() || !emptyString(viewModel.loginPage.userName());
	});
	viewModel.loginPage.displayNameValidates = ko.computed(function () {
		return !viewModel.formValidation() || !emptyString(viewModel.loginPage.displayName());
	});
	viewModel.loginPage.passwordValidates = ko.computed(function () {
		return !viewModel.formValidation() || !emptyString(viewModel.loginPage.password());
	});
	viewModel.loginPage.password2Validates = ko.computed(function () {
		return !viewModel.formValidation() || viewModel.loginPage.password() === viewModel.loginPage.password2();
	});
	viewModel.loginPage.authenticatorOtpValidates = ko.computed(function () {
		return !viewModel.formValidation() || viewModel.loginPage.twoFactorType() !== 'authenticator' ||  !emptyString(viewModel.loginPage.authenticatorOtp());
	});
	viewModel.loginPage.yubiOtpValidates = ko.computed(function () {
		return !viewModel.formValidation() || viewModel.loginPage.twoFactorType() !== 'yubikey' || !emptyString(viewModel.loginPage.yubiOtp());
	});

	viewModel.loginPage.twofactorRequired = ko.computed(function () {
		return viewModel.loginPage.authenticatorAvailable() || viewModel.loginPage.yubiAvailable();
	});
	viewModel.loginPage.twofactorBoth = ko.computed(function () {
		return viewModel.loginPage.authenticatorAvailable() && viewModel.loginPage.yubiAvailable();
	});
	viewModel.loginPage.twoFactorType = ko.computed(function () {
		if (viewModel.loginPage.twofactorBoth()) {
			return viewModel.loginPage.twofactorSelector();
		} else if (viewModel.loginPage.authenticatorAvailable()) {
			return "authenticator";
		} else if (viewModel.loginPage.yubiAvailable()) {
			return "yubikey";
		}
	});

	// changePwPage validation rules
	viewModel.changePwPage.oldPasswordValidates = ko.computed(function () {
		return !viewModel.formValidation() || !emptyString(viewModel.changePwPage.oldPassword());
	});
	viewModel.changePwPage.newPasswordValidates = ko.computed(function () {
		return !viewModel.formValidation() || !emptyString(viewModel.changePwPage.newPassword());
	});
	viewModel.changePwPage.newPassword2Validates = ko.computed(function () {
		return !viewModel.formValidation() || viewModel.changePwPage.newPassword() === viewModel.changePwPage.newPassword2();
	});

	// attachGooglePage validation rules
	viewModel.attachGooglePage.googleOtpValidates = ko.computed(function () {
		return !viewModel.formValidation() || !emptyString(viewModel.attachGooglePage.googleOtp());
	});
	// attachYubiPage validation rules
	viewModel.attachYubiPage.yubiOtpValidates = ko.computed(function () {
		return !viewModel.formValidation() || !emptyString(viewModel.attachYubiPage.yubiOtp());
	});

	// if the pageFlow changes clear errors
	viewModel.pageFlow.subscribe(function () {
		console.log('PageFlow changed:', viewModel.pageFlow());
		viewModel.errorState(false);
		viewModel.formValidation(false);
		viewModel.userInfo.successMsg('');
	});

	function ajax(method, url, data) {
		viewModel.errorState(false);
		return $.ajax({
			type: method,
			url: url,
			data: data,
			cache: false
		}).error(function (event, request, settings) {
			console.log("ajax error", event, request, settings);
			viewModel.errorState(true);
			viewModel.errorMsg("Server error!");
		});
	}

	function clearUserInfo() {
		viewModel.userInfo.loggedIn(false);
		viewModel.userInfo.userName('');
		viewModel.userInfo.displayName('');
		viewModel.userInfo.googleAuth(false);
		viewModel.userInfo.yubiAuth(false);
	}
	function clearLoginPage () {
		viewModel.formValidation(false);
		viewModel.loginPage.creatingUser(false),
		viewModel.loginPage.userName(''),
		viewModel.loginPage.displayName(''),
		viewModel.loginPage.password(''),
		viewModel.loginPage.password2(''),
		viewModel.loginPage.twofactorSelector(''),
		viewModel.loginPage.authenticatorAvailable(false);
		viewModel.loginPage.authenticatorOtp(undefined),
		viewModel.loginPage.yubiAvailable(false);
		viewModel.loginPage.yubiOtp(undefined)
	}
	function authenticatedSuccess(data) {
		viewModel.userInfo.loggedIn(true);
		viewModel.userInfo.userName(data.userName);
		viewModel.userInfo.displayName(data.displayName);
		viewModel.userInfo.googleAuth(data.googleAuth === "true");
		viewModel.userInfo.yubiAuth(data.yubiAuth === "true");
		viewModel.pageFlow("userInfo");
		clearLoginPage();
	}

	viewModel.toggleCreateUser = function () {
		viewModel.formValidation(false);
		viewModel.loginPage.creatingUser(!viewModel.loginPage.creatingUser());
	};
	viewModel.loginCancel = function () {
		clearLoginPage();
	};

	viewModel.createNewUser = function () {
		viewModel.errorState(false);
		viewModel.formValidation(true);

		if (viewModel.loginPage.userNameValidates()
				&& viewModel.loginPage.displayNameValidates()
				&& viewModel.loginPage.passwordValidates()
				&& viewModel.loginPage.password2Validates()) {
			var url = "user/createUser";
			var data = {
				userName: viewModel.loginPage.userName(),
				displayName: viewModel.loginPage.displayName(),
				password: viewModel.loginPage.password()
			};

			console.log("createNewUser: posting to url:", url, data);
			ajax("POST", url, data).success(function (data) {
				console.log("received data", data);
				if (data.status === "success") {
					authenticatedSuccess(data);
					viewModel.userInfo.successMsg("User successfully created");
				} else {
					viewModel.errorMsg("Error creating user");
					viewModel.errorState(true);
				}
			});
		}
	};

	viewModel.login = function () {
		viewModel.errorState(false);
		viewModel.formValidation(true);
		if (viewModel.loginPage.userNameValidates()	&& viewModel.loginPage.passwordValidates()
			&& viewModel.loginPage.authenticatorOtpValidates() && viewModel.loginPage.yubiOtpValidates()) {
			var url = "user/login";
			var data = {
				userName: viewModel.loginPage.userName(),
				password: viewModel.loginPage.password(),
				authenticatorOtp: viewModel.loginPage.authenticatorOtp(),
				yubiOtp: viewModel.loginPage.yubiOtp()
			};

			console.log("login: posting to url:", url, data);
			ajax("POST", url, data).success(function (data) {
				console.log("received data", data);
				if (data.status === "success") {
					authenticatedSuccess(data);
				} else if (data.reason === "twofactor.required") {
					viewModel.formValidation(false);
					viewModel.loginPage.authenticatorAvailable(data.googleAuthAvailable === "true");
					viewModel.loginPage.yubiAvailable(data.yubiAuthAvailable === "true");
				} else {
					viewModel.errorMsg("Login failed");
					viewModel.errorState(true);
				}
			});
		}
	};

	viewModel.logout = function () {
		viewModel.errorState(false);
		var url = "user/logout";
		ajax("GET", url);
		viewModel.pageFlow("login");
		clearUserInfo();
	}

	viewModel.requestChangePassword = function () {
		viewModel.changePwPage.oldPassword('');
		viewModel.changePwPage.newPassword('');
		viewModel.changePwPage.newPassword2('');
		viewModel.pageFlow("changepw");
	};

	viewModel.changePassword = function () {
		viewModel.errorState(false);
		viewModel.formValidation(true);
		if (viewModel.changePwPage.oldPasswordValidates() && viewModel.changePwPage.newPasswordValidates() && viewModel.changePwPage.newPassword2Validates()) {
			var url = "user/changePassword";
			var data = {
				oldPassword: viewModel.changePwPage.oldPassword(),
				newPassword: viewModel.changePwPage.newPassword()
			}
			ajax("POST", url, data).success(function (data) {
				console.log("received data", data);
				if (data.status === "success") {
					authenticatedSuccess(data);
					viewModel.userInfo.successMsg("Password successfully changed");
				} else {
					viewModel.errorMsg("Password did not match");
					viewModel.errorState(true);
				}
			});
		}
	};

	viewModel.changePasswordCancel = function () {
		viewModel.changePwPage.oldPassword('');
		viewModel.changePwPage.newPassword('');
		viewModel.changePwPage.newPassword2('');
		viewModel.pageFlow("userInfo");
	};

	viewModel.requestAttachGoogle = function () {
		viewModel.attachGooglePage.detach(false);
		var url = "user/googleauth/requestAttachAuthenticator";
		console.log("login: posting to url:", url);
		ajax("POST", url).success(function (data) {
			console.log("received data", data);
			if (data.status === "success") {
				viewModel.attachGooglePage.sharedSecret(data.sharedSecret);
				viewModel.attachGooglePage.qrName(data.qrUuid);
				viewModel.pageFlow("attachGoogle");
			} else {
				viewModel.errorMsg("Server error");
				viewModel.errorState(true);
			}
		});
	};

	viewModel.requestDetachGoogle = function () {
		viewModel.attachGooglePage.detach(true);
		viewModel.pageFlow("attachGoogle");
	};

	viewModel.toggleSecret = function () {
		viewModel.attachGooglePage.showSecret(!viewModel.attachGooglePage.showSecret());
	};

	viewModel.validateGoogleOtp = function () {
		viewModel.errorState(false);
		viewModel.formValidation(true);
		if (viewModel.attachGooglePage.googleOtpValidates()) {
			var url = viewModel.attachGooglePage.detach() ? "user/googleauth/detachAuthenticator" : "user/googleauth/validateOtp";
			var data = {
				googleOtp: viewModel.attachGooglePage.googleOtp()
			};
			viewModel.attachGooglePage.googleOtp('');
			viewModel.formValidation(false);
			console.log("login: posting to url:", url, data);
			ajax("POST", url, data).success(function (data) {
				if (data.status === "success") {
					authenticatedSuccess(data);
					viewModel.userInfo.successMsg(viewModel.attachGooglePage.detach() ? "Google Authenticator successfully detached" : "Google Authenticator successfully attached");
				} else {
					viewModel.errorMsg("One time code did not match, try again..");
					viewModel.errorState(true);
				}
			});
		}
	};

	viewModel.requestAttachYubi = function () {
		viewModel.attachYubiPage.detach(false);
		viewModel.pageFlow("attachYubi");
	};

	viewModel.requestDetachYubi = function () {
		viewModel.attachYubiPage.detach(true);
		viewModel.pageFlow("attachYubi");
	};

	viewModel.validateYubiOtp = function () {
		viewModel.errorState(false);
		viewModel.formValidation(true);
		if (viewModel.attachYubiPage.yubiOtpValidates()) {
			var url = viewModel.attachYubiPage.detach() ? "user/yubiauth/detachYubikey" : "user/yubiauth/attachYubikey";
			var data = {
				yubiOtp: viewModel.attachYubiPage.yubiOtp()
			};
			viewModel.attachYubiPage.yubiOtp('');
			viewModel.formValidation(false);
			console.log("login: posting to url:", url, data);
			ajax("POST", url, data).success(function (data) {
				if (data.status === "success") {
					authenticatedSuccess(data);
					viewModel.userInfo.successMsg(viewModel.attachYubiPage.detach() ? "Yubikey successfully detached" : "Yubikey successfully attached");
				} else {
					viewModel.errorMsg("One time code did not match, try again..");
					viewModel.errorState(true);
				}
			});
		}
	};

	viewModel.cancelAttachTwoFactor = function () {
		viewModel.pageFlow("userInfo");
	}

	$(document).ready(function () {
		ko.applyBindings(viewModel);
		console.log("requesting currentUser");
		viewModel.pageFlow('login');
		ajax("GET", "user/currentUser").success(function (data) {
			console.log("received data", data);
			if (data.status === "success") {
				authenticatedSuccess(data);
			}
		});
	});

}(window.jQuery, window.ko));
