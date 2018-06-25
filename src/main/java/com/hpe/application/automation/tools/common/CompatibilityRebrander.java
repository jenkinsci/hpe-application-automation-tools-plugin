package com.hpe.application.automation.tools.common;

import hudson.model.Items;
import hudson.model.Run;

import javax.annotation.Nonnull;

/**
 * CompatibilityRebrander is an interface for all related to the company rebranding phase.
 * <p>
 * This process is required because our serialized data includes the old package class name.
 * A measure must be taken in order to maintain backward compatibility.
 * <p>
 * Important note to mention is this interface will only work after:
 * <ul>
 *     <li> Package path is renamed from the old brand to the new one
 *     <li> Only the various Descriptors (e.g. BuildStepDescriptor) should extended this interface
 * </ul>
 */
public class CompatibilityRebrander {
    static String beforeBrand = "com.hpe";
    static String afterBrand = "com.microfocus";

    /**
     * addAliases is the actual function who does the rebranding part.
     * <p>
     * Items.XSTREAM2.addCompatibilityAlias is for serializing project configurations.
     * Run.XSTREAM2.addCompatibilityAlias is for serializing builds and its associated Actions.
     * @param newClass      the Descriptor class we want to add alias for
     * @since 5.5
     * @see hudson.model.Items#XSTREAM2
     */
    public static void addAliases(@Nonnull Class newClass) throws IllegalArgumentException {
        String newClassName = newClass.toString().replaceFirst("class ", "");
        String oldClassName = newClassName.replaceFirst(afterBrand, beforeBrand);

        if (!oldClassName.contains(beforeBrand))
            throw new IllegalArgumentException(String.format("The %s doesn't contain: %s", newClass.toString(), beforeBrand));

        Items.XSTREAM2.addCompatibilityAlias(oldClassName, newClass);
        Run.XSTREAM2.addCompatibilityAlias(oldClassName, newClass);
    }
}
