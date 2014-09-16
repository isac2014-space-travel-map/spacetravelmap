package info.izumin.android.isac2014.moverioapp.model;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Created by izumin on 2/12/14.
 */
public class RequestQueueProvider {
    private static final String TAG = RequestQueueProvider.class.getSimpleName();
    private final RequestQueueProvider self = this;

    private static RequestQueue mQueue = null;

    private RequestQueueProvider() {}

    public static RequestQueue getInstance() {
        return mQueue;
    }

    public static RequestQueue getInstance(Context context) {
        if (mQueue == null) {
            mQueue = Volley.newRequestQueue(context);
        }
        return mQueue;
    }

    public static void cancelRequest(final String tag) {
        mQueue.cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return request.getTag().equals(tag);
            }
        });
    }
}
