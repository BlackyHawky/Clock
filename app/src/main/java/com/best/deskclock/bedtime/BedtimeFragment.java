package com.best.deskclock.bedtime;


import static android.view.View.INVISIBLE;
import static com.best.deskclock.uidata.UiDataModel.Tab.BEDTIME;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.best.deskclock.DeskClockFragment;
import com.best.deskclock.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Fragment that shows the bedtime.
 // */
public final class BedtimeFragment extends DeskClockFragment {

    View view;

    /** The public no-arg constructor required by all fragments. */
    public BedtimeFragment() { super(BEDTIME); }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.bedtime_fragment, container, false);

        TextView ringtone = view.findViewById(R.id.choose_ringtone);
        TextView txtBedtime = view.findViewById(R.id.bedtime_time);
        TextView txtWakeup = view.findViewById(R.id.wakeup_time);
        TextView[] textViews = new TextView[]{ txtBedtime, txtWakeup };
        TextView hours_of_sleep_text = (TextView) view.findViewById(R.id.hours_of_sleep);

        hours_of_sleep_text.setText(hoursOfSleep());
        buildButton();

        // Sets the Click Listener for the bedtime and wakeup time
        for (TextView time: textViews ) {
            time.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (txtBedtime.equals(time)) {
                        showBedtimeBottomSheetDialog();
                    } else if (txtWakeup.equals(time)) {
                        showWakeupBottomSheetDialog();
                    }}});}

//        // Ringtone editor handler
//        ringtone.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Toast.makeText(getContext(), "TEST", Toast.LENGTH_SHORT).show();
//            }
//        });




        return view;
    }

    // Shows the bottom sheet for wakeup
    private void showWakeupBottomSheetDialog() {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
        bottomSheetDialog.setContentView(R.layout.wakeup_bottom_sheet);

        bottomSheetDialog.show();
    }

    //Shows the bottom sheet for bedtime
    public void showBedtimeBottomSheetDialog() {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
        bottomSheetDialog.setContentView(R.layout.bedtime_bottom_sheet);

        bottomSheetDialog.show();
    }


    // Build button for each day.
    private void buildButton(){
//         FIXME: 2022-07-05
//        LinearLayout repeatDays = view.findViewById(R.id.repeat_days_bedtime);
//        final LayoutInflater inflaters = LayoutInflater.from(getContext());
//        final List<Integer> weekdays = DataModel.getDataModel().getWeekdayOrder().getCalendarDays();
//        // Build button for each day.
//        for (int i = 0; i < 7; i++) {
//            final View dayButtonFrame = inflaters.inflate(R.layout.day_button, repeatDays,
//                    false /* attachToRoot */);
//            final CompoundButton dayButton = dayButtonFrame.findViewById(R.id.day_button_box);
//            final int weekday = weekdays.get(i);
//            dayButton.setChecked(true);
//            dayButton.setTextColor(ThemeUtils.resolveColor(getContext(),
//                    android.R.attr.textColorPrimaryInverse));
//            dayButton.setText(UiDataModel.getUiDataModel().getShortWeekday(weekday));
//            dayButton.setContentDescription(UiDataModel.getUiDataModel().getLongWeekday(weekday));
//            repeatDays.addView(dayButtonFrame);
//            dayButtons[i] = dayButton;
//        }
//        // Day buttons handler
//        for (int i = 0; i < dayButtons.length; i++) {
//            final int buttonIndex = i;
//            dayButtons[i].setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    final boolean isChecked = ((CompoundButton) view).isChecked();
//                    getAlarmTimeClickHandler().setDayOfWeekEnabled(getItemHolder().item,
//                            isChecked, buttonIndex);
//                }
//            });
//        }

    }
    // Calculates the different between the time times
    private String hoursOfSleep() {
        TextView bedtime_time = (TextView) view.findViewById(R.id.bedtime_time);
        TextView wakeup_time = (TextView) view.findViewById(R.id.wakeup_time);

        // HH converts hour in 24 hours format (0-23), day calculation
        SimpleDateFormat format = new SimpleDateFormat("HH:mm ");

        long diffMinutes = 0;
        long diffHours = 0;

        try {

            Date d1 = format.parse(String.valueOf(bedtime_time));
            Date d2 = format.parse(String.valueOf(wakeup_time));

            // In milliseconds
            long diff = d2.getTime() - d1.getTime();

            diffMinutes = diff / (60 * 1000) % 60;
            diffHours = diff / (60 * 60 * 1000) % 24;


        } catch (ParseException e) {
            e.printStackTrace();
        }

        // Removes min if it's zero for a nicer look
        if (diffMinutes == 0){
            return diffHours + "h";
        }
        else{
            return diffHours + "h " + diffMinutes + "min";
        }
    }


    @Override
    public void onUpdateFab(@NonNull ImageView fab) { fab.setVisibility(INVISIBLE); }

    @Override
    public void onUpdateFabButtons(@NonNull Button left, @NonNull Button right) {
        left.setVisibility(INVISIBLE);
        left.setClickable(false);

        right.setVisibility(INVISIBLE);
        right.setClickable(false);
    }

    @Override
    public void onFabClick(@NonNull ImageView fab) {}

}