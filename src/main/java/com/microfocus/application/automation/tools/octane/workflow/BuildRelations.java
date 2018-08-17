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

package com.microfocus.application.automation.tools.octane.workflow;

import com.hp.octane.integrations.dto.causes.CIEventCause;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
 * Created by gadiel on 21/06/2016.
 */
public class BuildRelations {
    private static BuildRelations instance;
    private static Map<String,CIEventCause> map;

    private BuildRelations()
    {
        map = new ConcurrentHashMap<String,CIEventCause>();
    }

    public static BuildRelations getInstance()
    {
        if(instance == null)
            instance = new BuildRelations();
        return instance;
    }

    public void removePairByKey(String key)
    {
        if(map.containsKey(key))
        {
            map.remove(key);
        }
    }
    public void addBuildRelation(String projectName, CIEventCause ciEventCause)
    {
        map.put(projectName,ciEventCause);
    }

    public boolean containKey(String key)
    {
        return map.containsKey(key);
    }

    public CIEventCause getValue(String key)
    {
        return (CIEventCause)map.get(key);
    }

    public String print()
    {
        String totalMap="";
        for(String s : map.keySet())
        {
            totalMap = totalMap.concat(s+"_");
        }
        return totalMap;
    }

}
