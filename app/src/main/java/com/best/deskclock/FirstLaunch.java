// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;

import androidx.activity.OnBackPressedCallback;
import androidx.core.graphics.Insets;
import androidx.core.text.HtmlCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.best.deskclock.databinding.FirstLaunchActivityBinding;
import com.best.deskclock.settings.PermissionsManagementActivity;
import com.best.deskclock.utils.InsetsUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class FirstLaunch extends BaseActivity {

    public static final String KEY_IS_FIRST_LAUNCH = "key_is_first_launch";

    private FirstLaunchActivityBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = FirstLaunchActivityBinding.inflate(getLayoutInflater());

        SharedPreferences sharedPreferences = getDefaultSharedPreferences(this);

        // To manually manage insets
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        ThemeUtils.allowDisplayCutout(getWindow());

        setContentView(mBinding.getRoot());

        setupTitle();

        setupVersion();

        setupMainFeaturesText();

        setupImportantInfoMessage();

        mBinding.nowButton.setOnClickListener(v -> {
            sharedPreferences.edit().putBoolean(KEY_IS_FIRST_LAUNCH, false).apply();
            finish();
            startActivity(new Intent(this, DeskClock.class));
            startActivity(new Intent(this, PermissionsManagementActivity.class));
        });

        mBinding.laterButton.setOnClickListener(v -> {
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

    @Override
    protected void onDestroy() {
        mBinding = null;

        super.onDestroy();
    }

    /**
     * This method adjusts the space occupied by system elements (such as the status bar,
     * navigation bar or screen notch) and adjust the display of the application interface
     * accordingly.
     */
    private void applyWindowInsets() {
        InsetsUtils.doOnApplyWindowInsets(mBinding.firstLaunchRootView, (v, insets) -> {
            // Get the system bar and notch insets
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());

            v.setPadding(bars.left, bars.top, bars.right, 0);

            mBinding.firstLaunchContent.setPadding(0, 0, 0, bars.bottom);
        });
    }

    /**
     * Automatically sets the application title according to whether it's the debug version or not.
     */
    private void setupTitle() {
        mBinding.firstLaunchAppTitle.setText(Utils.getStringResByBuildType(
            R.string.app_label, R.string.app_label_debug, R.string.app_label_nightly)
        );
    }

    /**
     * Automatically sets the application version according to whether it's the debug version or not.
     */
    private void setupVersion() {
        String versionNumber = BuildConfig.VERSION_NAME;
        mBinding.firstLaunchAppVersion.setText(getString(R.string.first_launch_version, versionNumber));
    }

    /**
     * Shows a dialog asking the user whether to quit the application.
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

        Spanned mainFeaturesMessage = HtmlCompat.fromHtml(getString(R.string.first_launch_main_feature_message, link),
            HtmlCompat.FROM_HTML_MODE_LEGACY);

        mBinding.firstLaunchMainFeaturesText.setText(mainFeaturesMessage);
        mBinding.firstLaunchMainFeaturesText.setMovementMethod(LinkMovementMethod.getInstance());
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

        Spanned importantInfoMessage = HtmlCompat.fromHtml(getString(R.string.first_launch_important_info_message, android14message),
            HtmlCompat.FROM_HTML_MODE_LEGACY);

        mBinding.firstLaunchImportantInfoText.setText(importantInfoMessage);
    }

}
