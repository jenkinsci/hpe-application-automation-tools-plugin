/*
 *
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * © Copyright 2012-2018 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors (“Micro Focus”) are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 * ___________________________________________________________________
 *
 */

package com.microfocus.application.automation.tools.octane.configuration;

import hudson.util.Secret;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;

@SuppressWarnings({"squid:S1312","squid:S00122"})
final public class ServerConfiguration {
	private static final Logger logger = LogManager.getLogger(ServerConfiguration.class);

	public String location;
	public String sharedSpace;
	public String username;
	public Secret password;
	public String impersonatedUser;
	public boolean suspend;

	public ServerConfiguration(String location, String sharedSpace, String username, Secret password, String impersonatedUser, boolean suspend) {
		this.location = location;
		this.sharedSpace = sharedSpace;
		this.username = username;
		this.password = password;
		this.impersonatedUser = impersonatedUser;
		this.suspend = suspend;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ServerConfiguration that = (ServerConfiguration) o;

		if (location != null ? !location.equals(that.location) : that.location != null) return false;
		if (sharedSpace != null ? !sharedSpace.equals(that.sharedSpace) : that.sharedSpace != null) return false;
		if (username != null ? !username.equals(that.username) : that.username != null) return false;
		if (password != null ? !password.equals(that.password) : that.password != null) return false;
		if (impersonatedUser != null ? !impersonatedUser.equals(that.impersonatedUser) : that.impersonatedUser != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = location != null ? location.hashCode() : 0;
		result = 31 * result + (sharedSpace != null ? sharedSpace.hashCode() : 0);
		result = 31 * result + (username != null ? username.hashCode() : 0);
		result = 31 * result + (password != null ? password.hashCode() : 0);
		result = 31 * result + (impersonatedUser != null ? impersonatedUser.hashCode() : 0);
		return result;
	}

	public boolean isValid() {
		boolean result = false;
		if (location != null && !location.isEmpty() &&
				sharedSpace != null && !sharedSpace.isEmpty()) {
			try {
				URL tmp = new URL(location);
				logger.debug(String.format("location: %s",tmp.toString()));
				result = true;
			} catch (MalformedURLException mue) {
				logger.error("configuration with malformed URL supplied", mue);
			}
		}
		return result;
	}

	public boolean isSuspend(){
		return this.suspend;
	}

	public void setSuspend(boolean suspend){
		this.suspend = suspend;
	}

	@Override
	public String toString() {
		return "{ url: " + location +
				", sharedSpace: " + sharedSpace +
				", username: " + username + " }";
	}
}
