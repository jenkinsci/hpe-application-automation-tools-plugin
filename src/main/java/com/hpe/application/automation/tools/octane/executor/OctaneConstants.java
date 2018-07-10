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

package com.hpe.application.automation.tools.octane.executor;

/**
 * Constants of Octane Entities
 */
public class OctaneConstants {

    public static class General{

        public static final String WINDOWS_PATH_SPLITTER = "\\";
        public static final String LINUX_PATH_SPLITTER = "/";
    }

    public static class Base {
        public static final String ID_FIELD = "id";
        public static final String NAME_FIELD = "name";
        public static final String LOGICAL_NAME_FIELD = "logical_name";
        public static final String DESCRIPTION_FIELD = "description";
    }

    public static class Tests extends Base {
        public static final String COLLECTION_NAME = "automated_tests";
        public static final String SCM_REPOSITORY_FIELD = "scm_repository";
        public static final String TESTING_TOOL_TYPE_FIELD = "testing_tool_type";
        public static final String TEST_TYPE_FIELD = "test_type";
        public static final String PACKAGE_FIELD = "package";
        public static final String EXECUTABLE_FIELD = "executable";
    }

    public static class DataTables extends Base {
        public static final String COLLECTION_NAME = "scm_resource_files";
        public static final String RELATIVE_PATH_FIELD = "relative_path";
        public static final String SCM_REPOSITORY_FIELD = "scm_repository";
    }

    public static class Executors extends Base {
        public static final String COLLECTION_NAME = "executors";
    }

}
