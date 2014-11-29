package info.izumin.android.isac2014.moverioapp;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.NetworkImageView;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.InstallCallbackInterface;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import info.izumin.android.isac2014.moverioapp.model.RequestQueueProvider;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import android.widget.ImageView;
import android.graphics.BitmapFactory;

public class MainActivity extends Activity implements View.OnTouchListener, CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = MainActivity.class.getSimpleName();

    private boolean              mIsColorSelected = false;
    private Mat                  mRgba;
    private Scalar               mBlobColorRgba;
    private Scalar               mBlobColorHsv;
    private ColorBlobDetector    mDetector;
    private Mat                  mSpectrum;
    private Size                 SPECTRUM_SIZE;
    private Scalar               CONTOUR_COLOR;

    private int mWidth, mHeight;
    private Handler mHandler;

    @InjectView(R.id.spot) TextView spotView;
    @InjectView(R.id.legend) TextView legendView;
    @InjectView(R.id.latitude) TextView latitudeView;
    @InjectView(R.id.longitude) TextView longitudeView;
//  @InjectView(R.id.imageView) NetworkImageView mImageView;
    @InjectView(R.id.imageView) ImageView localImageView;

    private CameraBridgeViewBase mCameraView;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch(status) {
            case LoaderCallbackInterface.SUCCESS:
                mCameraView.enableView();
                mCameraView.setOnTouchListener(MainActivity.this);
                break;
            default:
                super.onManagerConnected(status);
                break;
            }
        }

        @Override
        public void onPackageInstall(int operation, InstallCallbackInterface callback) {
            super.onPackageInstall(operation, callback);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        mCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_view);
        mCameraView.setCvCameraViewListener(this);
        RequestQueueProvider.getInstance(getApplicationContext());
        mHandler = new Handler();
        ButterKnife.inject(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mCameraView != null)
            mCameraView.disableView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_8, this, mLoaderCallback);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCameraView != null)
            mCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mWidth = width;
        mHeight = height;
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mDetector = new ColorBlobDetector();
        mSpectrum = new Mat();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
        SPECTRUM_SIZE = new Size(200, 64);
        CONTOUR_COLOR = new Scalar(255,0,0,255);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        if (mIsColorSelected) {
            mDetector.process(mRgba);
            List<MatOfPoint> contours = mDetector.getContours();
            Log.e(TAG, "Contours count: " + contours.size());
//            Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);

            Mat colorLabel = mRgba.submat(mHeight-68, mHeight-4, 4, 68);
            colorLabel.setTo(mBlobColorRgba);

            Mat spectrumLabel = mRgba.submat(mHeight - 4 - mSpectrum.rows(), mHeight - 4, 70, 70 + mSpectrum.cols());
            mSpectrum.copyTo(spectrumLabel);
        }

        return mRgba;
    }

    private void fncDrwCircles(Mat circles ,Mat img) {
        double[] data;
        double rho;
        Point pt = new Point();
        for (int i = 0; i < circles.cols(); i++){
            data = circles.get(0, i);
            pt.x = data[0];
            pt.y = data[1];
            rho = data[2];
            Mat sub = new Mat();
            try {
                img.submat((int) (pt.x - rho / Math.sqrt(2)), (int) (pt.x + rho / Math.sqrt(2)),
                        (int) (pt.y - rho / Math.sqrt(2)), (int) (pt.y + rho / Math.sqrt(2))).copyTo(sub);
                Scalar color = Core.mean(sub);
                Core.rectangle(img,
                        new Point((int) (pt.x - rho / Math.sqrt(2)), (int) (pt.y - rho / Math.sqrt(2))),
                        new Point((int) (pt.x + rho / Math.sqrt(2)), (int) (pt.y + rho / Math.sqrt(2))),
                        color, Core.FILLED);
            } catch (RuntimeException e) {
                // submatで事故るパターン
            }
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int cols = mRgba.cols();
        int rows = mRgba.rows();

        int xOffset = (mCameraView.getWidth() - cols) / 2;
        int yOffset = (mCameraView.getHeight() - rows) / 2;

        int x = (int)event.getX() - xOffset;
        int y = (int)event.getY() - yOffset;

        Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

        Rect touchedRect = new Rect();

        touchedRect.x = (x>4) ? x-4 : 0;
        touchedRect.y = (y>4) ? y-4 : 0;

        touchedRect.width = (x+4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
        touchedRect.height = (y+4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;

        Mat touchedRegionRgba = mRgba.submat(touchedRect);

        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

        // Calculate average color of touched region
        mBlobColorHsv = Core.sumElems(touchedRegionHsv);
        int pointCount = touchedRect.width*touchedRect.height;
        for (int i = 0; i < mBlobColorHsv.val.length; i++)
            mBlobColorHsv.val[i] /= pointCount;

        mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv);

        Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
                ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");

        mDetector.setHsvColor(mBlobColorHsv);
 //     requestPoint(mDetector.getHueIndex());
        requestLocalJson(mDetector.getHueIndex());

        Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);

        mIsColorSelected = true;

        touchedRegionRgba.release();
        touchedRegionHsv.release();

        return false; // don't need subsequent touch events
    }

    private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }

/*    private void requestPoint(int index) {
        RequestQueueProvider.getInstance().add(
                new JsonObjectRequest("http://spacetravelmap.smellman.org/points/" + index + ".json", null,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(final JSONObject response) {
                                Log.d(TAG, "onResponse");
                                Log.d(TAG, response.toString());
                                try {
                                    Log.d(TAG, "spot: " + response.getString("spot"));
                                    spotView.setText(response.getString("spot"));
                                    legendView.setText(response.getString("legend"));
                                    latitudeView.setText(response.getString("latitude"));
                                    longitudeView.setText(response.getString("longitude"));
                                    String imgUrl = response.getString("imgurl");
                                    Log.d(TAG, imgUrl);
                                    mImageView.setImageUrl(imgUrl, new ImageLoader(RequestQueueProvider.getInstance(), new ImageLoader.ImageCache() {
                                        @Override
                                        public Bitmap getBitmap(String url) {
                                            return null;
                                        }

                                        @Override
                                        public void putBitmap(String url, Bitmap bitmap) {
                                        }
                                    }));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.d(TAG, "onErrorResponse");
                                Log.d(TAG, "message:" + error.getLocalizedMessage());
                            }
                        }
                )
        );
    }
*/
    private void requestLocalJson(int index){
    	String imgUrl = null;

        try {
            JSONObject response = new JSONObject(loadJSONFromAsset());
            response = response.getJSONObject(String.valueOf(index));
            Log.d(TAG, "onResponse");
            Log.d(TAG, response.toString());

            spotView.setText(response.getString("spot"));
            legendView.setText(response.getString("legend"));
            latitudeView.setText("緯度 " + response.getString("latitude"));
            longitudeView.setText("経度 " + response.getString("longitude"));
            imgUrl = response.getString("imgfilename");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
        	InputStream is = getAssets().open(imgUrl);
        	Bitmap bm = BitmapFactory.decodeStream(is);
        	localImageView.setImageBitmap(bm);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public String loadJSONFromAsset() {
        String json = null;
        try {
            InputStream is = getAssets().open("sample.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;

    }
}

