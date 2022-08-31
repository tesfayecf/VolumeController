package com.example.volumecontroller;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class SettingsSheet extends BottomSheetDialogFragment {

    // edit text parameters
    public EditText intervalVal;
    public EditText freqVal;
    public EditText rangeVal;
    public CheckBox checkBox;
    public EditText rangeTop;
    public EditText rangeBot;
    public Button saveButt;

    // settings values
    public int interval ;
    public double frequency;
    public double range;
    public boolean custom;
    public double topRange;
    public double botRange;

    public SettingsSheet(int _interval, double _frequency, double _range, boolean _custom, double _topRange, double _botRange ) {
        // get current settings values from constructor
        interval = _interval;
        frequency = _frequency;
        range = _range;
        custom = _custom;
        topRange = _topRange;
        botRange = _botRange;
    }

    @Nullable
    @Override
    public View onCreateView(@Nullable LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        assert inflater != null;
        View view = inflater.inflate(R.layout.settings_bottom_sheet,container,false);

        // get view elements
        intervalVal = view.findViewById(R.id.intervalValue);
        freqVal = view.findViewById(R.id.freqValue);
        rangeVal = view.findViewById(R.id.rangeValue);
        checkBox = view.findViewById(R.id.customRangeCheckBox);
        rangeTop = view.findViewById(R.id.topRangeValue);
        rangeBot = view.findViewById(R.id.bottomRangeValue);
        saveButt = view.findViewById(R.id.saveButton);

        // set current settings values to editText
        intervalVal.setText(String.valueOf(interval));
        freqVal.setText(String.valueOf(frequency));
        rangeVal.setText(String.valueOf(range));
        checkBox.setChecked(custom);
        rangeTop.setText(String.valueOf(topRange));
        rangeBot.setText(String.valueOf(botRange));

        // define onClickListener for save button
        saveButt.setOnClickListener(view1 -> {
            Intent intent = new Intent(getActivity(), MainActivity.class);

            // get new values
            interval = getInt(interval, intervalVal);
            frequency = getDouble(frequency, freqVal);
            range = getDouble(range, rangeVal);
            custom = checkBox.isChecked();
            topRange = getDouble(topRange, rangeTop);
            botRange = getDouble(botRange, rangeBot);

            // send new values to main activity
            intent.putExtra("interval", interval);
            intent.putExtra("frequency", frequency);
            intent.putExtra("range", range);
            intent.putExtra("custom", custom);
            intent.putExtra("topRange", topRange);
            intent.putExtra("botRange", botRange);
            startActivity(intent);
        });

        return view;
    }

    private int getInt(int prev, @NonNull EditText element) {
        String prov = String.valueOf(element.getText());
        if (prov.trim().length() != 0) { // check if empty
            if (Integer.parseInt(prov) > 0) { // check if 0
                return Integer.parseInt(prov); // return new one
            }
        }
        return prev; // return old one
    }

    private double getDouble(double prev, @NonNull EditText element) {
        String prov = String.valueOf(element.getText());
        if (prov.trim().length() != 0) { // check if empty
            if (Double.parseDouble(prov) > 0) { // check if 0
                return Double.parseDouble(prov); // return new one
            }
        }
        return prev; // return old one
    }

}
