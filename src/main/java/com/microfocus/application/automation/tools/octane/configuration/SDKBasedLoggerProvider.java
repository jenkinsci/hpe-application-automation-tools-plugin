/*
 *  Certain versions of software accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 *  This software was acquired by Micro Focus on September 1, 2017, and is now
 *  offered by OpenText.
 *  Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical
 *  in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the
 *  property of their respective owners.
 *  OpenText is a trademark of Open Text.
 *  __________________________________________________________________
 *  MIT License
 *
 *  Copyright 2012-2025 Open Text.
 *
 *  The only warranties for products and services of Open Text and
 *  its affiliates and licensors ("Open Text") are as may be set forth
 *  in the express warranty statements accompanying such products and services.
 *  Nothing herein should be construed as constituting an additional warranty.
 *  Open Text shall not be liable for technical or editorial errors or
 *  omissions contained herein. The information contained herein is subject
 *  to change without notice.
 *
 *  Except as specifically indicated otherwise, this document contains
 *  confidential information and a valid license is required for possession,
 *  use or copying. If this work is provided to the U.S. Government,
 *  consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 *  Computer Software Documentation, and Technical Data for Commercial Items are
 *  licensed to the U.S. Government under vendor's standard commercial license.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ___________________________________________________________________
 */
package com.microfocus.application.automation.tools.octane.configuration;

import com.hp.octane.integrations.services.logging.CommonLoggerContextUtil;
import com.microfocus.application.automation.tools.octane.CIJenkinsServicesImpl;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.EntryMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.util.MessageSupplier;
import org.apache.logging.log4j.util.Supplier;

import java.io.File;

/**
 * SDK based (log4j brought by SDK) logger provider
 * Main purpose of this custom logger provider is to ensure correct logs location configuration at the earliest point of the plugin initialization
 * TODO: this method might become a part of an SPI interface of SDK's plugin services
 */
public final class SDKBasedLoggerProvider {
    private static volatile boolean sysParamConfigured = false;

    private SDKBasedLoggerProvider() {
        //CodeClimate  : Add a private constructor to hide the implicit public one.
    }

    public static Logger getLogger(Class<?> type) {
        try {
            if (!sysParamConfigured) {
                System.setProperty("octaneAllowedStorage", CIJenkinsServicesImpl.getAllowedStorageFile().getAbsolutePath() + File.separator);
                //CommonLoggerContextUtil.configureLogger(CIJenkinsServicesImpl.getAllowedStorageFile());
                sysParamConfigured = true;
            }
            return LogManager.getLogger(type);
        } catch (Exception e) {
            //it can fail on slave because of missing permissions to create a folder for logs. In this case we return logger that do nothing
            //JENKINS-6010 : Cannot send junit results to Octane, because there is no permission on slave to create log folder
            return new EmptyLogger();
        }
    }

    private static class EmptyLogger implements Logger {

        @Override
        public void catching(Level level, Throwable throwable) {
            //do nothing
        }

        @Override
        public void catching(Throwable throwable) {
            //do nothing
        }

        @Override
        public void debug(Marker marker, Message message) {
            //do nothing
        }

        @Override
        public void debug(Marker marker, Message message, Throwable throwable) {
            //do nothing
        }

        @Override
        public void debug(Marker marker, MessageSupplier messageSupplier) {
            //do nothing
        }

        @Override
        public void debug(Marker marker, MessageSupplier messageSupplier, Throwable throwable) {
            //do nothing
        }

        @Override
        public void debug(Marker marker, CharSequence charSequence) {
            //do nothing
        }

        @Override
        public void debug(Marker marker, CharSequence charSequence, Throwable throwable) {
            //do nothing
        }

        @Override
        public void debug(Marker marker, Object o) {
            //do nothing
        }

        @Override
        public void debug(Marker marker, Object o, Throwable throwable) {
            //do nothing
        }

        @Override
        public void debug(Marker marker, String s) {
            //do nothing
        }

        @Override
        public void debug(Marker marker, String s, Object... objects) {
            //do nothing
        }

        @Override
        public void debug(Marker marker, String s, Supplier<?>... suppliers) {
            //do nothing
        }

        @Override
        public void debug(Marker marker, String s, Throwable throwable) {
            //do nothing
        }

        @Override
        public void debug(Marker marker, Supplier<?> supplier) {
            //do nothing
        }

        @Override
        public void debug(Marker marker, Supplier<?> supplier, Throwable throwable) {
            //do nothing
        }

        @Override
        public void debug(Message message) {
            //do nothing
        }

        @Override
        public void debug(Message message, Throwable throwable) {
            //do nothing
        }

        @Override
        public void debug(MessageSupplier messageSupplier) {
            //do nothing
        }

        @Override
        public void debug(MessageSupplier messageSupplier, Throwable throwable) {
            //do nothing
        }

        @Override
        public void debug(CharSequence charSequence) {
            //do nothing
        }

        @Override
        public void debug(CharSequence charSequence, Throwable throwable) {
            //do nothing
        }

        @Override
        public void debug(Object o) {
            //do nothing
        }

        @Override
        public void debug(Object o, Throwable throwable) {
            //do nothing
        }

        @Override
        public void debug(String s) {
            //do nothing
        }

        @Override
        public void debug(String s, Object... objects) {
            //do nothing
        }

        @Override
        public void debug(String s, Supplier<?>... suppliers) {
            //do nothing
        }

        @Override
        public void debug(String s, Throwable throwable) {
            //do nothing
        }

        @Override
        public void debug(Supplier<?> supplier) {
            //do nothing
        }

        @Override
        public void debug(Supplier<?> supplier, Throwable throwable) {
            //do nothing
        }

        @Override
        public void debug(Marker marker, String s, Object o) {
            //do nothing
        }

        @Override
        public void debug(Marker marker, String s, Object o, Object o1) {
            //do nothing
        }

        @Override
        public void debug(Marker marker, String s, Object o, Object o1, Object o2) {
            //do nothing
        }

        @Override
        public void debug(Marker marker, String s, Object o, Object o1, Object o2, Object o3) {
            //do nothing
        }

        @Override
        public void debug(Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4) {
            //do nothing
        }

        @Override
        public void debug(Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5) {
            //do nothing
        }

        @Override
        public void debug(Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6) {
            //do nothing
        }

        @Override
        public void debug(Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7) {
            //do nothing
        }

        @Override
        public void debug(Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8) {
            //do nothing
        }

        @Override
        public void debug(Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8, Object o9) {
            //do nothing
        }

        @Override
        public void debug(String s, Object o) {
            //do nothing
        }

        @Override
        public void debug(String s, Object o, Object o1) {
            //do nothing
        }

        @Override
        public void debug(String s, Object o, Object o1, Object o2) {
            //do nothing
        }

        @Override
        public void debug(String s, Object o, Object o1, Object o2, Object o3) {
            //do nothing
        }

        @Override
        public void debug(String s, Object o, Object o1, Object o2, Object o3, Object o4) {
            //do nothing
        }

        @Override
        public void debug(String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5) {
            //do nothing
        }

        @Override
        public void debug(String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6) {
            //do nothing
        }

        @Override
        public void debug(String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7) {
            //do nothing
        }

        @Override
        public void debug(String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8) {
            //do nothing
        }

        @Override
        public void debug(String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8, Object o9) {
            //do nothing
        }

        @Override
        public void entry() {
            //do nothing
        }

        @Override
        public void entry(Object... objects) {
            //do nothing
        }

        @Override
        public void error(Marker marker, Message message) {
            //do nothing
        }

        @Override
        public void error(Marker marker, Message message, Throwable throwable) {
            //do nothing
        }

        @Override
        public void error(Marker marker, MessageSupplier messageSupplier) {
            //do nothing
        }

        @Override
        public void error(Marker marker, MessageSupplier messageSupplier, Throwable throwable) {
            //do nothing
        }

        @Override
        public void error(Marker marker, CharSequence charSequence) {
            //do nothing
        }

        @Override
        public void error(Marker marker, CharSequence charSequence, Throwable throwable) {
            //do nothing
        }

        @Override
        public void error(Marker marker, Object o) {
            //do nothing
        }

        @Override
        public void error(Marker marker, Object o, Throwable throwable) {
            //do nothing
        }

        @Override
        public void error(Marker marker, String s) {
            //do nothing
        }

        @Override
        public void error(Marker marker, String s, Object... objects) {
            //do nothing
        }

        @Override
        public void error(Marker marker, String s, Supplier<?>... suppliers) {
            //do nothing
        }

        @Override
        public void error(Marker marker, String s, Throwable throwable) {
            //do nothing
        }

        @Override
        public void error(Marker marker, Supplier<?> supplier) {
            //do nothing
        }

        @Override
        public void error(Marker marker, Supplier<?> supplier, Throwable throwable) {
            //do nothing
        }

        @Override
        public void error(Message message) {
            //do nothing
        }

        @Override
        public void error(Message message, Throwable throwable) {
            //do nothing
        }

        @Override
        public void error(MessageSupplier messageSupplier) {
            //do nothing
        }

        @Override
        public void error(MessageSupplier messageSupplier, Throwable throwable) {
            //do nothing
        }

        @Override
        public void error(CharSequence charSequence) {
            //do nothing
        }

        @Override
        public void error(CharSequence charSequence, Throwable throwable) {
            //do nothing
        }

        @Override
        public void error(Object o) {
            //do nothing
        }

        @Override
        public void error(Object o, Throwable throwable) {
            //do nothing
        }

        @Override
        public void error(String s) {
            //do nothing
        }

        @Override
        public void error(String s, Object... objects) {
            //do nothing
        }

        @Override
        public void error(String s, Supplier<?>... suppliers) {
            //do nothing
        }

        @Override
        public void error(String s, Throwable throwable) {
            //do nothing
        }

        @Override
        public void error(Supplier<?> supplier) {
            //do nothing
        }

        @Override
        public void error(Supplier<?> supplier, Throwable throwable) {
            //do nothing
        }

        @Override
        public void error(Marker marker, String s, Object o) {
            //do nothing
        }

        @Override
        public void error(Marker marker, String s, Object o, Object o1) {
            //do nothing
        }

        @Override
        public void error(Marker marker, String s, Object o, Object o1, Object o2) {
            //do nothing
        }

        @Override
        public void error(Marker marker, String s, Object o, Object o1, Object o2, Object o3) {
            //do nothing
        }

        @Override
        public void error(Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4) {
            //do nothing
        }

        @Override
        public void error(Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5) {
            //do nothing
        }

        @Override
        public void error(Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6) {
            //do nothing
        }

        @Override
        public void error(Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7) {
            //do nothing
        }

        @Override
        public void error(Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8) {
            //do nothing
        }

        @Override
        public void error(Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8, Object o9) {
            //do nothing
        }

        @Override
        public void error(String s, Object o) {
            //do nothing
        }

        @Override
        public void error(String s, Object o, Object o1) {
            //do nothing
        }

        @Override
        public void error(String s, Object o, Object o1, Object o2) {
            //do nothing
        }

        @Override
        public void error(String s, Object o, Object o1, Object o2, Object o3) {
            //do nothing
        }

        @Override
        public void error(String s, Object o, Object o1, Object o2, Object o3, Object o4) {
            //do nothing
        }

        @Override
        public void error(String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5) {
            //do nothing
        }

        @Override
        public void error(String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6) {
            //do nothing
        }

        @Override
        public void error(String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7) {
            //do nothing
        }

        @Override
        public void error(String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8) {
            //do nothing
        }

        @Override
        public void error(String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8, Object o9) {
            //do nothing
        }

        @Override
        public void exit() {
            //do nothing
        }

        @Override
        public <R> R exit(R r) {
            return null;
        }

        @Override
        public void fatal(Marker marker, Message message) {
            //do nothing
        }

        @Override
        public void fatal(Marker marker, Message message, Throwable throwable) {
            //do nothing
        }

        @Override
        public void fatal(Marker marker, MessageSupplier messageSupplier) {
            //do nothing
        }

        @Override
        public void fatal(Marker marker, MessageSupplier messageSupplier, Throwable throwable) {
            //do nothing
        }

        @Override
        public void fatal(Marker marker, CharSequence charSequence) {
            //do nothing
        }

        @Override
        public void fatal(Marker marker, CharSequence charSequence, Throwable throwable) {
            //do nothing
        }

        @Override
        public void fatal(Marker marker, Object o) {
            //do nothing
        }

        @Override
        public void fatal(Marker marker, Object o, Throwable throwable) {
            //do nothing
        }

        @Override
        public void fatal(Marker marker, String s) {
            //do nothing
        }

        @Override
        public void fatal(Marker marker, String s, Object... objects) {
            //do nothing
        }

        @Override
        public void fatal(Marker marker, String s, Supplier<?>... suppliers) {
            //do nothing
        }

        @Override
        public void fatal(Marker marker, String s, Throwable throwable) {
            //do nothing
        }

        @Override
        public void fatal(Marker marker, Supplier<?> supplier) {
            //do nothing
        }

        @Override
        public void fatal(Marker marker, Supplier<?> supplier, Throwable throwable) {
            //do nothing
        }

        @Override
        public void fatal(Message message) {
            //do nothing
        }

        @Override
        public void fatal(Message message, Throwable throwable) {
            //do nothing
        }

        @Override
        public void fatal(MessageSupplier messageSupplier) {
            //do nothing
        }

        @Override
        public void fatal(MessageSupplier messageSupplier, Throwable throwable) {
            //do nothing
        }

        @Override
        public void fatal(CharSequence charSequence) {
            //do nothing
        }

        @Override
        public void fatal(CharSequence charSequence, Throwable throwable) {
            //do nothing
        }

        @Override
        public void fatal(Object o) {
            //do nothing
        }

        @Override
        public void fatal(Object o, Throwable throwable) {
            //do nothing
        }

        @Override
        public void fatal(String s) {
            //do nothing
        }

        @Override
        public void fatal(String s, Object... objects) {
            //do nothing
        }

        @Override
        public void fatal(String s, Supplier<?>... suppliers) {
            //do nothing
        }

        @Override
        public void fatal(String s, Throwable throwable) {
            //do nothing
        }

        @Override
        public void fatal(Supplier<?> supplier) {
            //do nothing
        }

        @Override
        public void fatal(Supplier<?> supplier, Throwable throwable) {
            //do nothing
        }

        @Override
        public void fatal(Marker marker, String s, Object o) {
            //do nothing
        }

        @Override
        public void fatal(Marker marker, String s, Object o, Object o1) {
            //do nothing
        }

        @Override
        public void fatal(Marker marker, String s, Object o, Object o1, Object o2) {
            //do nothing
        }

        @Override
        public void fatal(Marker marker, String s, Object o, Object o1, Object o2, Object o3) {
            //do nothing
        }

        @Override
        public void fatal(Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4) {
            //do nothing
        }

        @Override
        public void fatal(Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5) {
            //do nothing
        }

        @Override
        public void fatal(Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6) {
            //do nothing
        }

        @Override
        public void fatal(Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7) {
            //do nothing
        }

        @Override
        public void fatal(Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8) {
            //do nothing
        }

        @Override
        public void fatal(Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8, Object o9) {
            //do nothing
        }

        @Override
        public void fatal(String s, Object o) {
            //do nothing
        }

        @Override
        public void fatal(String s, Object o, Object o1) {
            //do nothing
        }

        @Override
        public void fatal(String s, Object o, Object o1, Object o2) {
            //do nothing
        }

        @Override
        public void fatal(String s, Object o, Object o1, Object o2, Object o3) {
            //do nothing
        }

        @Override
        public void fatal(String s, Object o, Object o1, Object o2, Object o3, Object o4) {
            //do nothing
        }

        @Override
        public void fatal(String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5) {
            //do nothing
        }

        @Override
        public void fatal(String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6) {
            //do nothing
        }

        @Override
        public void fatal(String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7) {
            //do nothing
        }

        @Override
        public void fatal(String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8) {
            //do nothing
        }

        @Override
        public void fatal(String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8, Object o9) {
            //do nothing
        }

        @Override
        public Level getLevel() {
            return null;
        }

        @Override
        public <MF extends MessageFactory> MF getMessageFactory() {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public void info(Marker marker, Message message) {
            //do nothing
        }

        @Override
        public void info(Marker marker, Message message, Throwable throwable) {
            //do nothing
        }

        @Override
        public void info(Marker marker, MessageSupplier messageSupplier) {
            //do nothing
        }

        @Override
        public void info(Marker marker, MessageSupplier messageSupplier, Throwable throwable) {
            //do nothing
        }

        @Override
        public void info(Marker marker, CharSequence charSequence) {
            //do nothing
        }

        @Override
        public void info(Marker marker, CharSequence charSequence, Throwable throwable) {
            //do nothing
        }

        @Override
        public void info(Marker marker, Object o) {
            //do nothing
        }

        @Override
        public void info(Marker marker, Object o, Throwable throwable) {
            //do nothing
        }

        @Override
        public void info(Marker marker, String s) {
            //do nothing
        }

        @Override
        public void info(Marker marker, String s, Object... objects) {
            //do nothing
        }

        @Override
        public void info(Marker marker, String s, Supplier<?>... suppliers) {
            //do nothing
        }

        @Override
        public void info(Marker marker, String s, Throwable throwable) {
            //do nothing
        }

        @Override
        public void info(Marker marker, Supplier<?> supplier) {
            //do nothing
        }

        @Override
        public void info(Marker marker, Supplier<?> supplier, Throwable throwable) {
            //do nothing
        }

        @Override
        public void info(Message message) {
            //do nothing
        }

        @Override
        public void info(Message message, Throwable throwable) {
            //do nothing
        }

        @Override
        public void info(MessageSupplier messageSupplier) {
            //do nothing
        }

        @Override
        public void info(MessageSupplier messageSupplier, Throwable throwable) {
            //do nothing
        }

        @Override
        public void info(CharSequence charSequence) {
            //do nothing
        }

        @Override
        public void info(CharSequence charSequence, Throwable throwable) {
            //do nothing
        }

        @Override
        public void info(Object o) {
            //do nothing
        }

        @Override
        public void info(Object o, Throwable throwable) {
            //do nothing
        }

        @Override
        public void info(String s) {
            //do nothing
        }

        @Override
        public void info(String s, Object... objects) {
            //do nothing
        }

        @Override
        public void info(String s, Supplier<?>... suppliers) {
            //do nothing
        }

        @Override
        public void info(String s, Throwable throwable) {
            //do nothing
        }

        @Override
        public void info(Supplier<?> supplier) {
            //do nothing
        }

        @Override
        public void info(Supplier<?> supplier, Throwable throwable) {
            //do nothing
        }

        @Override
        public void info(Marker marker, String s, Object o) {
            //do nothing
        }

        @Override
        public void info(Marker marker, String s, Object o, Object o1) {
            //do nothing
        }

        @Override
        public void info(Marker marker, String s, Object o, Object o1, Object o2) {
            //do nothing
        }

        @Override
        public void info(Marker marker, String s, Object o, Object o1, Object o2, Object o3) {
            //do nothing
        }

        @Override
        public void info(Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4) {
            //do nothing
        }

        @Override
        public void info(Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5) {
            //do nothing
        }

        @Override
        public void info(Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6) {
            //do nothing
        }

        @Override
        public void info(Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7) {
            //do nothing
        }

        @Override
        public void info(Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8) {
            //do nothing
        }

        @Override
        public void info(Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8, Object o9) {
            //do nothing
        }

        @Override
        public void info(String s, Object o) {
            //do nothing
        }

        @Override
        public void info(String s, Object o, Object o1) {
            //do nothing
        }

        @Override
        public void info(String s, Object o, Object o1, Object o2) {
            //do nothing
        }

        @Override
        public void info(String s, Object o, Object o1, Object o2, Object o3) {
            //do nothing
        }

        @Override
        public void info(String s, Object o, Object o1, Object o2, Object o3, Object o4) {
            //do nothing
        }

        @Override
        public void info(String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5) {
            //do nothing
        }

        @Override
        public void info(String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6) {
            //do nothing
        }

        @Override
        public void info(String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7) {
            //do nothing
        }

        @Override
        public void info(String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8) {
            //do nothing
        }

        @Override
        public void info(String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8, Object o9) {
            //do nothing
        }

        @Override
        public boolean isDebugEnabled() {
            return false;
        }

        @Override
        public boolean isDebugEnabled(Marker marker) {
            return false;
        }

        @Override
        public boolean isEnabled(Level level) {
            return false;
        }

        @Override
        public boolean isEnabled(Level level, Marker marker) {
            return false;
        }

        @Override
        public boolean isErrorEnabled() {
            return false;
        }

        @Override
        public boolean isErrorEnabled(Marker marker) {
            return false;
        }

        @Override
        public boolean isFatalEnabled() {
            return false;
        }

        @Override
        public boolean isFatalEnabled(Marker marker) {
            return false;
        }

        @Override
        public boolean isInfoEnabled() {
            return false;
        }

        @Override
        public boolean isInfoEnabled(Marker marker) {
            return false;
        }

        @Override
        public boolean isTraceEnabled() {
            return false;
        }

        @Override
        public boolean isTraceEnabled(Marker marker) {
            return false;
        }

        @Override
        public boolean isWarnEnabled() {
            return false;
        }

        @Override
        public boolean isWarnEnabled(Marker marker) {
            return false;
        }

        @Override
        public void log(Level level, Marker marker, Message message) {
            //do nothing
        }

        @Override
        public void log(Level level, Marker marker, Message message, Throwable throwable) {
            //do nothing
        }

        @Override
        public void log(Level level, Marker marker, MessageSupplier messageSupplier) {
            //do nothing
        }

        @Override
        public void log(Level level, Marker marker, MessageSupplier messageSupplier, Throwable throwable) {
            //do nothing
        }

        @Override
        public void log(Level level, Marker marker, CharSequence charSequence) {
            //do nothing
        }

        @Override
        public void log(Level level, Marker marker, CharSequence charSequence, Throwable throwable) {
            //do nothing
        }

        @Override
        public void log(Level level, Marker marker, Object o) {
            //do nothing
        }

        @Override
        public void log(Level level, Marker marker, Object o, Throwable throwable) {
            //do nothing
        }

        @Override
        public void log(Level level, Marker marker, String s) {
            //do nothing
        }

        @Override
        public void log(Level level, Marker marker, String s, Object... objects) {
            //do nothing
        }

        @Override
        public void log(Level level, Marker marker, String s, Supplier<?>... suppliers) {
            //do nothing
        }

        @Override
        public void log(Level level, Marker marker, String s, Throwable throwable) {
            //do nothing
        }

        @Override
        public void log(Level level, Marker marker, Supplier<?> supplier) {
            //do nothing
        }

        @Override
        public void log(Level level, Marker marker, Supplier<?> supplier, Throwable throwable) {
            //do nothing
        }

        @Override
        public void log(Level level, Message message) {
            //do nothing
        }

        @Override
        public void log(Level level, Message message, Throwable throwable) {
            //do nothing
        }

        @Override
        public void log(Level level, MessageSupplier messageSupplier) {
            //do nothing
        }

        @Override
        public void log(Level level, MessageSupplier messageSupplier, Throwable throwable) {
            //do nothing
        }

        @Override
        public void log(Level level, CharSequence charSequence) {
            //do nothing
        }

        @Override
        public void log(Level level, CharSequence charSequence, Throwable throwable) {
            //do nothing
        }

        @Override
        public void log(Level level, Object o) {
            //do nothing
        }

        @Override
        public void log(Level level, Object o, Throwable throwable) {
            //do nothing
        }

        @Override
        public void log(Level level, String s) {
            //do nothing
        }

        @Override
        public void log(Level level, String s, Object... objects) {
            //do nothing
        }

        @Override
        public void log(Level level, String s, Supplier<?>... suppliers) {
            //do nothing
        }

        @Override
        public void log(Level level, String s, Throwable throwable) {
            //do nothing
        }

        @Override
        public void log(Level level, Supplier<?> supplier) {
            //do nothing
        }

        @Override
        public void log(Level level, Supplier<?> supplier, Throwable throwable) {
            //do nothing
        }

        @Override
        public void log(Level level, Marker marker, String s, Object o) {
            //do nothing
        }

        @Override
        public void log(Level level, Marker marker, String s, Object o, Object o1) {
            //do nothing
        }

        @Override
        public void log(Level level, Marker marker, String s, Object o, Object o1, Object o2) {
            //do nothing
        }

        @Override
        public void log(Level level, Marker marker, String s, Object o, Object o1, Object o2, Object o3) {
            //do nothing
        }

        @Override
        public void log(Level level, Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4) {
            //do nothing
        }

        @Override
        public void log(Level level, Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5) {
            //do nothing
        }

        @Override
        public void log(Level level, Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6) {
            //do nothing
        }

        @Override
        public void log(Level level, Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7) {
            //do nothing
        }

        @Override
        public void log(Level level, Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8) {
            //do nothing
        }

        @Override
        public void log(Level level, Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8, Object o9) {
            //do nothing
        }

        @Override
        public void log(Level level, String s, Object o) {
            //do nothing
        }

        @Override
        public void log(Level level, String s, Object o, Object o1) {
            //do nothing
        }

        @Override
        public void log(Level level, String s, Object o, Object o1, Object o2) {
            //do nothing
        }

        @Override
        public void log(Level level, String s, Object o, Object o1, Object o2, Object o3) {
            //do nothing
        }

        @Override
        public void log(Level level, String s, Object o, Object o1, Object o2, Object o3, Object o4) {
            //do nothing
        }

        @Override
        public void log(Level level, String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5) {
            //do nothing
        }

        @Override
        public void log(Level level, String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6) {
            //do nothing
        }

        @Override
        public void log(Level level, String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7) {
            //do nothing
        }

        @Override
        public void log(Level level, String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8) {
            //do nothing
        }

        @Override
        public void log(Level level, String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8, Object o9) {
            //do nothing
        }

        @Override
        public void printf(Level level, Marker marker, String s, Object... objects) {
            //do nothing
        }

        @Override
        public void printf(Level level, String s, Object... objects) {
            //do nothing
        }

        @Override
        public <T extends Throwable> T throwing(Level level, T t) {
            return null;
        }

        @Override
        public <T extends Throwable> T throwing(T t) {
            return null;
        }

        @Override
        public void trace(Marker marker, Message message) {
            //do nothing
        }

        @Override
        public void trace(Marker marker, Message message, Throwable throwable) {
            //do nothing
        }

        @Override
        public void trace(Marker marker, MessageSupplier messageSupplier) {
            //do nothing
        }

        @Override
        public void trace(Marker marker, MessageSupplier messageSupplier, Throwable throwable) {
            //do nothing
        }

        @Override
        public void trace(Marker marker, CharSequence charSequence) {
            //do nothing
        }

        @Override
        public void trace(Marker marker, CharSequence charSequence, Throwable throwable) {
            //do nothing
        }

        @Override
        public void trace(Marker marker, Object o) {
            //do nothing
        }

        @Override
        public void trace(Marker marker, Object o, Throwable throwable) {
            //do nothing
        }

        @Override
        public void trace(Marker marker, String s) {
            //do nothing
        }

        @Override
        public void trace(Marker marker, String s, Object... objects) {
            //do nothing
        }

        @Override
        public void trace(Marker marker, String s, Supplier<?>... suppliers) {
            //do nothing
        }

        @Override
        public void trace(Marker marker, String s, Throwable throwable) {
            //do nothing
        }

        @Override
        public void trace(Marker marker, Supplier<?> supplier) {
            //do nothing
        }

        @Override
        public void trace(Marker marker, Supplier<?> supplier, Throwable throwable) {
            //do nothing
        }

        @Override
        public void trace(Message message) {
            //do nothing
        }

        @Override
        public void trace(Message message, Throwable throwable) {
            //do nothing
        }

        @Override
        public void trace(MessageSupplier messageSupplier) {
            //do nothing
        }

        @Override
        public void trace(MessageSupplier messageSupplier, Throwable throwable) {
            //do nothing
        }

        @Override
        public void trace(CharSequence charSequence) {
            //do nothing
        }

        @Override
        public void trace(CharSequence charSequence, Throwable throwable) {
            //do nothing
        }

        @Override
        public void trace(Object o) {
            //do nothing
        }

        @Override
        public void trace(Object o, Throwable throwable) {
            //do nothing
        }

        @Override
        public void trace(String s) {
            //do nothing
        }

        @Override
        public void trace(String s, Object... objects) {
            //do nothing
        }

        @Override
        public void trace(String s, Supplier<?>... suppliers) {
            //do nothing
        }

        @Override
        public void trace(String s, Throwable throwable) {
            //do nothing
        }

        @Override
        public void trace(Supplier<?> supplier) {
            //do nothing
        }

        @Override
        public void trace(Supplier<?> supplier, Throwable throwable) {
            //do nothing
        }

        @Override
        public void trace(Marker marker, String s, Object o) {
            //do nothing
        }

        @Override
        public void trace(Marker marker, String s, Object o, Object o1) {
            //do nothing
        }

        @Override
        public void trace(Marker marker, String s, Object o, Object o1, Object o2) {
            //do nothing
        }

        @Override
        public void trace(Marker marker, String s, Object o, Object o1, Object o2, Object o3) {
            //do nothing
        }

        @Override
        public void trace(Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4) {
            //do nothing
        }

        @Override
        public void trace(Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5) {
            //do nothing
        }

        @Override
        public void trace(Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6) {
            //do nothing
        }

        @Override
        public void trace(Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7) {
            //do nothing
        }

        @Override
        public void trace(Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8) {
            //do nothing
        }

        @Override
        public void trace(Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8, Object o9) {
            //do nothing
        }

        @Override
        public void trace(String s, Object o) {
            //do nothing
        }

        @Override
        public void trace(String s, Object o, Object o1) {
            //do nothing
        }

        @Override
        public void trace(String s, Object o, Object o1, Object o2) {
            //do nothing
        }

        @Override
        public void trace(String s, Object o, Object o1, Object o2, Object o3) {
            //do nothing
        }

        @Override
        public void trace(String s, Object o, Object o1, Object o2, Object o3, Object o4) {
            //do nothing
        }

        @Override
        public void trace(String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5) {
            //do nothing
        }

        @Override
        public void trace(String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6) {
            //do nothing
        }

        @Override
        public void trace(String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7) {
            //do nothing
        }

        @Override
        public void trace(String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8) {
            //do nothing
        }

        @Override
        public void trace(String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8, Object o9) {
            //do nothing
        }

        @Override
        public EntryMessage traceEntry() {
            return null;
        }

        @Override
        public EntryMessage traceEntry(String s, Object... objects) {
            return null;
        }

        @Override
        public EntryMessage traceEntry(Supplier<?>... suppliers) {
            return null;
        }

        @Override
        public EntryMessage traceEntry(String s, Supplier<?>... suppliers) {
            return null;
        }

        @Override
        public EntryMessage traceEntry(Message message) {
            return null;
        }

        @Override
        public void traceExit() {
            //do nothing
        }

        @Override
        public <R> R traceExit(R r) {
            return null;
        }

        @Override
        public <R> R traceExit(String s, R r) {
            return null;
        }

        @Override
        public void traceExit(EntryMessage entryMessage) {
            //do nothing
        }

        @Override
        public <R> R traceExit(EntryMessage entryMessage, R r) {
            return null;
        }

        @Override
        public <R> R traceExit(Message message, R r) {
            return null;
        }

        @Override
        public void warn(Marker marker, Message message) {
            //do nothing
        }

        @Override
        public void warn(Marker marker, Message message, Throwable throwable) {
            //do nothing
        }

        @Override
        public void warn(Marker marker, MessageSupplier messageSupplier) {
            //do nothing
        }

        @Override
        public void warn(Marker marker, MessageSupplier messageSupplier, Throwable throwable) {
            //do nothing
        }

        @Override
        public void warn(Marker marker, CharSequence charSequence) {
            //do nothing
        }

        @Override
        public void warn(Marker marker, CharSequence charSequence, Throwable throwable) {
            //do nothing
        }

        @Override
        public void warn(Marker marker, Object o) {
            //do nothing
        }

        @Override
        public void warn(Marker marker, Object o, Throwable throwable) {
            //do nothing
        }

        @Override
        public void warn(Marker marker, String s) {
            //do nothing
        }

        @Override
        public void warn(Marker marker, String s, Object... objects) {
            //do nothing
        }

        @Override
        public void warn(Marker marker, String s, Supplier<?>... suppliers) {
            //do nothing
        }

        @Override
        public void warn(Marker marker, String s, Throwable throwable) {
            //do nothing
        }

        @Override
        public void warn(Marker marker, Supplier<?> supplier) {
            //do nothing
        }

        @Override
        public void warn(Marker marker, Supplier<?> supplier, Throwable throwable) {
            //do nothing
        }

        @Override
        public void warn(Message message) {
            //do nothing
        }

        @Override
        public void warn(Message message, Throwable throwable) {
            //do nothing
        }

        @Override
        public void warn(MessageSupplier messageSupplier) {
            //do nothing
        }

        @Override
        public void warn(MessageSupplier messageSupplier, Throwable throwable) {
            //do nothing
        }

        @Override
        public void warn(CharSequence charSequence) {
            //do nothing
        }

        @Override
        public void warn(CharSequence charSequence, Throwable throwable) {
            //do nothing
        }

        @Override
        public void warn(Object o) {
            //do nothing
        }

        @Override
        public void warn(Object o, Throwable throwable) {
            //do nothing
        }

        @Override
        public void warn(String s) {
            //do nothing
        }

        @Override
        public void warn(String s, Object... objects) {
            //do nothing
        }

        @Override
        public void warn(String s, Supplier<?>... suppliers) {
            //do nothing
        }

        @Override
        public void warn(String s, Throwable throwable) {
            //do nothing
        }

        @Override
        public void warn(Supplier<?> supplier) {
            //do nothing
        }

        @Override
        public void warn(Supplier<?> supplier, Throwable throwable) {
            //do nothing
        }

        @Override
        public void warn(Marker marker, String s, Object o) {
            //do nothing
        }

        @Override
        public void warn(Marker marker, String s, Object o, Object o1) {
            //do nothing
        }

        @Override
        public void warn(Marker marker, String s, Object o, Object o1, Object o2) {
            //do nothing
        }

        @Override
        public void warn(Marker marker, String s, Object o, Object o1, Object o2, Object o3) {
            //do nothing
        }

        @Override
        public void warn(Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4) {
            //do nothing
        }

        @Override
        public void warn(Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5) {
            //do nothing
        }

        @Override
        public void warn(Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6) {
            //do nothing
        }

        @Override
        public void warn(Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7) {
            //do nothing
        }

        @Override
        public void warn(Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8) {
            //do nothing
        }

        @Override
        public void warn(Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8, Object o9) {
            //do nothing
        }

        @Override
        public void warn(String s, Object o) {
            //do nothing
        }

        @Override
        public void warn(String s, Object o, Object o1) {
            //do nothing
        }

        @Override
        public void warn(String s, Object o, Object o1, Object o2) {
            //do nothing
        }

        @Override
        public void warn(String s, Object o, Object o1, Object o2, Object o3) {
            //do nothing
        }

        @Override
        public void warn(String s, Object o, Object o1, Object o2, Object o3, Object o4) {
            //do nothing
        }

        @Override
        public void warn(String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5) {
            //do nothing
        }

        @Override
        public void warn(String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6) {
            //do nothing
        }

        @Override
        public void warn(String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7) {
            //do nothing
        }

        @Override
        public void warn(String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8) {
            //do nothing
        }

        @Override
        public void warn(String s, Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8, Object o9) {
            //do nothing
        }
    }
}
