package in.insideandroid.eyetracker;

import android.content.Context;

import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;

public class FaceTrackerDaemon implements MultiProcessor.Factory<Face> {

    private EyeStateListener listener;
    private Context context; // Store the context

    public FaceTrackerDaemon(Context context, EyeStateListener listener) {
        this.listener = listener;
        this.context = context; // Store the context in the constructor
    }

    @Override
    public Tracker<Face> create(Face face) {
        return new EyesTracker(context, listener);
    }
}
