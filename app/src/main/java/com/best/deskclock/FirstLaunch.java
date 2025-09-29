// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.best.deskclock.settings.PermissionsManagementActivity;
import com.best.deskclock.utils.InsetsUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class FirstLaunch extends BaseActivity {

    public static final String KEY_IS_FIRST_LAUNCH = "key_is_first_launch";

    View mFirstLaunchRootView;
    View mFirstLaunchContent;

    TextView mAppTitle;
    TextView mAppVersion;
    TextView mMainFeaturesText;
    TextView mImportantInfoText;
    Button mNowButton;
    Button mLaterButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPreferences = getDefaultSharedPreferences(this);

        // To manually manage insets
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        ThemeUtils.allowDisplayCutout(getWindow());

        setContentView(R.layout.first_launch_activity);

        mFirstLaunchRootView = findViewById(R.id.first_launch_root_view);
        mFirstLaunchContent = findViewById(R.id.first_launch_content);
        mAppTitle = findViewById(R.id.first_launch_app_title);
        mAppVersion = findViewById(R.id.first_launch_app_version);
        mMainFeaturesText = findViewById(R.id.first_launch_main_features_text);
        mImportantInfoText = findViewById(R.id.first_launch_important_info_text);
        mNowButton = findViewById(R.id.now_button);
        mLaterButton = findViewById(R.id.later_button);

        setupTitle();

        setupVersion();

        setupMainFeaturesText();

        setupImportantInfoMessage();

        mNowButton.setOnClickListener(v -> {
            sharedPreferences.edit().putBoolean(KEY_IS_FIRST_LAUNCH, false).apply();
            finish();
            startActivity(new Intent(this, DeskClock.class));
            startActivity(new Intent(this, PermissionsManagementActivity.class));
        });

        mLaterButton.setOnClickListener(v -> {
            sharedPreferences.edit().putBoolean(KEY_IS_FIRST_LAUNCH, false).apply();
            finish();
            startActivity(new Intent(this, DeskClock.class));
        });

        applyWindowInsets();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showDialogToQuit();
            }
        });
    }

    /**
     * This method adjusts the space occupied by system elements (such as the status bar,
     * navigation bar or screen notch) and adjust the display of the application interface
     * accordingly.
     */
    private void applyWindowInsets() {
        InsetsUtils.doOnApplyWindowInsets(mFirstLaunchRootView, (v, insets) -> {
            // Get the system bar and notch insets
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() |
                    WindowInsetsCompat.Type.displayCutout());

            v.setPadding(bars.left, bars.top, bars.right, 0);

            mFirstLaunchContent.setPadding(0, 0, 0, bars.bottom);
        });
    }

    /**
     * Automatically sets the application title according to whether it's the debug version or not.
     */
    private void setupTitle() {
        if (Utils.isDebugConfig()) {
            mAppTitle.setText(R.string.about_debug_app_title);
        } else {
            mAppTitle.setText(R.string.app_label);
        }
    }

    /**
     * Automatically sets the application version according to whether it's the debug version or not.
     */
    private void setupVersion() {
        String versionNumber = BuildConfig.VERSION_NAME;
        mAppVersion.setText(getString(R.string.first_launch_version, versionNumber));
    }

    /**
     * Shows a dialog asking the user whether or not to quit the application.
     */
    private void showDialogToQuit() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.first_launch_dialog_title))
                .setIcon(R.drawable.ic_logout)
                .setMessage(getString(R.string.first_launch_dialog_message))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> finishAffinity())
                .setNegativeButton(android.R.string.cancel, null)
                .setCancelable(false)
                .show();
    }

    /**
     * Points to the GitHub page where you can view all the application's features.
     */
    private void setupMainFeaturesText() {
        String link = ("<a href=\"https://github.com/BlackyHawky/Clock#features-\">"
                + getString(R.string.first_launch_main_feature_link) + "</a>");

        Spanned mainFeaturesMessage;
        if (SdkUtils.isAtLeastAndroid7()) {
            mainFeaturesMessage = Html.fromHtml(
                    getString(R.string.first_launch_main_feature_message, link), Html.FROM_HTML_MODE_LEGACY);
        } else {
            mainFeaturesMessage = Html.fromHtml(getString(R.string.first_launch_main_feature_message, link));
        }
        mMainFeaturesText.setText(mainFeaturesMessage);
        mMainFeaturesText.setMovementMethod(LinkMovementMethod.getInstance());
    }

    /**
     * Define an important message for the first launch.
     */
    private void setupImportantInfoMessage() {
        String android14message;
        if (SdkUtils.isAtLeastAndroid14()) {
            android14message = getString(R.string.first_launch_important_info_message_for_SDK34);
        } else {
            android14message = "";
        }

        Spanned importantInfoMessage;
        if (SdkUtils.isAtLeastAndroid7()) {
            importantInfoMessage = Html.fromHtml(
                    getString(R.string.first_launch_important_info_message, android14message), Html.FROM_HTML_MODE_LEGACY);
        } else {
            importantInfoMessage = Html.fromHtml(getString(R.string.first_launch_important_info_message, android14message));
        }

        mImportantInfoText.setText(importantInfoMessage);
    }

}
