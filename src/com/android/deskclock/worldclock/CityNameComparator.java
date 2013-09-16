/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.android.deskclock.worldclock;

import java.text.Collator;
import java.util.Comparator;

public class CityNameComparator implements Comparator<CityObj> {

    private Collator mCollator;

    public CityNameComparator() {
        mCollator = Collator.getInstance();
    }

    @Override
    public int compare(CityObj c1, CityObj c2) {
        return mCollator.compare(c1.mCityName, c2.mCityName);
    }
}
