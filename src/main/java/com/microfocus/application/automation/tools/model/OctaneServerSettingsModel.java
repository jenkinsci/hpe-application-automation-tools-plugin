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

package com.microfocus.application.automation.tools.model;

import hudson.util.Secret;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.Date;

public class OctaneServerSettingsModel {

    private String identity;
    private Long identityFrom;

    private String uiLocation;
    private String username;
    private Secret password;
    private String impersonatedUser;
    private boolean suspend;

    // inferred from uiLocation
    private String location;
    private String sharedSpace;


    public OctaneServerSettingsModel() {

    }

    @DataBoundConstructor
    public OctaneServerSettingsModel(String uiLocation, String username, Secret password, String impersonatedUser) {
        this.uiLocation = StringUtils.trim(uiLocation);
        this.username = username;
        this.password = password;
        this.impersonatedUser = impersonatedUser;
    }

    public boolean isSuspend(){
        return this.suspend;
    }

    @DataBoundSetter
    public void setSuspend(boolean suspend){
        this.suspend = suspend;
    }

    public String getUiLocation() {
        return uiLocation;
    }

    public String getUsername() {
        return username;
    }

    public Secret getPassword() {
        return password;
    }

    public String getImpersonatedUser() {
        return impersonatedUser;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        if (StringUtils.isEmpty(identity)) {
            throw new IllegalArgumentException("Empty identity is not allowed");
        }
        this.identity = identity;
        this.setIdentityFrom(new Date().getTime());
    }

    public Long getIdentityFrom() {
        return identityFrom;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getSharedSpace() {
        return sharedSpace;
    }

    public void setSharedSpace(String sharedSpace) {
        this.sharedSpace = sharedSpace;
    }

    public void setIdentityFrom(Long identityFrom) {
        this.identityFrom = identityFrom;
    }
}