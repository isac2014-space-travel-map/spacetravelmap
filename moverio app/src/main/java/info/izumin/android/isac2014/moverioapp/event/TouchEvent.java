package info.izumin.android.isac2014.moverioapp.event;

import android.view.MotionEvent;

import java.util.EventObject;

/**
 * Created by izumin on 4/13/14.
 */
public class TouchEvent extends EventObject {
    public static final String TAG = TouchEvent.class.getSimpleName();

    private MotionEvent mEvent;
    /**
     * Constructs a new instance of this class.
     *
     * @param source the object which fired the event.
     */
    public TouchEvent(Object source, MotionEvent event) {
        super(source);
        mEvent = event;
    }

    public MotionEvent getEvent() { return mEvent; }
}
