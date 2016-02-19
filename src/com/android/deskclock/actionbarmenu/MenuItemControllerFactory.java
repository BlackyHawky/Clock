/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.deskclock.actionbarmenu;

import android.app.Activity;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory that builds optional {@link MenuItemController} instances.
 */
public final class MenuItemControllerFactory {

    private static final MenuItemControllerFactory INSTANCE = new MenuItemControllerFactory();

    public static MenuItemControllerFactory getInstance() {
        return INSTANCE;
    }

    private final List<MenuItemProvider> mMenuItemProviders;

    private MenuItemControllerFactory() {
        mMenuItemProviders = new ArrayList<>();
    }

    public MenuItemControllerFactory addMenuItemProvider(MenuItemProvider provider) {
        mMenuItemProviders.add(provider);
        return this;
    }

    public MenuItemController[] buildMenuItemControllers(Activity activity) {
        final int providerSize = mMenuItemProviders.size();
        final MenuItemController[] controllers = new MenuItemController[providerSize];
        for (int i = 0; i < providerSize; i++) {
            controllers[i] = mMenuItemProviders.get(i).provide(activity);
        }
        return controllers;
    }
}
