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

package com.microfocus.application.automation.tools.results.service;

public class AlmRestInfo {
	
	private String serverUrl;
	private String userName;
	private String password;
	private String domain;
	private String clientType;
	private String project;
	private String timeout;
	
	public AlmRestInfo(String serverUrl, String domain, String clientType, String project,  String userName, String password, String timeout) {
		this.serverUrl = serverUrl;
		this.userName = userName;
		this.password = password;
		this.domain = domain;
		this.clientType = clientType;
		this.project = project;
		this.timeout = timeout;
	}
	
	public String getServerUrl(){
		return serverUrl;
	}
	
	public void setServerUrl(String serverUrl) {
		this.serverUrl = serverUrl;
	}
	
	public String getUserName(){
		return userName;
	}
	
	public void setUserName(String userName) {
		this.userName = userName;
	}
	
	public String getPassword(){
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public String getDomain(){
		return domain;
	}
	
	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getClientType() {
		return clientType;
	}

	public void setClientType(String clientType) {
		this.clientType = clientType;
	}

	public String getProject(){
		return project;
	}

	public void setProject(String project){
		this.project = project;
	}
	
	public String getTimeout() {
		return timeout;
	}
	
	public void setTimeout(String timeout){
		this.timeout = timeout;
	}
}
