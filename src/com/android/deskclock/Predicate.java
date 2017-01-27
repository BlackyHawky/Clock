/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.deskclock;

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

    boolean apply(T t);

    /**
     * An implementation of the predicate interface that always returns true.
     */
    Predicate TRUE = new Predicate() {
        @Override
        public boolean apply(Object o) {
            return true;
        }
    };

    /**
     * An implementation of the predicate interface that always returns false.
     */
    Predicate FALSE = new Predicate() {
        @Override
        public boolean apply(Object o) {
            return false;
        }
    };
}