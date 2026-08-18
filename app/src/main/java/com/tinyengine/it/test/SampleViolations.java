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

package com.tinyengine.it.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sample class used to validate code quality checks.
 *
 * @since 2026-08-17
 */
public class SampleViolations {
    private final List<String> messages;

    /**
     * Creates an empty message collection.
     */
    public SampleViolations() {
        messages = new ArrayList<>();
    }

    /**
     * Adds a non-blank message.
     *
     * @param message the message
     * @return true if the message is added
     */
    public boolean addMessage(final String message) {
        final String trimmedMessage = message == null ? "" : message.trim();
        final boolean added = !isEmpty(trimmedMessage);
        if (added) {
            messages.add(trimmedMessage);
        }

        return added;
    }

    private static boolean isEmpty(final String value) {
        return value.isEmpty();
    }

    /**
     * Gets all collected messages.
     *
     * @return the collected messages
     */
    public List<String> getMessages() {
        return Collections.unmodifiableList(new ArrayList<>(messages));
    }

    /**
     * Gets the number of collected messages.
     *
     * @return the message count
     */
    public int getMessageCount() {
        return messages.size();
    }
}
