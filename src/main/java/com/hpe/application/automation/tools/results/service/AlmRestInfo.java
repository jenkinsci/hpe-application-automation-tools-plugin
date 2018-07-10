/*
 * © Copyright 2013 EntIT Software LLC
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright (c) 2018 Micro Focus Company, L.P.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ___________________________________________________________________
 *
 */

package com.hpe.application.automation.tools.results.service;

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
