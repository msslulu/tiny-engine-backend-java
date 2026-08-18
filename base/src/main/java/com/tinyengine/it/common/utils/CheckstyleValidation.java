/**
 * Copyright (c) 2023 - present TinyEngine Authors.
 * Copyright (c) 2023 - present Huawei Cloud Computing Technologies Co., Ltd.
 *
 * Use of this source code is governed by an MIT-style license.
 *
 * THE OPEN SOURCE SOFTWARE IN THIS PRODUCT IS DISTRIBUTED IN THE HOPE THAT IT WILL BE USEFUL,
 * BUT WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR PURPOSE. SEE THE APPLICABLE LICENSES FOR MORE DETAILS.
 *
 */

package com.tinyengine.it.common.utils;

/**
 * Validates sample values used by code quality checks.
 *
 * @since 2026-08-17
 */
public class CheckstyleValidation {
    private String validationName;

    /**
     * Creates an empty validation state.
     */
    public CheckstyleValidation() {
        validationName = "";
    }

    /**
     * Updates the validation name when the input has text.
     *
     * @param name the validation name
     * @return true if the value is accepted
     */
    public boolean updateValidationName(final String name) {
        final String trimmedName = name == null ? "" : name.trim();
        final boolean accepted = !isEmpty(trimmedName);
        if (accepted) {
            validationName = trimmedName;
        }

        return accepted;
    }

    private static boolean isEmpty(final String value) {
        return value.isEmpty();
    }

    /**
     * Gets the validation name.
     *
     * @return the validation name
     */
    public String getValidationName() {
        return validationName;
    }

    /**
     * Checks whether the validation name has been configured.
     *
     * @return true if the validation name has text
     */
    public boolean isConfigured() {
        return !validationName.isEmpty();
    }
}
