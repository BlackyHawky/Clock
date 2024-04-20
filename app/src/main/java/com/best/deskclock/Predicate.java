/*
 * Copyright (C) 2017 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock;

import android.content.Context;

/**
 * A Predicate can determine a true or false value for any input of its
 * parameterized type. For example, a {@code RegexPredicate} might implement
 * {@code Predicate<String>}, and return true for any String that matches its
 * given regular expression.
 * <p/>
 * <p/>
 * Implementors of Predicate which may cause side effects upon evaluation are
 * strongly encouraged to state this fact clearly in their API documentation.
 */
public interface Predicate<T> {

    /**
     * An implementation of the predicate interface that always returns true.
     */
    Predicate<Context> TRUE = o -> true;
    /**
     * An implementation of the predicate interface that always returns false.
     */
    Predicate<Context> FALSE = o -> false;

    boolean apply(T t);
}
