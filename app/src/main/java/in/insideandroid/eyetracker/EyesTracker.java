package in.insideandroid.eyetracker;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;

import static androidx.constraintlayout.widget.Constraints.TAG;

public class EyesTracker extends Tracker<Face> {
    private final float THRESHOLD = 0.75f;
    private EyeStateListener listener;

    public EyesTracker(Context context, EyeStateListener listener) {
        this.listener = listener;
    }

    @Override
    public void onUpdate(Detector.Detections<Face> detections, Face face) {
        if (face.getIsLeftEyeOpenProbability() > THRESHOLD || face.getIsRightEyeOpenProbability() > THRESHOLD) {
            Log.i(TAG, "onUpdate: Eyes Open");
            if (listener != null) {
                listener.onEyeStateChanged(Condition.USER_EYES_OPEN);
            }
        }
        else {
                Log.i(TAG, "onUpdate: Eyes Closed");
                if (listener != null) {
                    listener.onEyeStateChanged(Condition.USER_EYES_CLOSED);
                }
            }
    }

    @Override
    public void onMissing(Detector.Detections<Face> detections) {
        super.onMissing(detections);

        Log.i(TAG, "onUpdate: Face Not Found!");
        if (listener != null) {
            listener.onEyeStateChanged(Condition.FACE_NOT_FOUND);
        }
    }

    @Override
    public void onDone() {
        super.onDone();
    }
}
