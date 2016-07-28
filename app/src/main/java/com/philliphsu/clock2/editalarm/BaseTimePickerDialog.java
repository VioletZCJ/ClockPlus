package com.philliphsu.clock2.editalarm;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import butterknife.ButterKnife;

/**
 * Created by Phillip Hsu on 7/16/2016.
 */
public abstract class BaseTimePickerDialog extends BottomSheetDialogFragment {

    // TODO: Consider private access, and then writing package/protected API that subclasses
    // can use to interface with this field.
    /*package*/ OnTimeSetListener mCallback;

    /**
     * The callback interface used to indicate the user is done filling in
     * the time (they clicked on the 'Set' button).
     */
    interface OnTimeSetListener {
        /**
         * @param viewGroup The view associated with this listener.
         * @param hourOfDay The hour that was set.
         * @param minute The minute that was set.
         */
        // TODO: Consider removing VG param, since listeners probably won't need to use it....
        void onTimeSet(ViewGroup viewGroup, int hourOfDay, int minute);
    }

    /**
     * Empty constructor required for dialog fragment.
     * Subclasses do not need to write their own.
     */
    public BaseTimePickerDialog() {}

    @LayoutRes
    protected abstract int contentLayout();

    public final void setOnTimeSetListener(OnTimeSetListener callback) {
        mCallback = callback;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Not needed for bottom sheet dialogs
//        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        final View view = inflater.inflate(contentLayout(), container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    // Code for AlertDialog style only.
//    @NonNull
//    @Override
//    public Dialog onCreateDialog(Bundle savedInstanceState) {
//        // Use an AlertDialog to display footer buttons, rather than
//        // re-invent them in our layout.
//        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//        builder.setView(contentLayout())
//                // The action strings are already defined and localized by the system!
//                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//
//                    }
//                })
//                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//
//                    }
//                });
//        return builder.create();
//    }

    // This was an unsatisfactory solution to forcing the bottom sheet to show at its
    // fully expanded state. Our anchored FAB and GridLayout buttons would not be visible.
//    @Override
//    public Dialog onCreateDialog(Bundle savedInstanceState) {
//        Dialog dialog = super.onCreateDialog(savedInstanceState);
//        //dialog = new BottomSheetDialog(getActivity(), R.style.AppTheme_AppCompatDialog/*crashes our app!*/);
//        // We're past onCreate() in the lifecycle, so the activity is alive.
//        View view = LayoutInflater.from(getActivity()).inflate(contentLayout(), null);
//        /**
//         * Adds our view to a ViewGroup that has a BottomSheetBehavior attached. The ViewGroup
//         * itself is a child of a CoordinatorLayout.
//         * @see {@link BottomSheetDialog#wrapInBottomSheet(int, View, ViewGroup.LayoutParams)}
//         */
//        dialog.setContentView(view);
//        // Bind this fragment, not the internal dialog! (There is a bind(Dialog) API.)
//        ButterKnife.bind(this, view);
//        final BottomSheetBehavior behavior = BottomSheetBehavior.from((View) view.getParent());

//        // When we collapse, collapse all the way. Do not be misled by the "docs" in
//        // https://android-developers.blogspot.com.au/2016/02/android-support-library-232.html
//        // when it says:
//        // "STATE_COLLAPSED: ... the app:behavior_peekHeight attribute (defaults to 0)"
//        // While it is true by default, BottomSheetDialogs override this default height.

          // This means the sheet is considered "open" even at a height of 0! This is why
//        // when you swipe to hide the sheet, the screen remains darkened--indicative
//        // of an open dialog.
//        behavior.setPeekHeight(0);

//        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
//            @Override
//            public void onShow(DialogInterface dialog) {
//                // Every time we show, show at our full height.
//                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
//            }
//        });
//
//        return dialog;
//    }
}
