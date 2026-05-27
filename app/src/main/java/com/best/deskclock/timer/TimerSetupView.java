/*
 * Copyright (C) 2008 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.timer;

import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.AMOLED_DARK_MODE;
import static com.best.deskclock.uicomponents.FabContainer.FAB_REQUEST_FOCUS;
import static com.best.deskclock.uicomponents.FabContainer.FAB_SHRINK_AND_EXPAND;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.BidiFormatter;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.databinding.TimerSetupViewBinding;
import com.best.deskclock.uicomponents.FabContainer;
import com.best.deskclock.uidata.UiDataModel;
import com.best.deskclock.utils.FormattedTextUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;

import java.io.Serializable;
import java.util.Arrays;

public class TimerSetupView extends LinearLayout implements View.OnClickListener, View.OnLongClickListener {

    private final TimerSetupViewBinding mBinding;

    private final int[] mInput = {0, 0, 0, 0, 0, 0};
    private final CharSequence mTimeTemplate;
    private int mInputPointer = -1;
    private MaterialButton[] mDigitButton;

    /**
     * Updates to the fab are requested via this container.
     */
    private FabContainer mFabContainer;

    public TimerSetupView(Context context) {
        this(context, null);
    }

    public TimerSetupView(Context context, AttributeSet attrs) {
        super(context, attrs);

        final BidiFormatter bf = BidiFormatter.getInstance(false /* rtlContext */);
        final String hoursLabel = bf.unicodeWrap(context.getString(R.string.hours_label));
        final String minutesLabel = bf.unicodeWrap(context.getString(R.string.minutes_label));
        final String secondsLabel = bf.unicodeWrap(context.getString(R.string.seconds_label));

        // Create a formatted template for "00h 00m 00s".
        mTimeTemplate = TextUtils.expandTemplate("^1^4 ^2^5 ^3^6",
            bf.unicodeWrap("^1"),
            bf.unicodeWrap("^2"),
            bf.unicodeWrap("^3"),
            FormattedTextUtils.formatText(hoursLabel, new RelativeSizeSpan(0.5f)),
            FormattedTextUtils.formatText(minutesLabel, new RelativeSizeSpan(0.5f)),
            FormattedTextUtils.formatText(secondsLabel, new RelativeSizeSpan(0.5f)));

        mBinding = TimerSetupViewBinding.inflate(LayoutInflater.from(context), this, true);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        final SharedPreferences prefs = getDefaultSharedPreferences(getContext());
        final Typeface generalTypeface = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(prefs));
        final Typeface timerTypeface = ThemeUtils.loadFont(SettingsDAO.getTimerDurationFont(prefs));
        final DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

        final boolean isCardBackgroundDisplayed = SettingsDAO.isCardBackgroundDisplayed(prefs);
        final boolean isCardBorderDisplayed = SettingsDAO.isCardBorderDisplayed(prefs);
        final String darkMode = SettingsDAO.getDarkMode(prefs);
        final boolean isNight = ThemeUtils.isNight(getResources());

        mDigitButton = new MaterialButton[]{
            findViewById(R.id.timer_setup_digit_0),
            findViewById(R.id.timer_setup_digit_1),
            findViewById(R.id.timer_setup_digit_2),
            findViewById(R.id.timer_setup_digit_3),
            findViewById(R.id.timer_setup_digit_4),
            findViewById(R.id.timer_setup_digit_5),
            findViewById(R.id.timer_setup_digit_6),
            findViewById(R.id.timer_setup_digit_7),
            findViewById(R.id.timer_setup_digit_8),
            findViewById(R.id.timer_setup_digit_9),
        };

        mBinding.timerSetupTimeLayout.timerSetupTime.setTypeface(timerTypeface);

        for (final MaterialButton digitButton : mDigitButton) {
            digitButton.setTypeface(generalTypeface);

            if (isCardBackgroundDisplayed) {
                digitButton.setBackgroundTintList(ColorStateList.valueOf(MaterialColors.getColor(
                    getContext(), com.google.android.material.R.attr.colorSurface, Color.BLACK)));
            } else if (isNight && darkMode.equals(AMOLED_DARK_MODE)) {
                digitButton.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
            } else {
                digitButton.setBackgroundTintList(ColorStateList.valueOf(MaterialColors.getColor(
                    getContext(), android.R.attr.colorBackground, Color.BLACK)));
                digitButton.setStateListAnimator(null);
            }

            if (isCardBorderDisplayed) {
                digitButton.setStrokeWidth((int) dpToPx(2, displayMetrics));
                digitButton.setStrokeColor(ColorStateList.valueOf(MaterialColors.getColor(
                    getContext(), androidx.appcompat.R.attr.colorPrimary, Color.BLACK)));
            }

            digitButton.setOnClickListener(this);
        }

        MaterialButton doubleZeroButton = findViewById(R.id.timer_setup_digit_00);
        doubleZeroButton.setTypeface(generalTypeface);

        if (isCardBackgroundDisplayed) {
            doubleZeroButton.setBackgroundTintList(ColorStateList.valueOf(MaterialColors.getColor(
                getContext(), com.google.android.material.R.attr.colorPrimaryContainer, Color.BLACK)));
            mBinding.timerSetupDigitsLayout.timerSetupDelete.setBackgroundTintList(ColorStateList.valueOf(MaterialColors.getColor(
                getContext(), com.google.android.material.R.attr.colorPrimaryContainer, Color.BLACK)));
        } else if (isNight && darkMode.equals((AMOLED_DARK_MODE))) {
            doubleZeroButton.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
            mBinding.timerSetupDigitsLayout.timerSetupDelete.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
        } else {
            doubleZeroButton.setBackgroundTintList(ColorStateList.valueOf(MaterialColors.getColor(
                getContext(), android.R.attr.colorBackground, Color.BLACK)));
            doubleZeroButton.setStateListAnimator(null);
            mBinding.timerSetupDigitsLayout.timerSetupDelete.setBackgroundTintList(ColorStateList.valueOf(MaterialColors.getColor(
                getContext(), android.R.attr.colorBackground, Color.BLACK)));
            mBinding.timerSetupDigitsLayout.timerSetupDelete.setStateListAnimator(null);
        }

        if (isCardBorderDisplayed) {
            doubleZeroButton.setStrokeWidth((int) dpToPx(2, displayMetrics));
            doubleZeroButton.setStrokeColor(ColorStateList.valueOf(MaterialColors.getColor(
                getContext(), com.google.android.material.R.attr.colorPrimaryInverse, Color.BLACK)));
            mBinding.timerSetupDigitsLayout.timerSetupDelete.setStrokeWidth((int) dpToPx(2, displayMetrics));
            mBinding.timerSetupDigitsLayout.timerSetupDelete.setStrokeColor(ColorStateList.valueOf(MaterialColors.getColor(
                getContext(), com.google.android.material.R.attr.colorPrimaryInverse, Color.BLACK)));
        }

        doubleZeroButton.setOnClickListener(this);

        mBinding.timerSetupDigitsLayout.timerSetupDelete.setTypeface(generalTypeface);
        mBinding.timerSetupDigitsLayout.timerSetupDelete.setOnClickListener(this);
        mBinding.timerSetupDigitsLayout.timerSetupDelete.setOnLongClickListener(this);

        updateTime();
        updateDeleteAndDivider();
    }

    public void setFabContainer(FabContainer fabContainer) {
        mFabContainer = fabContainer;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        View view = null;
        if (keyCode == KeyEvent.KEYCODE_DEL) {
            view = mBinding.timerSetupDigitsLayout.timerSetupDelete;
        } else if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
            view = mDigitButton[keyCode - KeyEvent.KEYCODE_0];
        }

        if (view != null) {
            final boolean result = view.performClick();
            if (result && hasValidInput()) {
                mFabContainer.updateFab(FAB_REQUEST_FOCUS);
            }
            return result;
        }

        return false;
    }

    @Override
    public void onClick(View view) {
        if (view == mBinding.timerSetupDigitsLayout.timerSetupDelete) {
            delete();
        } else if (view.getId() == R.id.timer_setup_digit_00) {
            append(0);
            append(0);
        } else {
            append(getDigitForId(view.getId()));
        }
        Utils.setVibrationTime(getContext(), 10);
    }

    @Override
    public boolean onLongClick(View view) {
        if (view == mBinding.timerSetupDigitsLayout.timerSetupDelete) {
            Utils.setVibrationTime(getContext(), 10);
            reset();
            updateFab();
            return true;
        }
        return false;
    }

    private int getDigitForId(int id) {
        if (id == R.id.timer_setup_digit_0) {
            return 0;
        } else if (id == R.id.timer_setup_digit_1) {
            return 1;
        } else if (id == R.id.timer_setup_digit_2) {
            return 2;
        } else if (id == R.id.timer_setup_digit_3) {
            return 3;
        } else if (id == R.id.timer_setup_digit_4) {
            return 4;
        } else if (id == R.id.timer_setup_digit_5) {
            return 5;
        } else if (id == R.id.timer_setup_digit_6) {
            return 6;
        } else if (id == R.id.timer_setup_digit_7) {
            return 7;
        } else if (id == R.id.timer_setup_digit_8) {
            return 8;
        } else if (id == R.id.timer_setup_digit_9) {
            return 9;
        }
        throw new IllegalArgumentException("Invalid id: " + id);
    }

    private void updateTime() {
        final int seconds = mInput[1] * 10 + mInput[0];
        final int minutes = mInput[3] * 10 + mInput[2];
        final int hours = mInput[5] * 10 + mInput[4];

        final UiDataModel uiDataModel = UiDataModel.getUiDataModel();
        SpannableString text = new SpannableString(TextUtils.expandTemplate(mTimeTemplate,
            uiDataModel.getFormattedNumber(hours, 2),
            uiDataModel.getFormattedNumber(minutes, 2),
            uiDataModel.getFormattedNumber(seconds, 2)));

        int endIdx = text.length();
        int startIdx = seconds > 0 ? 8 : endIdx;
        startIdx = minutes > 0 ? 4 : startIdx;
        startIdx = hours > 0 ? 0 : startIdx;
        if (startIdx != endIdx) {
            int highlightColor = MaterialColors.getColor(getContext(), androidx.appcompat.R.attr.colorPrimary, Color.BLACK);
            text.setSpan(new ForegroundColorSpan(highlightColor), startIdx, endIdx, 0);
        }
        mBinding.timerSetupTimeLayout.timerSetupTime.setText(text);
        mBinding.timerSetupTimeLayout.timerSetupTime.setContentDescription(
            getResources().getString(R.string.timer_setup_description,
            getResources().getQuantityString(R.plurals.hours, hours, hours),
            getResources().getQuantityString(R.plurals.minutes, minutes, minutes),
            getResources().getQuantityString(R.plurals.seconds, seconds, seconds))
        );
    }

    private void updateDeleteAndDivider() {
        final boolean enabled = hasValidInput();
        mBinding.timerSetupDigitsLayout.timerSetupDelete.setEnabled(enabled);
    }

    private void updateFab() {
        mFabContainer.updateFab(FAB_SHRINK_AND_EXPAND);
    }

    private void append(int digit) {
        if (digit < 0 || digit > 9) {
            throw new IllegalArgumentException("Invalid digit: " + digit);
        }

        // Pressing "0" as the first digit does nothing.
        if (mInputPointer == -1 && digit == 0) {
            return;
        }

        // No space for more digits, so ignore input.
        if (mInputPointer == mInput.length - 1) {
            return;
        }

        // Append the new digit.
        System.arraycopy(mInput, 0, mInput, 1, mInputPointer + 1);
        mInput[0] = digit;
        mInputPointer++;
        updateTime();

        // Update TalkBack to read the number being deleted.
        mBinding.timerSetupDigitsLayout.timerSetupDelete.setContentDescription(getContext().getString(
            R.string.timer_descriptive_delete,
            UiDataModel.getUiDataModel().getFormattedNumber(digit))
        );

        // Update the fab, delete, and divider when we have valid input.
        if (mInputPointer == 0) {
            updateFab();
            updateDeleteAndDivider();
        }
    }

    private void delete() {
        // Nothing exists to delete so return.
        if (mInputPointer < 0) {
            return;
        }

        System.arraycopy(mInput, 1, mInput, 0, mInputPointer);
        mInput[mInputPointer] = 0;
        mInputPointer--;
        updateTime();

        // Update TalkBack to read the number being deleted or its original description.
        if (mInputPointer >= 0) {
            mBinding.timerSetupDigitsLayout.timerSetupDelete.setContentDescription(getContext().getString(
                R.string.timer_descriptive_delete,
                UiDataModel.getUiDataModel().getFormattedNumber(mInput[0])));
        } else {
            mBinding.timerSetupDigitsLayout.timerSetupDelete.setContentDescription(getContext().getString(R.string.delete));
        }

        // Update the fab, delete, and divider when we no longer have valid input.
        if (mInputPointer == -1) {
            updateFab();
            updateDeleteAndDivider();
        }
    }

    public void reset() {
        if (mInputPointer != -1) {
            Arrays.fill(mInput, 0);
            mInputPointer = -1;
            updateTime();
            updateDeleteAndDivider();
        }
    }

    public boolean hasValidInput() {
        return mInputPointer != -1;
    }

    public long getTimeInMillis() {
        final int seconds = mInput[1] * 10 + mInput[0];
        final int minutes = mInput[3] * 10 + mInput[2];
        final int hours = mInput[5] * 10 + mInput[4];
        return seconds * DateUtils.SECOND_IN_MILLIS
            + minutes * DateUtils.MINUTE_IN_MILLIS
            + hours * DateUtils.HOUR_IN_MILLIS;
    }

    /**
     * @return an opaque representation of the state of timer setup
     */
    public Serializable getState() {
        return Arrays.copyOf(mInput, mInput.length);
    }

    /**
     * @param state an opaque state of this view previously produced by {@link #getState()}
     */
    public void setState(Serializable state) {
        final int[] input = (int[]) state;
        if (input != null && mInput.length == input.length) {
            for (int i = 0; i < mInput.length; i++) {
                mInput[i] = input[i];
                if (mInput[i] != 0) {
                    mInputPointer = i;
                }
            }
            updateTime();
            updateDeleteAndDivider();
        }
    }
}
