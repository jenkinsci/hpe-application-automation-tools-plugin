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

import javax.annotation.Nonnull;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class SvDataModelSelection extends AbstractDescribableImpl<SvDataModelSelection> {

    protected final SelectionType selectionType;
    protected final String dataModel;

    @DataBoundConstructor
    public SvDataModelSelection(SelectionType selectionType, String dataModel) {
        this.selectionType = selectionType;
        this.dataModel = StringUtils.trim(dataModel);
    }

    public static void validateField(FormValidation result) {
        if (!result.equals(FormValidation.ok())) {
            throw new IllegalArgumentException(StringEscapeUtils.unescapeXml(result.getMessage()));
        }
    }

    @SuppressWarnings("unused")
    public SelectionType getSelectionType() {
        return selectionType;
    }

    public String getDataModel() {
        return (StringUtils.isNotBlank(dataModel)) ? dataModel : null;
    }

    @SuppressWarnings("unused")
    public boolean isSelected(String type) {
        return SelectionType.valueOf(type) == this.selectionType;
    }

    public boolean isNoneSelected() {
        return selectionType == SelectionType.NONE;
    }

    public boolean isDefaultSelected() {
        return selectionType == SelectionType.DEFAULT;
    }

    @Override
    public String toString() {
        switch (selectionType) {
            case BY_NAME:
                return dataModel;
            case NONE:
                return "<none>";
            case DEFAULT:
            default:
                return "<default>";
        }
    }

    public String getSelectedModelName() {
        switch (selectionType) {
            case BY_NAME:
                DescriptorImpl descriptor = (DescriptorImpl) getDescriptor();
                validateField(descriptor.doCheckDataModel(dataModel));
                return dataModel;
            default:
                return null;
        }
    }

    public enum SelectionType {
        BY_NAME,
        NONE,
        /**
         * Default means first model in alphabetical order by model name
         */
        DEFAULT,
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<SvDataModelSelection> {

        @Nonnull
        public String getDisplayName() {
            return "Data model Selection";
        }

        @SuppressWarnings("unused")
        public FormValidation doCheckDataModel(@QueryParameter String dataModel) {
            if (StringUtils.isBlank(dataModel)) {
                return FormValidation.error("Data model cannot be empty if 'Specific' model is selected");
            }
            return FormValidation.ok();
        }
    }
}
