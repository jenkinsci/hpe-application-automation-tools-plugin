package com.microfocus.application.automation.tools.common;

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
 * <li> Package path is renamed from the old brand to the new one
 * <li> Only the various Descriptors (e.g. BuildStepDescriptor) should extended this interface
 * </ul>
 */
public class CompatibilityRebrander {
    static String beforeHpeBrand = "com.hpe";
    static String beforeHpBrand = "com.hp";
    static String afterBrand = "com.microfocus";

    /**
     * addAliases is the actual function who does the rebranding part for all the old package names
     * <p>
     * Items.XSTREAM2.addCompatibilityAlias is for serializing project configurations.
     * Run.XSTREAM2.addCompatibilityAlias is for serializing builds and its associated Actions.
     *
     * @param newClass the Descriptor class we want to add alias for
     * @see hudson.model.Items#XSTREAM2
     * @see hudson.model.Run#XSTREAM2
     * @since 5.5
     */
    public static void addAliases(@Nonnull Class newClass) throws IllegalArgumentException {
        String newClassName = newClass.toString().replaceFirst("class ", "");
        String oldHpeClassName = newClassName.replaceFirst(afterBrand, beforeHpeBrand);
        String oldHpClassName = newClassName.replaceFirst(afterBrand, beforeHpBrand);

        addAliasesForSingleClass(newClass, oldHpClassName, beforeHpBrand);
        addAliasesForSingleClass(newClass, oldHpeClassName, beforeHpeBrand);
    }

    /**
     * addAliasesForSingleClass responsible for handling the rebranding for a single class
     */
    private static void addAliasesForSingleClass(@Nonnull Class newClass, String oldClassName, String beforeBrand) {
        handleReceivedWrongParameters(newClass, oldClassName, beforeBrand);
        invokeXstreamCompatibilityAlias(newClass, oldClassName);
    }

    /**
     * invokeXstreamCompatibilityAlias invokes the XSTREAM2 functions required for the rebranding
     */
    private static void invokeXstreamCompatibilityAlias(@Nonnull Class newClass, String oldHpeClassName) {
        Items.XSTREAM2.addCompatibilityAlias(oldHpeClassName, newClass);
        Run.XSTREAM2.addCompatibilityAlias(oldHpeClassName, newClass);
    }

    /**
     * handleReceivedWrongParameters throws exception when the passed newClass doesn't contain any of the package names
     * we trying to add alias to
     *
     * @throws IllegalArgumentException
     */
    private static void handleReceivedWrongParameters(@Nonnull Class newClass, String oldClassName, String beforeBrand) throws IllegalArgumentException {
        if (!oldClassName.contains(beforeBrand))
            throw new IllegalArgumentException(String.format("The %s doesn't contain: %s", newClass.toString(), beforeBrand));
    }
}