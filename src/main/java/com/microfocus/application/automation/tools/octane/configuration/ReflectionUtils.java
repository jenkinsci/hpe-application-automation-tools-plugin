/*
 * Certain versions of software and/or documents ("Material") accessible here may contain branding from
 * Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 * the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 * and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 * marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * (c) Copyright 2012-2019 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors ("Micro Focus") are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 * ___________________________________________________________________
 */

package com.microfocus.application.automation.tools.octane.configuration;

import hudson.model.Action;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/***
 * A utility class to help retrieving data from objects,
 * on whom we have no type data.
 */
public class ReflectionUtils {
    private static final Logger logger = SDKBasedLoggerProvider.getLogger(ReflectionUtils.class);

    public static <T>  T getFieldValue(Object someObject, String fieldName) {
        for (Field field : someObject.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            if (field.getName().equals(fieldName)) {
                Object value = null;
                try {
                    value = field.get(someObject);
                } catch (IllegalAccessException e) {
                    logger.error("Failed to getFieldValue", e);
                }
                if (value != null) {
                    return (T)value;
                }
            }
        }
        return null;
    }

    public static Object invokeMethodByName(Action action, String methodName, Object... args) throws InvocationTargetException, IllegalAccessException {
        Method method = getMethodByName(action, methodName);

        return method.invoke(action, args);
    }

    public static Method getMethodByName(Action action, String methodName) {
        Method method = Arrays.stream(action.getClass().getDeclaredMethods())
                .filter(m->m.getName().equals(methodName))
                .findFirst().orElse(null);
        return method;
    }
}
