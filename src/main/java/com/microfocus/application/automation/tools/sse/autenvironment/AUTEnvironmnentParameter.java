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

package com.microfocus.application.automation.tools.sse.autenvironment;

/**
 * Created by barush on 02/11/2014.
 */
public class AUTEnvironmnentParameter {
    
    public final static String ALM_PARAMETER_ID_FIELD = "id";
    public final static String ALM_PARAMETER_PARENT_ID_FIELD = "parent-id";
    public final static String ALM_PARAMETER_NAME_FIELD = "name";
    public final static String ALM_PARAMETER_VALUE_FIELD = "value";
    
    private String id;
    private String parentId;
    private String name;
    private String value;
    private String fullPath;
    
    public AUTEnvironmnentParameter(String id, String parentId, String name) {
        
        this.id = id;
        this.parentId = parentId;
        this.name = name;
    }
    
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
    
    public String getFullPath() {
        return fullPath;
    }
    
    public void setFullPath(String fullPath) {
        this.fullPath = fullPath;
    }
    
    public String getId() {
        return id;
    }
    
    public String getParentId() {
        return parentId;
    }
    
    public String getName() {
        return name;
    }
}
