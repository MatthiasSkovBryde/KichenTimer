package com.example.kitchentimer;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    // Written by Matthias Skov Bryde
    private TextView txt_time;
    private EditText et_hours, et_min, et_sec;
    private Button btn_start, btn_stop, btn_resset;
    private long totalTimeInMillis, newCurrentTime;
    private Thread timerThread;
    public boolean isPaused, isRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Initialize UI elements
        init();
    }

    private void init() {
        // Assign UI elements to variables
        txt_time = findViewById(R.id.time);
        et_hours = findViewById(R.id.et_hours);
        et_min = findViewById(R.id.et_min);
        et_sec = findViewById(R.id.et_sec);
        btn_start = findViewById(R.id.btn_start);
        btn_stop = findViewById(R.id.btn_stop);
        btn_resset = findViewById(R.id.btn_reset);

        // Set click listeners for buttons
        btn_start.setOnClickListener(view -> startTimer());
        btn_stop.setOnClickListener(view -> stopTimer());
        btn_resset.setOnClickListener(view -> resetTimer());
    }

    private void startTimer() {
        int hours, minutes, seconds;
        isPaused = false;

        // If the timer is not already running, parse input and calculate total time
        if (!isRunning) {
            try {
                hours = Integer.parseInt(et_hours.getText().toString());
                minutes = Integer.parseInt(et_min.getText().toString());
                seconds = Integer.parseInt(et_sec.getText().toString());
            } catch (NumberFormatException e) {
                // Display a toast for invalid input
                Toast.makeText(this, "Invalid input. Please enter a numeric value.", Toast.LENGTH_LONG).show();
                return;
            }
            totalTimeInMillis = (hours * 60 * 60 + minutes * 60 + seconds) * 1000;
        }
        // Stop any existing timer thread
        stopTimerThread();

        // Start a new timer thread with the calculated total time
        timerThread = new Thread(new Multithreding(totalTimeInMillis, this));
        timerThread.start();

        // Update flags and clear input fields
        isRunning = true;
        et_hours.getText().clear();
        et_min.getText().clear();
        et_sec.getText().clear();
    }

    private void stopTimer() {
        // Pause the timer and update the current time
        isPaused = true;
        totalTimeInMillis = newCurrentTime;
    }

    private void resetTimer() {
        // Stop the timer and reset UI elements
        stopTimer();
        isRunning = false;
        txt_time.setText("00:00:00");
    }

    private void stopTimerThread() {
        // Interrupt and stop the existing timer thread if it's alive
        if (timerThread != null && timerThread.isAlive()) {
            timerThread.interrupt();
        }
    }

    public void updateTimerText(long millisUntilFinished) {
        // Format and update the displayed timer text
        String formattedTime = formatTime(millisUntilFinished);
        newCurrentTime = millisUntilFinished;
        txt_time.setText(formattedTime);
    }

    public void timerFinished() {
        // Display a toast when the timer finishes
        Toast.makeText(this, "DONE", Toast.LENGTH_LONG).show();
    }

    private String formatTime(long millis) {
        // Format the time in HH:mm:ss
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;

        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }
}

class Multithreding implements Runnable {
    private MainActivity mainActivity;
    private long currentTimeInMillis;

    public Multithreding(long totalTimeInMillis, MainActivity mainActivity) {
        this.currentTimeInMillis = totalTimeInMillis;
        this.mainActivity = mainActivity;
    }

    @Override
    public void run() {
        try {
            // Check if the total time is not zero
            if (currentTimeInMillis != 0) {
                // Run the timer until it's interrupted, time reaches zero, or it's paused
                while (!Thread.currentThread().isInterrupted() && currentTimeInMillis > 0 && !mainActivity.isPaused) {
                    Thread.sleep(1000);
                    currentTimeInMillis -= 1000;
                    // Update UI on the main thread
                    mainActivity.runOnUiThread(() -> mainActivity.updateTimerText(currentTimeInMillis));
                }
                // Run on the main thread when the timer finishes
                mainActivity.runOnUiThread(() -> mainActivity.timerFinished());
            }
            else {
                // Display an error toast if the total time is zero
                Toast.makeText(this.mainActivity, "ERROR", Toast.LENGTH_LONG).show();
            }

        } catch (InterruptedException e) {
            // Handle thread interruption and throw a runtime exception
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}