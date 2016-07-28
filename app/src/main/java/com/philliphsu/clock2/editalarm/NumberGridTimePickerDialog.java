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
 * limitations under the License
 */

package com.philliphsu.clock2.editalarm;

import android.animation.ObjectAnimator;
import android.app.ActionBar.LayoutParams;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.GridLayout;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.philliphsu.clock2.R;
import com.philliphsu.clock2.aospdatetimepicker.Utils;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;

import butterknife.Bind;
import butterknife.OnClick;

import static com.philliphsu.clock2.util.ConversionUtils.dpToPx;

//import com.android.datetimepicker.HapticFeedbackController;
//import com.android.datetimepicker.R;
//import com.android.datetimepicker.Utils;
//import com.android.datetimepicker.time.RadialPickerLayout.OnValueSelectedListener;

/**
 * Dialog to set a time.
 */
public class NumberGridTimePickerDialog extends BaseTimePickerDialog /*DialogFragment implements OnValueSelectedListener*/{
    private static final String TAG = "TimePickerDialog";

    private static final String KEY_HOUR_OF_DAY = "hour_of_day";
    private static final String KEY_MINUTE = "minute";
    private static final String KEY_IS_24_HOUR_VIEW = "is_24_hour_view";
    private static final String KEY_CURRENT_ITEM_SHOWING = "current_item_showing";
    private static final String KEY_IN_KB_MODE = "in_kb_mode";
    private static final String KEY_TYPED_TIMES = "typed_times";
    private static final String KEY_DARK_THEME = "dark_theme";

    public static final int HOUR_INDEX = 0;
    public static final int MINUTE_INDEX = 1;
    // NOT a real index for the purpose of what's showing.
    public static final int AMPM_INDEX = 2;
    // Also NOT a real index, just used for keyboard mode.
    public static final int ENABLE_PICKER_INDEX = 3;
    /**
     * TODO: (Me) Use HALF_DAY_1 instead
     */
    public static final int AM = 0;
    /**
     * TODO: (Me) Use HALF_DAY_2 instead
     */
    public static final int PM = 1;

    // Delay before starting the pulse animation, in ms.
    private static final int PULSE_ANIMATOR_DELAY = 300;
//    private OnTimeSetListener mCallback;

//    private HapticFeedbackController mHapticFeedbackController;

//TODO: Delete    private TextView mDoneButton;
    private TextView mHourView;
    private TextView mHourSpaceView;
    private TextView mMinuteView;
    private TextView mMinuteSpaceView;
    private TextView mAmPmTextView;
    private View mAmPmHitspace;
//    private RadialPickerLayout mTimePicker;

    private int mSelectedColor;
    private int mUnselectedColor;
    private String mAmText;
    private String mPmText;

    private boolean mAllowAutoAdvance;
    private int mInitialHourOfDay;
    private int mInitialMinute;
    private boolean mIs24HourMode;
    private boolean mThemeDark;

    // For hardware IME input.
    private char mPlaceholderText;
    private String mDoublePlaceholderText;
    private String mDeletedKeyFormat;
    private boolean mInKbMode;
    private ArrayList<Integer> mTypedTimes;
    private Node mLegalTimesTree;
    private int mAmKeyCode;
    private int mPmKeyCode;

    // Accessibility strings.
    private String mHourPickerDescription;
    private String mSelectHours;
    private String mMinutePickerDescription;
    private String mSelectMinutes;

    // ====================================== MY STUFF =============================================
    private static final int[] HOURS_12 = {1,2,3,4,5,6,7,8,9,10,11,12};
    private static final int[] HOURS_24_HALF_DAY_1 = {0,1,2,3,4,5,6,7,8,9,10,11};
    private static final int[] HOURS_24_HALF_DAY_2 = {12,13,14,15,16,17,18,19,20,21,22,23};
    private static final int[] MINUTES = {0,5,10,15,20,25,30,35,40,45,50,55};
    // The delay in ms before a OnLongClick on a TwentyFourHourGridItem defers to OnClick
    private static final int LONG_CLICK_RESULT_LEEWAY = 150;
    // The padding in dp for the half day icon compound drawable
    public static final int HALF_DAY_ICON_PADDING = 8;

    // TODO: Private?
    // Describes both AM/PM in the 12-hour clock and the half-days of the 24-hour clock.
    public static final int HALF_DAY_1 = AM;
    public static final int HALF_DAY_2 = PM;

    private int mCurrentIndex = HOUR_INDEX;
    private int mSelectedHalfDay = HALF_DAY_1;
    private int mSelectedHourOfDay;
    private int mSelectedMinute;
    private Handler mHandler;

    @Bind(R.id.grid_layout) GridLayout mGridLayout;
    @Bind(R.id.fab) FloatingActionButton mDoneButton;
    // These are currently defined as Buttons in the dialog's layout,
    // but we refer to them as TextViews to save an extra refactoring
    // step in case we change them.
    @Bind(R.id.half_day_toggle_1) FrameLayout mLeftHalfDayToggle;
    @Bind(R.id.half_day_toggle_2) FrameLayout mRightHalfDayToggle;

    @Override
    protected int contentLayout() {
        return R.layout.dialog_time_picker_number_grid;
    }

    private void setNumberTexts() {
        if (mCurrentIndex != HOUR_INDEX && mCurrentIndex != MINUTE_INDEX) {
            Log.e(TAG, "TimePicker does not support view at index "+mCurrentIndex);
            return;
        }

        // Set the appropriate texts on each view
        for (int i = 0; i < mGridLayout.getChildCount(); i++) {
            View v = mGridLayout.getChildAt(i);
            if (mCurrentIndex == MINUTE_INDEX || mCurrentIndex == HOUR_INDEX && !mIs24HourMode) {
                if (!(v instanceof TextView))
                    return; // Reached the ImageButtons
                TextView tv = (TextView) v;
                tv.setText(mCurrentIndex == MINUTE_INDEX
                        ? String.format("%02d", MINUTES[i])
                        : String.valueOf(HOURS_12[i]));
            } else if (mCurrentIndex == HOUR_INDEX && mIs24HourMode) {
                TwentyFourHourGridItem item = (TwentyFourHourGridItem) v;
                String s1 = String.format("%02d", HOURS_24_HALF_DAY_1[i]);
                String s2 = String.valueOf(HOURS_24_HALF_DAY_2[i]);
                if (mSelectedHalfDay == HALF_DAY_1) {
                    item.setPrimaryText(s1);
                    item.setSecondaryText(s2);
                } else if (mSelectedHalfDay == HALF_DAY_2) {
                    item.setPrimaryText(s2);
                    item.setSecondaryText(s1);
                } else {
                    Log.e(TAG, "mSelectedHalfDay = " + mSelectedHalfDay + "?");
                }
            }
        }
    }

    // TODO: boolean animate param???
    private void setCurrentItemShowing(int index) {
        if (index != HOUR_INDEX && index != MINUTE_INDEX) {
            Log.e(TAG, "TimePicker does not support view at index "+index);
            return;
        }

        int lastIndex = mCurrentIndex;
        mCurrentIndex = index;

        if (index != lastIndex) {
            if (mIs24HourMode) {
                // Hours layout and normal layout use different Views for their grid items,
                // so we have to start fresh.
                mGridLayout.removeAllViews();
                int layout = index == HOUR_INDEX ? R.layout.content_24h_number_grid : R.layout.content_number_grid;
                View.inflate(getActivity(), layout, mGridLayout);

                // TOneverDO: call after inflating minute tuner buttons
                setNumberTexts();
                setClickListenersOnButtons();
                //end TOneverDO
            } else {
                if (index == HOUR_INDEX) {
                    // Remove the minute tuners
                    mGridLayout.removeViews(mGridLayout.getChildCount() - 2, 2);
                }
                // We can reuse the existing child Views, just change the texts.
                // They already have the click listener set.
                setNumberTexts();
            }

            if (index == MINUTE_INDEX) {
                createMinuteTuners();
            }
        }
    }

    private void setClickListenersOnButtons() {
        for (int i = 0; i < mGridLayout.getChildCount(); i++) {
            // TODO: Consider leaving out the minute tuner buttons
            View v = mGridLayout.getChildAt(i);
            v.setOnClickListener(mOnNumberClickListener);
            if (v instanceof TwentyFourHourGridItem) {
                v.setOnLongClickListener(mOn24HourItemLongClickListener);
            }
        }
    }

    @OnClick({ R.id.half_day_toggle_1, R.id.half_day_toggle_2 })
    void onHalfDayToggleClick(View v) {
        int halfDay = v == mLeftHalfDayToggle ? HALF_DAY_1 : HALF_DAY_2;
        if (halfDay != mSelectedHalfDay) {
            toggleHalfDay();
        }
    }

    private void toggleHalfDay() {
//        int amOrPm = mTimePicker.getIsCurrentlyAmOrPm();
        int amOrPm = mSelectedHalfDay;
        // TODO: Use HALF_DAY_1 and 2 instead
        if (amOrPm == AM) {
            amOrPm = PM;
        } else if (amOrPm == PM){
            amOrPm = AM;
        }
        updateAmPmDisplay(amOrPm);
//        mTimePicker.setAmOrPm(amOrPm);
        mSelectedHalfDay = amOrPm;

        if (mIs24HourMode) {
            if (mCurrentIndex == HOUR_INDEX) {
                for (int i = 0; i < mGridLayout.getChildCount(); i++) {
                    View v = mGridLayout.getChildAt(i);
                    ((TwentyFourHourGridItem) v).swapTexts();
                }
            }
        }

        // TODO: Verify the corresponding TwentyFourHourGridItem retains its indicator
        if (amOrPm == HALF_DAY_1) {
            mSelectedHourOfDay %= 12;
        } else if (amOrPm == HALF_DAY_2) {
            mSelectedHourOfDay = (mSelectedHourOfDay % 12) + 12;
        }
        onValueSelected(HOUR_INDEX, mSelectedHourOfDay, false);
    }

    private void createMinuteTuners() {
        // https://android-developers.blogspot.com/2009/03/android-layout-tricks-3-optimize-by.html
        // "When inflating a layout starting with a <merge />, you *must* specify a parent ViewGroup
        // and you must set attachToRoot to true (see the documentation of the LayoutInflater#inflate() method)"
        // Note that by passing in a non-null parent, this will pass in true for attachToRoot.
        View.inflate(getActivity(), R.layout.content_number_grid_minute_tuners, mGridLayout);
        int childCount = mGridLayout.getChildCount();
        // The tuner buttons are always the last two children in the grid
        mGridLayout.getChildAt(childCount - 2).setOnClickListener(mOnDecrementMinuteListener);
        mGridLayout.getChildAt(childCount - 1).setOnClickListener(mOnIncrementMinuteListener);
    }

    // TODO: Break this into two OnClickListeners instead--one for normal TextViews and
    // the other for TwentyFourHourGridItem.
    // TODO: Set the indicator
    private final OnClickListener mOnNumberClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            String number;
            if (v instanceof TextView) {
                number = ((TextView) v).getText().toString();
            } else if (v instanceof TwentyFourHourGridItem) {
                number = ((TwentyFourHourGridItem) v).getPrimaryText().toString();
            } else {
                Log.e(TAG, "TimePicker does not support button type " + v.getClass().getName());
                return;
            }
            int value = Integer.parseInt(number);
            if (mCurrentIndex == HOUR_INDEX && !mIs24HourMode) {
                if (value == 12 && mSelectedHalfDay == HALF_DAY_1) {
                    value = 0;
                } else if (value != 12 && mSelectedHalfDay == HALF_DAY_2) {
                    value += 12;
                }
            }
            onValueSelected(mCurrentIndex, value, true);
        }
    };

    private final View.OnLongClickListener mOn24HourItemLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(final View v) {
            // TODO: Do we need this if we already check this before setting the listener on the view?
            if (!(v instanceof TwentyFourHourGridItem))
                return false;
            toggleHalfDay();
            mOnNumberClickListener.onClick(v);
            return true;
        }
    };

    private final OnClickListener mOnIncrementMinuteListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            // Don't need to check for minute overflow, because setMinute()
            // sets minute to 0 if we pass in 60
            onValueSelected(MINUTE_INDEX, mSelectedMinute + 1, false);
        }
    };

    private final OnClickListener mOnDecrementMinuteListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            int value = mSelectedMinute - 1;
            if (value < 0)
                value = 59;
            onValueSelected(MINUTE_INDEX, value, false);
        }
    };

    // =============================================================================================

//    /**
//     * The callback interface used to indicate the user is done filling in
//     * the time (they clicked on the 'Set' button).
//     */
//    public interface OnTimeSetListener {
//
//        /**
//         * @param view The view associated with this listener.
//         * @param hourOfDay The hour that was set.
//         * @param minute The minute that was set.
//         */
//        void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute);
//    }

    public NumberGridTimePickerDialog() {
        // Empty constructor required for dialog fragment.
    }

//    public NumberGridTimePickerDialog(Context context, int theme, OnTimeSetListener callback,
//                                        int hourOfDay, int minute, boolean is24HourMode) {
//        // Empty constructor required for dialog fragment.
//    }

    public static NumberGridTimePickerDialog newInstance(OnTimeSetListener callback,
                                                         int hourOfDay, int minute, boolean is24HourMode) {
        NumberGridTimePickerDialog ret = new NumberGridTimePickerDialog();
        ret.initialize(callback, hourOfDay, minute, is24HourMode);
        return ret;
    }

    /**
     * @param timeFieldIndex The index representing the time field whose values, ranging from its natural
     *                       lower and upper limits, will be presented as choices in the GridLayout
     *                       contained in this dialog's layout. Must be one of {@link #HOUR_INDEX}
     *                       or {@link #MINUTE_INDEX}. TODO: Why do we need this?
     * @param initialHalfDay The half-day, a.k.a. AM/PM for 12-hour time, that this picker should be
     *                       initialized to. Must be one of {@link #HALF_DAY_1} or {@link #HALF_DAY_2}.
     *                       TODO: Why do we need this?
     */
    @Deprecated
    public static NumberGridTimePickerDialog newInstance(int timeFieldIndex, int initialHalfDay) {
        NumberGridTimePickerDialog dialog = new NumberGridTimePickerDialog();
        dialog.mCurrentIndex = timeFieldIndex;
        dialog.mSelectedHalfDay = initialHalfDay;
        return dialog;
    }

    public void initialize(OnTimeSetListener callback,
            int hourOfDay, int minute, boolean is24HourMode) {
        mCallback = callback; // TODO: Use setOnTimeSetListener() instead?

        mInitialHourOfDay = hourOfDay;
        mInitialMinute = minute;
        mIs24HourMode = is24HourMode;
        mInKbMode = false;
        mThemeDark = false;

        mSelectedHalfDay = hourOfDay < 12 ? HALF_DAY_1 : HALF_DAY_2;
    }

    /**
     * Set a dark or light theme. NOTE: this will only take effect for the next onCreateView.
     */
    public void setThemeDark(boolean dark) {
        mThemeDark = dark;
    }

    public boolean isThemeDark() {
        return mThemeDark;
    }

//    public void setOnTimeSetListener(OnTimeSetListener callback) {
//        mCallback = callback;
//    }

    public void setStartTime(int hourOfDay, int minute) {
        mInitialHourOfDay = hourOfDay;
        mInitialMinute = minute;
        mInKbMode = false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // The Activity is created at this point
//        mIs24HourMode = DateFormat.is24HourFormat(getActivity());
        mHandler = new Handler();
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_HOUR_OF_DAY)
                    && savedInstanceState.containsKey(KEY_MINUTE)
                    && savedInstanceState.containsKey(KEY_IS_24_HOUR_VIEW)) {
            mInitialHourOfDay = savedInstanceState.getInt(KEY_HOUR_OF_DAY);
            mInitialMinute = savedInstanceState.getInt(KEY_MINUTE);
            mIs24HourMode = savedInstanceState.getBoolean(KEY_IS_24_HOUR_VIEW);
//            mInKbMode = savedInstanceState.getBoolean(KEY_IN_KB_MODE);
            mThemeDark = savedInstanceState.getBoolean(KEY_DARK_THEME);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
//        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

//        View view = inflater.inflate(R.layout.time_picker_dialog, null);
//        KeyboardListener keyboardListener = new KeyboardListener();
//        view.findViewById(R.id.time_picker_dialog).setOnKeyListener(keyboardListener);

        View view = super.onCreateView(inflater, container, savedInstanceState);

        // Inflate the buttons into the grid
        int layout = mIs24HourMode ? R.layout.content_24h_number_grid : R.layout.content_number_grid;
        View.inflate(getActivity(), layout, mGridLayout);
        setNumberTexts();
        setClickListenersOnButtons();
        if (mCurrentIndex == MINUTE_INDEX) {
            createMinuteTuners();
        }

        Resources res = getResources();
        mHourPickerDescription = res.getString(R.string.hour_picker_description);
        mSelectHours = res.getString(R.string.select_hours);
        mMinutePickerDescription = res.getString(R.string.minute_picker_description);
        mSelectMinutes = res.getString(R.string.select_minutes);
        mSelectedColor = res.getColor(mThemeDark? R.color.red : R.color.blue);
        mUnselectedColor =
                res.getColor(mThemeDark? android.R.color.white : R.color.numbers_text_color);

        mHourView = (TextView) view.findViewById(R.id.hours);
//        mHourView.setOnKeyListener(keyboardListener);
        mHourSpaceView = (TextView) view.findViewById(R.id.hour_space);
        mMinuteSpaceView = (TextView) view.findViewById(R.id.minutes_space);
        mMinuteView = (TextView) view.findViewById(R.id.minutes);
//        mMinuteView.setOnKeyListener(keyboardListener);
        mAmPmTextView = (TextView) view.findViewById(R.id.ampm_label);
//        mAmPmTextView.setOnKeyListener(keyboardListener);
        String[] amPmTexts = new DateFormatSymbols().getAmPmStrings();
        mAmText = amPmTexts[0];
        mPmText = amPmTexts[1];

        TextView tv1 = (TextView) mLeftHalfDayToggle.getChildAt(0);
        TextView tv2 = (TextView) mRightHalfDayToggle.getChildAt(0);
        if (mIs24HourMode) {
            tv1.setText("00 - 11");
            // Intrinsic bounds meaning the drawable's own bounds? So 24dp box.
            tv1.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_half_day_1_black_24dp, 0, 0, 0);
            tv1.setCompoundDrawablePadding((int) dpToPx(getActivity(), HALF_DAY_ICON_PADDING));
            tv2.setText("12 - 23");
            tv2.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_half_day_2_black_24dp, 0, 0, 0);
            tv2.setCompoundDrawablePadding((int) dpToPx(getActivity(), HALF_DAY_ICON_PADDING));
        } else {
            tv1.setText(mAmText);
            tv2.setText(mPmText);
        }

//        mHapticFeedbackController = new HapticFeedbackController(getActivity());

//        mTimePicker = (RadialPickerLayout) view.findViewById(R.id.time_picker);
//        mTimePicker.setOnValueSelectedListener(this);
//        mTimePicker.setOnKeyListener(keyboardListener);
//        mTimePicker.initialize(getActivity(), mHapticFeedbackController, mInitialHourOfDay,
//            mInitialMinute, mIs24HourMode);

        int currentItemShowing = HOUR_INDEX;
        if (savedInstanceState != null &&
                savedInstanceState.containsKey(KEY_CURRENT_ITEM_SHOWING)) {
            currentItemShowing = savedInstanceState.getInt(KEY_CURRENT_ITEM_SHOWING);
        }
        setCurrentItemShowing(currentItemShowing, false, true, true);
//        mTimePicker.invalidate();

        mHourView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setCurrentItemShowing(HOUR_INDEX, true, false, true);
                tryVibrate();
            }
        });
        mMinuteView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setCurrentItemShowing(MINUTE_INDEX, true, false, true);
                tryVibrate();
            }
        });

//        mDoneButton = (TextView) view.findViewById(R.id.done_button);
        mDoneButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mInKbMode && isTypedTimeFullyLegal()) { //  TODO: Delete
//                    finishKbMode(false);
                } else {
                    tryVibrate();
                }
                Log.i(TAG, String.format("Selected time is %02d:%02d", mSelectedHourOfDay, mSelectedMinute));
                if (mCallback != null) {
//                    mCallback.onTimeSet(mTimePicker, mTimePicker.getHours(), mTimePicker.getMinutes());
                    // I don't think the listener actually uses the first param passed back,
                    // so passing null is fine.
                    mCallback.onTimeSet(null, mSelectedHourOfDay, mSelectedMinute);
                }
                dismiss();
            }
        });
//        mDoneButton.setOnKeyListener(keyboardListener);

        // Enable or disable the AM/PM view.
        mAmPmHitspace = view.findViewById(R.id.ampm_hitspace);
        if (mIs24HourMode) {
            mAmPmTextView.setVisibility(View.GONE);

            RelativeLayout.LayoutParams paramsSeparator = new RelativeLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            paramsSeparator.addRule(RelativeLayout.CENTER_IN_PARENT);
            TextView separatorView = (TextView) view.findViewById(R.id.separator);
            separatorView.setLayoutParams(paramsSeparator);
        } else {
            mAmPmTextView.setVisibility(View.VISIBLE);
            updateAmPmDisplay(mInitialHourOfDay < 12? AM : PM);
            mAmPmHitspace.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    tryVibrate();
//                    int amOrPm = mTimePicker.getIsCurrentlyAmOrPm();
//                    if (amOrPm == AM) {
//                        amOrPm = PM;
//                    } else if (amOrPm == PM){
//                        amOrPm = AM;
//                    }
//                    updateAmPmDisplay(amOrPm);
//                    mTimePicker.setAmOrPm(amOrPm);
                    toggleHalfDay();
                }
            });
        }

        mAllowAutoAdvance = true;
        setHour(mInitialHourOfDay, true);
        setMinute(mInitialMinute);

        // Set up for keyboard mode.
        mDoublePlaceholderText = res.getString(R.string.time_placeholder);
        mDeletedKeyFormat = res.getString(R.string.deleted_key);
        mPlaceholderText = mDoublePlaceholderText.charAt(0);
        mAmKeyCode = mPmKeyCode = -1;
        generateLegalTimesTree();
        if (mInKbMode) {
            mTypedTimes = savedInstanceState.getIntegerArrayList(KEY_TYPED_TIMES);
//            tryStartingKbMode(-1);
            mHourView.invalidate();
        } else if (mTypedTimes == null) {
            mTypedTimes = new ArrayList<Integer>();
        }

        // Set the theme at the end so that the initialize()s above don't counteract the theme.
//        mTimePicker.setTheme(getActivity().getApplicationContext(), mThemeDark);
        // Prepare some colors to use.
        int white = res.getColor(android.R.color.white);
//        int circleBackground = res.getColor(R.color.circle_background);
//        int line = res.getColor(R.color.line_background);
        int timeDisplay = res.getColor(R.color.numbers_text_color);
//        ColorStateList doneTextColor = res.getColorStateList(R.color.done_text_color);
//        int doneBackground = R.drawable.done_background_color;

        int darkGray = res.getColor(R.color.dark_gray);
//        int lightGray = res.getColor(R.color.light_gray);
//        int darkLine = res.getColor(R.color.line_dark);
//        ColorStateList darkDoneTextColor = res.getColorStateList(R.color.done_text_color_dark);
//        int darkDoneBackground = R.drawable.done_background_color_dark;

        // Set the colors for each view based on the theme.
        view.findViewById(R.id.time_display_background).setBackgroundColor(mThemeDark? darkGray : white);
        view.findViewById(R.id.time_display).setBackgroundColor(mThemeDark? darkGray : white);
        ((TextView) view.findViewById(R.id.separator)).setTextColor(mThemeDark? white : timeDisplay);
        ((TextView) view.findViewById(R.id.ampm_label)).setTextColor(mThemeDark? white : timeDisplay);
//        view.findViewById(R.id.line).setBackgroundColor(mThemeDark? darkLine : line);
//        mDoneButton.setTextColor(mThemeDark? darkDoneTextColor : doneTextColor);
//        mTimePicker.setBackgroundColor(mThemeDark? lightGray : circleBackground);
//        mDoneButton.setBackgroundResource(mThemeDark? darkDoneBackground : doneBackground);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
//        mHapticFeedbackController.start();
    }

    @Override
    public void onPause() {
        super.onPause();
//        mHapticFeedbackController.stop();
    }

    public void tryVibrate() {
//        mHapticFeedbackController.tryVibrate();
    }

    private void updateAmPmDisplay(int amOrPm) {
        if (amOrPm == AM) {
            mAmPmTextView.setText(mAmText);
//            Utils.tryAccessibilityAnnounce(mTimePicker, mAmText);
            mAmPmHitspace.setContentDescription(mAmText);
        } else if (amOrPm == PM){
            mAmPmTextView.setText(mPmText);
//            Utils.tryAccessibilityAnnounce(mTimePicker, mPmText);
            mAmPmHitspace.setContentDescription(mPmText);
        } else {
            mAmPmTextView.setText(mDoublePlaceholderText);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
//        if (mTimePicker != null) {
//            outState.putInt(KEY_HOUR_OF_DAY, mTimePicker.getHours());
//            outState.putInt(KEY_MINUTE, mTimePicker.getMinutes());
//            outState.putBoolean(KEY_IS_24_HOUR_VIEW, mIs24HourMode);
//            outState.putInt(KEY_CURRENT_ITEM_SHOWING, mTimePicker.getCurrentItemShowing());
//            outState.putBoolean(KEY_IN_KB_MODE, mInKbMode);
//            if (mInKbMode) {
//                outState.putIntegerArrayList(KEY_TYPED_TIMES, mTypedTimes);
//            }
//            outState.putBoolean(KEY_DARK_THEME, mThemeDark);
//        }
        outState.putInt(KEY_HOUR_OF_DAY, mSelectedHourOfDay);
        outState.putInt(KEY_MINUTE, mSelectedMinute);
        outState.putBoolean(KEY_IS_24_HOUR_VIEW, mIs24HourMode);
        outState.putInt(KEY_CURRENT_ITEM_SHOWING, mCurrentIndex);
        outState.putBoolean(KEY_DARK_THEME, mThemeDark);
    }

//    /**
//     * Called by the picker for updating the header display.
//     */
//    @Override
    public void onValueSelected(int pickerIndex, int newValue, boolean autoAdvance) {
        if (pickerIndex == HOUR_INDEX) {
            setHour(newValue, false);
            String announcement = String.format("%d", newValue);
            if (mAllowAutoAdvance && autoAdvance) {
                setCurrentItemShowing(MINUTE_INDEX, true, true, false);
                announcement += ". " + mSelectMinutes;
            } else {
//                mTimePicker.setContentDescription(mHourPickerDescription + ": " + newValue);
            }

//            Utils.tryAccessibilityAnnounce(mTimePicker, announcement);
        } else if (pickerIndex == MINUTE_INDEX){
            setMinute(newValue);
//            mTimePicker.setContentDescription(mMinutePickerDescription + ": " + newValue);
        } else if (pickerIndex == AMPM_INDEX) {
            updateAmPmDisplay(newValue);
        } else if (pickerIndex == ENABLE_PICKER_INDEX) {
            if (!isTypedTimeFullyLegal()) {
                mTypedTimes.clear();
            }
//            finishKbMode(true);
        }
    }

    private void setHour(int value, boolean announce) {
        // TOneverDO: Set after if-else block (modulo operation changes value!)
        mSelectedHourOfDay = value;

        String format;
        if (mIs24HourMode) {
            format = "%02d";
        } else {
            format = "%d";
            value = value % 12;
            if (value == 0) {
                value = 12;
            }
        }

        CharSequence text = String.format(format, value);
        mHourView.setText(text);
        mHourSpaceView.setText(text);
        if (announce) {
//            Utils.tryAccessibilityAnnounce(mTimePicker, text);
        }
    }

    private void setMinute(int value) {
        if (value == 60) {
            value = 0;
        }
        CharSequence text = String.format(Locale.getDefault(), "%02d", value);
//        Utils.tryAccessibilityAnnounce(mTimePicker, text);
        mMinuteView.setText(text);
        mMinuteSpaceView.setText(text);

        // Setting this here is fine.
        mSelectedMinute = value;
    }

    // Show either Hours or Minutes.
    private void setCurrentItemShowing(int index, boolean animateCircle, boolean delayLabelAnimate,
            boolean announce) {
//        mTimePicker.setCurrentItemShowing(index, animateCircle);
        setCurrentItemShowing(index);

        TextView labelToAnimate;
        if (index == HOUR_INDEX) {
//            int hours = mTimePicker.getHours();
//            if (!mIs24HourMode) {
//                hours = hours % 12;
//            }
//            mTimePicker.setContentDescription(mHourPickerDescription + ": " + hours);
//            if (announce) {
//                Utils.tryAccessibilityAnnounce(mTimePicker, mSelectHours);
//            }
            labelToAnimate = mHourView;
        } else {
//            int minutes = mTimePicker.getMinutes();
//            mTimePicker.setContentDescription(mMinutePickerDescription + ": " + minutes);
//            if (announce) {
//                Utils.tryAccessibilityAnnounce(mTimePicker, mSelectMinutes);
//            }
            labelToAnimate = mMinuteView;
        }

        int hourColor = (index == HOUR_INDEX)? mSelectedColor : mUnselectedColor;
        int minuteColor = (index == MINUTE_INDEX)? mSelectedColor : mUnselectedColor;
        mHourView.setTextColor(hourColor);
        mMinuteView.setTextColor(minuteColor);

        ObjectAnimator pulseAnimator = Utils.getPulseAnimator(labelToAnimate, 0.85f, 1.1f);
        if (delayLabelAnimate) {
            pulseAnimator.setStartDelay(PULSE_ANIMATOR_DELAY);
        }
        pulseAnimator.start();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // END RELEVANT CODE
    ////////////////////////////////////////////////////////////////////////////////////////////////

//    /**
//     * For keyboard mode, processes key events.
//     * @param keyCode the pressed key.
//     * @return true if the key was successfully processed, false otherwise.
//     */
//    private boolean processKeyUp(int keyCode) {
//        if (keyCode == KeyEvent.KEYCODE_ESCAPE || keyCode == KeyEvent.KEYCODE_BACK) {
//            dismiss();
//            return true;
//        } else if (keyCode == KeyEvent.KEYCODE_TAB) {
//            if(mInKbMode) {
//                if (isTypedTimeFullyLegal()) {
//                    finishKbMode(true);
//                }
//                return true;
//            }
//        } else if (keyCode == KeyEvent.KEYCODE_ENTER) {
//            if (mInKbMode) {
//                if (!isTypedTimeFullyLegal()) {
//                    return true;
//                }
//                finishKbMode(false);
//            }
//            if (mCallback != null) {
//                mCallback.onTimeSet(mTimePicker,
//                        mTimePicker.getHours(), mTimePicker.getMinutes());
//            }
//            dismiss();
//            return true;
//        } else if (keyCode == KeyEvent.KEYCODE_DEL) {
//            if (mInKbMode) {
//                if (!mTypedTimes.isEmpty()) {
//                    int deleted = deleteLastTypedKey();
//                    String deletedKeyStr;
//                    if (deleted == getAmOrPmKeyCode(AM)) {
//                        deletedKeyStr = mAmText;
//                    } else if (deleted == getAmOrPmKeyCode(PM)) {
//                        deletedKeyStr = mPmText;
//                    } else {
//                        deletedKeyStr = String.format("%d", getValFromKeyCode(deleted));
//                    }
//                    Utils.tryAccessibilityAnnounce(mTimePicker,
//                            String.format(mDeletedKeyFormat, deletedKeyStr));
//                    updateDisplay(true);
//                }
//            }
//        } else if (keyCode == KeyEvent.KEYCODE_0 || keyCode == KeyEvent.KEYCODE_1
//                || keyCode == KeyEvent.KEYCODE_2 || keyCode == KeyEvent.KEYCODE_3
//                || keyCode == KeyEvent.KEYCODE_4 || keyCode == KeyEvent.KEYCODE_5
//                || keyCode == KeyEvent.KEYCODE_6 || keyCode == KeyEvent.KEYCODE_7
//                || keyCode == KeyEvent.KEYCODE_8 || keyCode == KeyEvent.KEYCODE_9
//                || (!mIs24HourMode &&
//                        (keyCode == getAmOrPmKeyCode(AM) || keyCode == getAmOrPmKeyCode(PM)))) {
//            if (!mInKbMode) {
//                if (mTimePicker == null) {
//                    // Something's wrong, because time picker should definitely not be null.
//                    Log.e(TAG, "Unable to initiate keyboard mode, TimePicker was null.");
//                    return true;
//                }
//                mTypedTimes.clear();
//                tryStartingKbMode(keyCode);
//                return true;
//            }
//            // We're already in keyboard mode.
//            if (addKeyIfLegal(keyCode)) {
//                updateDisplay(false);
//            }
//            return true;
//        }
//        return false;
//    }

//    /**
//     * Try to start keyboard mode with the specified key, as long as the timepicker is not in the
//     * middle of a touch-event.
//     * @param keyCode The key to use as the first press. Keyboard mode will not be started if the
//     * key is not legal to start with. Or, pass in -1 to get into keyboard mode without a starting
//     * key.
//     */
//    private void tryStartingKbMode(int keyCode) {
//        if (mTimePicker.trySettingInputEnabled(false) &&
//                (keyCode == -1 || addKeyIfLegal(keyCode))) {
//            mInKbMode = true;
//            mDoneButton.setEnabled(false);
//            updateDisplay(false);
//        }
//    }
//
//    private boolean addKeyIfLegal(int keyCode) {
//        // If we're in 24hour mode, we'll need to check if the input is full. If in AM/PM mode,
//        // we'll need to see if AM/PM have been typed.
//        if ((mIs24HourMode && mTypedTimes.size() == 4) ||
//                (!mIs24HourMode && isTypedTimeFullyLegal())) {
//            return false;
//        }
//
//        mTypedTimes.add(keyCode);
//        if (!isTypedTimeLegalSoFar()) {
//            deleteLastTypedKey();
//            return false;
//        }
//
//        int val = getValFromKeyCode(keyCode);
//        Utils.tryAccessibilityAnnounce(mTimePicker, String.format("%d", val));
//        // Automatically fill in 0's if AM or PM was legally entered.
//        if (isTypedTimeFullyLegal()) {
//            if (!mIs24HourMode && mTypedTimes.size() <= 3) {
//                mTypedTimes.add(mTypedTimes.size() - 1, KeyEvent.KEYCODE_0);
//                mTypedTimes.add(mTypedTimes.size() - 1, KeyEvent.KEYCODE_0);
//            }
//            mDoneButton.setEnabled(true);
//        }
//
//        return true;
//    }
//
//    /**
//     * Traverse the tree to see if the keys that have been typed so far are legal as is,
//     * or may become legal as more keys are typed (excluding backspace).
//     */
//    private boolean isTypedTimeLegalSoFar() {
//        Node node = mLegalTimesTree;
//        for (int keyCode : mTypedTimes) {
//            node = node.canReach(keyCode);
//            if (node == null) {
//                return false;
//            }
//        }
//        return true;
//    }
//
    /**
     * Check if the time that has been typed so far is completely legal, as is.
     */
    private boolean isTypedTimeFullyLegal() {
        if (mIs24HourMode) {
            // For 24-hour mode, the time is legal if the hours and minutes are each legal. Note:
            // getEnteredTime() will ONLY call isTypedTimeFullyLegal() when NOT in 24hour mode.
            int[] values = getEnteredTime(null);
            return (values[0] >= 0 && values[1] >= 0 && values[1] < 60);
        } else {
            // For AM/PM mode, the time is legal if it contains an AM or PM, as those can only be
            // legally added at specific times based on the tree's algorithm.
            return (mTypedTimes.contains(getAmOrPmKeyCode(AM)) ||
                    mTypedTimes.contains(getAmOrPmKeyCode(PM)));
        }
    }
//
//    private int deleteLastTypedKey() {
//        int deleted = mTypedTimes.remove(mTypedTimes.size() - 1);
//        if (!isTypedTimeFullyLegal()) {
//            mDoneButton.setEnabled(false);
//        }
//        return deleted;
//    }
//
//    /**
//     * Get out of keyboard mode. If there is nothing in typedTimes, revert to TimePicker's time.
//     * @param changeDisplays If true, update the displays with the relevant time.
//     */
//    private void finishKbMode(boolean updateDisplays) {
//        mInKbMode = false;
//        if (!mTypedTimes.isEmpty()) {
//            int values[] = getEnteredTime(null);
//            mTimePicker.setTime(values[0], values[1]);
//            if (!mIs24HourMode) {
//                mTimePicker.setAmOrPm(values[2]);
//            }
//            mTypedTimes.clear();
//        }
//        if (updateDisplays) {
//            updateDisplay(false);
//            mTimePicker.trySettingInputEnabled(true);
//        }
//    }
//
//    /**
//     * Update the hours, minutes, and AM/PM displays with the typed times. If the typedTimes is
//     * empty, either show an empty display (filled with the placeholder text), or update from the
//     * timepicker's values.
//     * @param allowEmptyDisplay if true, then if the typedTimes is empty, use the placeholder text.
//     * Otherwise, revert to the timepicker's values.
//     */
//    private void updateDisplay(boolean allowEmptyDisplay) {
//        if (!allowEmptyDisplay && mTypedTimes.isEmpty()) {
//            int hour = mTimePicker.getHours();
//            int minute = mTimePicker.getMinutes();
//            setHour(hour, true);
//            setMinute(minute);
//            if (!mIs24HourMode) {
//                updateAmPmDisplay(hour < 12? AM : PM);
//            }
//            setCurrentItemShowing(mTimePicker.getCurrentItemShowing(), true, true, true);
//            mDoneButton.setEnabled(true);
//        } else {
//            Boolean[] enteredZeros = {false, false};
//            int[] values = getEnteredTime(enteredZeros);
//            String hourFormat = enteredZeros[0]? "%02d" : "%2d";
//            String minuteFormat = (enteredZeros[1])? "%02d" : "%2d";
//            String hourStr = (values[0] == -1)? mDoublePlaceholderText :
//                String.format(hourFormat, values[0]).replace(' ', mPlaceholderText);
//            String minuteStr = (values[1] == -1)? mDoublePlaceholderText :
//                String.format(minuteFormat, values[1]).replace(' ', mPlaceholderText);
//            mHourView.setText(hourStr);
//            mHourSpaceView.setText(hourStr);
//            mHourView.setTextColor(mUnselectedColor);
//            mMinuteView.setText(minuteStr);
//            mMinuteSpaceView.setText(minuteStr);
//            mMinuteView.setTextColor(mUnselectedColor);
//            if (!mIs24HourMode) {
//                updateAmPmDisplay(values[2]);
//            }
//        }
//    }

    private static int getValFromKeyCode(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_0:
                return 0;
            case KeyEvent.KEYCODE_1:
                return 1;
            case KeyEvent.KEYCODE_2:
                return 2;
            case KeyEvent.KEYCODE_3:
                return 3;
            case KeyEvent.KEYCODE_4:
                return 4;
            case KeyEvent.KEYCODE_5:
                return 5;
            case KeyEvent.KEYCODE_6:
                return 6;
            case KeyEvent.KEYCODE_7:
                return 7;
            case KeyEvent.KEYCODE_8:
                return 8;
            case KeyEvent.KEYCODE_9:
                return 9;
            default:
                return -1;
        }
    }

    /**
     * Get the currently-entered time, as integer values of the hours and minutes typed.
     * @param enteredZeros A size-2 boolean array, which the caller should initialize, and which
     * may then be used for the caller to know whether zeros had been explicitly entered as either
     * hours of minutes. This is helpful for deciding whether to show the dashes, or actual 0's.
     * @return A size-3 int array. The first value will be the hours, the second value will be the
     * minutes, and the third will be either TimePickerDialog.AM or TimePickerDialog.PM.
     */
    private int[] getEnteredTime(Boolean[] enteredZeros) {
        int amOrPm = -1;
        int startIndex = 1;
        if (!mIs24HourMode && isTypedTimeFullyLegal()) {
            int keyCode = mTypedTimes.get(mTypedTimes.size() - 1);
            if (keyCode == getAmOrPmKeyCode(AM)) {
                amOrPm = AM;
            } else if (keyCode == getAmOrPmKeyCode(PM)){
                amOrPm = PM;
            }
            startIndex = 2;
        }
        int minute = -1;
        int hour = -1;
        for (int i = startIndex; i <= mTypedTimes.size(); i++) {
            int val = getValFromKeyCode(mTypedTimes.get(mTypedTimes.size() - i));
            if (i == startIndex) {
                minute = val;
            } else if (i == startIndex+1) {
                minute += 10*val;
                if (enteredZeros != null && val == 0) {
                    enteredZeros[1] = true;
                }
            } else if (i == startIndex+2) {
                hour = val;
            } else if (i == startIndex+3) {
                hour += 10*val;
                if (enteredZeros != null && val == 0) {
                    enteredZeros[0] = true;
                }
            }
        }

        int[] ret = {hour, minute, amOrPm};
        return ret;
    }

    /**
     * Get the keycode value for AM and PM in the current language.
     */
    private int getAmOrPmKeyCode(int amOrPm) {
        // Cache the codes.
        if (mAmKeyCode == -1 || mPmKeyCode == -1) {
            // Find the first character in the AM/PM text that is unique.
            KeyCharacterMap kcm = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);
            char amChar;
            char pmChar;
            for (int i = 0; i < Math.max(mAmText.length(), mPmText.length()); i++) {
                amChar = mAmText.toLowerCase(Locale.getDefault()).charAt(i);
                pmChar = mPmText.toLowerCase(Locale.getDefault()).charAt(i);
                if (amChar != pmChar) {
                    KeyEvent[] events = kcm.getEvents(new char[]{amChar, pmChar});
                    // There should be 4 events: a down and up for both AM and PM.
                    if (events != null && events.length == 4) {
                        mAmKeyCode = events[0].getKeyCode();
                        mPmKeyCode = events[2].getKeyCode();
                    } else {
                        Log.e(TAG, "Unable to find keycodes for AM and PM.");
                    }
                    break;
                }
            }
        }
        if (amOrPm == AM) {
            return mAmKeyCode;
        } else if (amOrPm == PM) {
            return mPmKeyCode;
        }

        return -1;
    }

    /**
     * Create a tree for deciding what keys can legally be typed.
     */
    private void generateLegalTimesTree() {
        // Create a quick cache of numbers to their keycodes.
        int k0 = KeyEvent.KEYCODE_0;
        int k1 = KeyEvent.KEYCODE_1;
        int k2 = KeyEvent.KEYCODE_2;
        int k3 = KeyEvent.KEYCODE_3;
        int k4 = KeyEvent.KEYCODE_4;
        int k5 = KeyEvent.KEYCODE_5;
        int k6 = KeyEvent.KEYCODE_6;
        int k7 = KeyEvent.KEYCODE_7;
        int k8 = KeyEvent.KEYCODE_8;
        int k9 = KeyEvent.KEYCODE_9;

        // The root of the tree doesn't contain any numbers.
        mLegalTimesTree = new Node();
        if (mIs24HourMode) {
            // We'll be re-using these nodes, so we'll save them.
            Node minuteFirstDigit = new Node(k0, k1, k2, k3, k4, k5);
            Node minuteSecondDigit = new Node(k0, k1, k2, k3, k4, k5, k6, k7, k8, k9);
            // The first digit must be followed by the second digit.
            minuteFirstDigit.addChild(minuteSecondDigit);

            // The first digit may be 0-1.
            Node firstDigit = new Node(k0, k1);
            mLegalTimesTree.addChild(firstDigit);

            // When the first digit is 0-1, the second digit may be 0-5.
            Node secondDigit = new Node(k0, k1, k2, k3, k4, k5);
            firstDigit.addChild(secondDigit);
            // We may now be followed by the first minute digit. E.g. 00:09, 15:58.
            secondDigit.addChild(minuteFirstDigit);

            // When the first digit is 0-1, and the second digit is 0-5, the third digit may be 6-9.
            Node thirdDigit = new Node(k6, k7, k8, k9);
            // The time must now be finished. E.g. 0:55, 1:08.
            secondDigit.addChild(thirdDigit);

            // When the first digit is 0-1, the second digit may be 6-9.
            secondDigit = new Node(k6, k7, k8, k9);
            firstDigit.addChild(secondDigit);
            // We must now be followed by the first minute digit. E.g. 06:50, 18:20.
            secondDigit.addChild(minuteFirstDigit);

            // The first digit may be 2.
            firstDigit = new Node(k2);
            mLegalTimesTree.addChild(firstDigit);

            // When the first digit is 2, the second digit may be 0-3.
            secondDigit = new Node(k0, k1, k2, k3);
            firstDigit.addChild(secondDigit);
            // We must now be followed by the first minute digit. E.g. 20:50, 23:09.
            secondDigit.addChild(minuteFirstDigit);

            // When the first digit is 2, the second digit may be 4-5.
            secondDigit = new Node(k4, k5);
            firstDigit.addChild(secondDigit);
            // We must now be followd by the last minute digit. E.g. 2:40, 2:53.
            secondDigit.addChild(minuteSecondDigit);

            // The first digit may be 3-9.
            firstDigit = new Node(k3, k4, k5, k6, k7, k8, k9);
            mLegalTimesTree.addChild(firstDigit);
            // We must now be followed by the first minute digit. E.g. 3:57, 8:12.
            firstDigit.addChild(minuteFirstDigit);
        } else {
            // We'll need to use the AM/PM node a lot.
            // Set up AM and PM to respond to "a" and "p".
            Node ampm = new Node(getAmOrPmKeyCode(AM), getAmOrPmKeyCode(PM));

            // The first hour digit may be 1.
            Node firstDigit = new Node(k1);
            mLegalTimesTree.addChild(firstDigit);
            // We'll allow quick input of on-the-hour times. E.g. 1pm.
            firstDigit.addChild(ampm);

            // When the first digit is 1, the second digit may be 0-2.
            Node secondDigit = new Node(k0, k1, k2);
            firstDigit.addChild(secondDigit);
            // Also for quick input of on-the-hour times. E.g. 10pm, 12am.
            secondDigit.addChild(ampm);

            // When the first digit is 1, and the second digit is 0-2, the third digit may be 0-5.
            Node thirdDigit = new Node(k0, k1, k2, k3, k4, k5);
            secondDigit.addChild(thirdDigit);
            // The time may be finished now. E.g. 1:02pm, 1:25am.
            thirdDigit.addChild(ampm);

            // When the first digit is 1, the second digit is 0-2, and the third digit is 0-5,
            // the fourth digit may be 0-9.
            Node fourthDigit = new Node(k0, k1, k2, k3, k4, k5, k6, k7, k8, k9);
            thirdDigit.addChild(fourthDigit);
            // The time must be finished now. E.g. 10:49am, 12:40pm.
            fourthDigit.addChild(ampm);

            // When the first digit is 1, and the second digit is 0-2, the third digit may be 6-9.
            thirdDigit = new Node(k6, k7, k8, k9);
            secondDigit.addChild(thirdDigit);
            // The time must be finished now. E.g. 1:08am, 1:26pm.
            thirdDigit.addChild(ampm);

            // When the first digit is 1, the second digit may be 3-5.
            secondDigit = new Node(k3, k4, k5);
            firstDigit.addChild(secondDigit);

            // When the first digit is 1, and the second digit is 3-5, the third digit may be 0-9.
            thirdDigit = new Node(k0, k1, k2, k3, k4, k5, k6, k7, k8, k9);
            secondDigit.addChild(thirdDigit);
            // The time must be finished now. E.g. 1:39am, 1:50pm.
            thirdDigit.addChild(ampm);

            // The hour digit may be 2-9.
            firstDigit = new Node(k2, k3, k4, k5, k6, k7, k8, k9);
            mLegalTimesTree.addChild(firstDigit);
            // We'll allow quick input of on-the-hour-times. E.g. 2am, 5pm.
            firstDigit.addChild(ampm);

            // When the first digit is 2-9, the second digit may be 0-5.
            secondDigit = new Node(k0, k1, k2, k3, k4, k5);
            firstDigit.addChild(secondDigit);

            // When the first digit is 2-9, and the second digit is 0-5, the third digit may be 0-9.
            thirdDigit = new Node(k0, k1, k2, k3, k4, k5, k6, k7, k8, k9);
            secondDigit.addChild(thirdDigit);
            // The time must be finished now. E.g. 2:57am, 9:30pm.
            thirdDigit.addChild(ampm);
        }
    }

    /**
     * Simple node class to be used for traversal to check for legal times.
     * mLegalKeys represents the keys that can be typed to get to the node.
     * mChildren are the children that can be reached from this node.
     */
    private class Node {
        private int[] mLegalKeys;
        private ArrayList<Node> mChildren;

        public Node(int... legalKeys) {
            mLegalKeys = legalKeys;
            mChildren = new ArrayList<Node>();
        }

        public void addChild(Node child) {
            mChildren.add(child);
        }

        public boolean containsKey(int key) {
            for (int i = 0; i < mLegalKeys.length; i++) {
                if (mLegalKeys[i] == key) {
                    return true;
                }
            }
            return false;
        }

        public Node canReach(int key) {
            if (mChildren == null) {
                return null;
            }
            for (Node child : mChildren) {
                if (child.containsKey(key)) {
                    return child;
                }
            }
            return null;
        }
    }

//    private class KeyboardListener implements OnKeyListener {
//        @Override
//        public boolean onKey(View v, int keyCode, KeyEvent event) {
//            if (event.getAction() == KeyEvent.ACTION_UP) {
//                return processKeyUp(keyCode);
//            }
//            return false;
//        }
//    }
}
