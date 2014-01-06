package com.android.deskclock;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.android.deskclock.provider.Alarm;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ExtensionsFactory {

    private static String TAG = "ExtensionsFactory";
    // Config filename for mappings of various class names to their custom
    // implementations.
    private static String EXTENSIONS_PROPERTIES = "deskclock_extensions.properties";
    private static String DESKCLOCKEXTENSIONS_KEY = "DeskclockExtensions";
    private static Properties sProperties = new Properties();
    private static DeskClockExtensions sDeskClockExtensions = null;

    public static void init(AssetManager assetManager) {
        try {
            InputStream fileStream = assetManager.open(EXTENSIONS_PROPERTIES);
            sProperties.load(fileStream);
            fileStream.close();
        } catch (FileNotFoundException e) {
            // No custom extensions. Ignore.
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "No custom extensions.");
            }
        } catch (IOException e) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, e.toString());
            }
        }
    }

    private static <T> T createInstance(String className) {
        try {
            Class<?> c = Class.forName(className);
            return (T) c.newInstance();
        } catch (ClassNotFoundException e) {
            if (Log.isLoggable(TAG, Log.ERROR)) {
                Log.e(TAG, className + ": unable to create instance.", e);
            }
        } catch (IllegalAccessException e) {
            if (Log.isLoggable(TAG, Log.ERROR)) {
                Log.e(TAG, className + ": unable to create instance.", e);
            }
        } catch (InstantiationException e) {
            if (Log.isLoggable(TAG, Log.ERROR)) {
                Log.e(TAG, className + ": unable to create instance.", e);
            }
        }
        return null;
    }

    public static DeskClockExtensions getDeskClockExtensions() {
        if ((sDeskClockExtensions != null)) {
            return sDeskClockExtensions;
        }

        String className = sProperties.getProperty(DESKCLOCKEXTENSIONS_KEY);
        if (className != null) {
            sDeskClockExtensions = createInstance(className);
        } else {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, DESKCLOCKEXTENSIONS_KEY + " not found in properties file.");
            }
        }

        if (sDeskClockExtensions == null) {
            sDeskClockExtensions = new DeskClockExtensions() {
                @Override
                public void addAlarm(Context context, Alarm newAlarm) {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Add alarm: Empty inline implementation called.");
                    }
                }

                @Override
                public void deleteAlarm(Context context, long alarmId) {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Delete alarm: Empty inline implementation called.");
                    }
                }
            };
        }
        return sDeskClockExtensions;
    }
}
