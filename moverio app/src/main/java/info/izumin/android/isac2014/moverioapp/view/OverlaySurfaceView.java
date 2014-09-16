package info.izumin.android.isac2014.moverioapp.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import info.izumin.android.isac2014.moverioapp.R;


/**
 * Created by izumin on 1/18/14.
 */
public class OverlaySurfaceView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private static final String TAG = OverlaySurfaceView.class.getSimpleName();
    private final OverlaySurfaceView self = this;

    private SurfaceHolder mHolder;
    private Thread mThread;

    private int mWidth, mHeight, mCurrentHeight = 0;
    private int mLineCount, mLineStep, mMoveStep;
    private int mBaseColor;

    private static final int LINE_COUNT = 50, LINE_STEP = 4, MOVE_STEP = 20, DEFAULT_COLOR = 0x6666ff66;

    public OverlaySurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs);
    }

    public OverlaySurfaceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize(context, attrs);
    }

    private void initialize(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.OverlaySurfaceView);
        mLineCount = a.getInteger(R.styleable.OverlaySurfaceView_lineCount, LINE_COUNT);
        mLineStep = a.getInteger(R.styleable.OverlaySurfaceView_lineStep, LINE_STEP);
        mMoveStep = a.getInteger(R.styleable.OverlaySurfaceView_moveStep, MOVE_STEP);
        mBaseColor = a.getColor(R.styleable.OverlaySurfaceView_overlayColor, DEFAULT_COLOR);
        mHolder = getHolder();
        mHolder.setFormat(PixelFormat.TRANSPARENT);
        mHolder.addCallback(this);
        setZOrderMediaOverlay(true);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mThread = new Thread(this);
        mThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        this.mWidth = width;
        this.mHeight = height;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mThread = null;
    }

    @Override
    protected void onDraw(Canvas canvas) {
    }

    @Override
    public void run() {
        while (mThread != null) {
            Canvas canvas = null;
            try {
                synchronized (mHolder) {
                    canvas = mHolder.lockCanvas();

                    if (canvas != null) {
                        canvas.drawColor(mBaseColor, PorterDuff.Mode.SRC);
                        moveLines();
                        drawLines(canvas);
                    }
                }
            } finally {
                if (canvas != null && mThread != null) mHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    private void moveLines() {
        mCurrentHeight += mMoveStep;
        if (mCurrentHeight > mHeight + (mLineCount * mLineStep)) mCurrentHeight = 0;
    }

    private void drawLines(Canvas canvas) {
        Paint paint = new Paint();
        paint.setStrokeWidth(1);
        for (int i = 0; i < mLineCount; i++) {
            paint.setColor(Color.argb(0xff - (0xff/mLineCount)*i, Color.red(mBaseColor), Color.green(mBaseColor), Color.blue(mBaseColor)));
            int y = mCurrentHeight - i * mLineStep;
            canvas.drawLine(0, y, mWidth, y, paint);
        }
    }
}
