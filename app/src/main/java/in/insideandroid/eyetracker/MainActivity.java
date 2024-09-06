package in.insideandroid.eyetracker;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import android.widget.ImageView;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements EyeStateListener {

    private ConstraintLayout background;
    private ImageView backgroundImage; // ImageView for background image
    private TextView user_message;
    private Button snoozeButton;
    private TimePicker snoozeTimePicker;

    private boolean flag = false;
    private CameraSource cameraSource;
    private MediaPlayer mp;
    private boolean isAlarmStopped = false; // Flag to track alarm state
    private boolean isSnoozing = false; // Flag for snooze state
    private CountDownTimer snoozeTimer; // Timer for snooze duration
    private long snoozeDuration;  // Variable to store user-selected snooze duration in milliseconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
            Toast.makeText(this, "Permission not granted!\n Grant permission and restart app", Toast.LENGTH_SHORT).show();
        } else {
            init();
        }
    }

    private void init() {
        background = findViewById(R.id.background);
        backgroundImage = findViewById(R.id.background_image); // Reference the ImageView from layout
        user_message = findViewById(R.id.user_text);
        flag = true;

        // Find the snooze button only once (remove duplicate declaration)
        snoozeButton = findViewById(R.id.set_snooze_button);

        // Initialize media player for sound
        mp = MediaPlayer.create(this, R.raw.alarm); // Replace with your sound resource ID

        // Initialize TimePicker (hidden initially)
        snoozeTimePicker = findViewById(R.id.snooze_time_picker);
        snoozeTimePicker.setVisibility(View.GONE);

        // Button to show TimePicker for setting snooze duration
        snoozeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show TimePicker to allow user selection
                snoozeTimePicker.setVisibility(View.VISIBLE);
            }
        });


        initCameraSource();

        // Set click listener for snooze button
        snoozeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mp != null && mp.isPlaying()) { // Check if alarm is playing
                    mp.pause(); // Pause the alarm (snooze functionality)
                    isAlarmStopped = true; // Set flag to prevent automatic restart
                    user_message.setText("Alarm Snoozed!");
                    isSnoozing = true; // Set snooze flag
                    // Disable snooze button to prevent multiple clicks
                    snoozeButton.setEnabled(false);

                    // Start snooze timer using user-selected duration
                    startSnoozeTimer();
                }
            }
        });

        // Set listener for TimePicker to capture user-selected snooze duration
        snoozeTimePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                // Convert to milliseconds and store in snoozeDuration
                snoozeDuration = (minute * 60 * 1000);  // Assuming only minutes for snooze
            }
        });
    }

    private void initCameraSource() {
        FaceDetector detector = new FaceDetector.Builder(this)
                .setTrackingEnabled(true)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setMode(FaceDetector.FAST_MODE)
                .build();
        detector.setProcessor(new MultiProcessor.Builder(new FaceTrackerDaemon(this, this)).build()); // Pass MainActivity as listener

        cameraSource = new CameraSource.Builder(this, detector)
                .setRequestedPreviewSize(1024, 768)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setRequestedFps(30.0f)
                .build();

        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            cameraSource.start();
            // Start alarm on app launch (assuming eyes are closed initially)
            if (mp != null && !isAlarmStopped) {
                if (mp != null && !isAlarmStopped) {
                    mp.start();
                }
            }
        } catch (IOException e) {
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                init();
            } else {
                Toast.makeText(this, "Camera Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (cameraSource != null) {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                cameraSource.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraSource != null) {
            cameraSource.stop();
        }

        setBackgroundGrey();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraSource != null) {
            cameraSource.release();
        }
        if (mp != null) {
            mp.release(); // Release MediaPlayer resources
        }
        if (snoozeTimer != null) {
            snoozeTimer.cancel(); // Cancel any ongoing snooze timer
        }
    }

    // Update view based on eye state
    @Override
    public void onEyeStateChanged(Condition condition) {
        switch (condition) {
            case USER_EYES_OPEN:
                if (!isSnoozing) { // Only stop sound and change color if not snoozing
                    setBackgroundOpenEyes();
                    user_message.setText("Open eyes detected");
                    if (mp != null && mp.isPlaying()) {
                        mp.stop();
                        isAlarmStopped = true; // Set flag to prevent automatic restart
                    }
                }
                break;
            case USER_EYES_CLOSED:
                //if (isSnoozing) { // Only play sound if not snoozing
                    setBackgroundClosedEyes();
                    user_message.setText("Close eyes detected");
                    /*if (mp != null && !isAlarmStopped) {
                        mp.start();
                    }*/

                break;
            case FACE_NOT_FOUND:
                if (isSnoozing) {
                    setBackgroundNoUserFound();
                    user_message.setText("User not found");
                    break;
                }
        }
    }

    private void setBackgroundImage(Drawable drawable) {
        backgroundImage.setImageDrawable(drawable);
    }

    private boolean setBackgroundOpenEyes() {
        setBackgroundImage(getDrawable(R.drawable.open_eyes));
        return false;
    }

    private void setBackgroundClosedEyes() {
        setBackgroundImage(getDrawable(R.drawable.closed_eyes));
    }

    private void setBackgroundNoUserFound() {
        setBackgroundImage(getDrawable(R.drawable.no_user_found));
    }


    // helper methods to set background color
    private void setBackgroundGrey() {
        if (background != null)
            background.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
    }

    /*private void setBackgroundGreen() {
        if (background != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    background.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
                }
            });
        }
    }

    private void setBackgroundOrange() {
        if (background != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    background.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_dark));
                }
            });
        }
    }

    private void setBackgroundRed() {
        if (background != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    background.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
                }
            });
        }
    }*/

    // Method to start a snooze timer

    private void startSnoozeTimer() {

            // Set your desired snooze duration in milliseconds (e.g., 1 minutes)
            long snoozeDuration = 1 * 60 * 1000;

            snoozeTimer = new CountDownTimer(snoozeDuration, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    user_message.setText("Alarm Snoozed (" + millisUntilFinished / 1000 + "s remaining)");
                }

                @Override
                public void onFinish() {
                    user_message.setText("Snooze Finished!");
                    isSnoozing = false; // Reset snooze flag
                    snoozeButton.setEnabled(true); // Re-enable snooze button
                    // Play alarm sound again or trigger other actions based on your app logic
                    if (mp != null) {
                        mp.start();
                    }
                }
            }.start();

    }
}
