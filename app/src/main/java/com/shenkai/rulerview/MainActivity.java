package com.shenkai.rulerview;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import com.shenkai.rulerview.widget.TimeRulerView;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    private TimeRulerView timeRulerView;
    private TextView tvTime;
    private int selectedMinute;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        timeRulerView = findViewById(R.id.time_ruler_view);
        tvTime = findViewById(R.id.tv_time);
        timeRulerView.setOnValueChangeListener(minute -> {
            selectedMinute = minute;
            tvTime.setText(minuteToTime(minute));
        });
        initTime();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initTime() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR);
        int minute = calendar.get(Calendar.MINUTE);
        if (calendar.get(Calendar.AM_PM) == Calendar.AM) {
            tvTime.setText(minuteToTime(hour * 60 + minute));
            timeRulerView.setValue(selectedMinute = hour * 60 + minute, 0, 60 * 24 - 1, 1);
        } else {
            tvTime.setText(minuteToTime((hour + 12) * 60 + minute));
            timeRulerView.setValue(selectedMinute = (hour + 12) * 60 + minute, 0, 60 * 24 - 1, 1);
        }
    }

    private String minuteToHour(int minute) {
        int hour = minute / 60;
        if (hour == 0) {
            hour = 12;
        } else if (hour > 12) {
            hour = hour - 12;
        }
        return String.valueOf(hour);
    }

    private String minuteToMinute(int minute) {
        minute = minute % 60;
        return (minute < 10 ? "0" + minute : String.valueOf(minute));
    }

    private String minuteToTime(int minute) {
        int hour = minute / 60;
        minute = minute % 60;
        if (hour == 0) {
            hour = 12;
        } else if (hour > 12) {
            hour = hour - 12;
        }

        return hour + ":" + (minute < 10 ? "0" + minute : String.valueOf(minute));
    }
}
