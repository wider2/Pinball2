package ball.pinball;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class PinBallView extends View implements SensorEventListener {

    private SensorManager mSensorManager;
    private Display mDisplay;
    private Activity activity;

    private static final float sBallDiameter = 0.004f;
    private static final float sBallDiameter2 = sBallDiameter * sBallDiameter;

    private static final float sFriction = 0.1f;
    private Sensor mAccelerometer;
    private long mLastT;
    private float mLastDeltaT;
    private float mXDpi;
    private float mYDpi;
    private float mMetersToPixelsX;
    private float mMetersToPixelsY;
    private Bitmap mBitmap;

    private float mXOrigin;
    private float mYOrigin;
    private float mSensorX;
    private float mSensorY;
    private long mSensorTimeStamp;
    private long mCpuTimeStamp;
    private float mHorizontalBound;
    private float mVerticalBound;
    private final ParticleSystem mParticleSystem = new ParticleSystem();

    private boolean stopEvent = false;
    private List<RingPoint> activeRings;
    private List<Barrier> listBarriers;
    private Bitmap activeTarget;
    private OnPinballClickedListener pinballListener;
    private int countCoincidence = 0;
    float prevX = 0, prevY = 0;

    private int widthPixels, heightPixels;

    class Particle {
        private float mPosX;
        private float mPosY;
        private float mAccelX;
        private float mAccelY;
        private float mLastPosX;
        private float mLastPosY;
        private float mOneMinusFriction;

        Particle() {
            final float r = ((float) Math.random() - 0.5f) * 0.2f;
            mOneMinusFriction = 1.0f - sFriction + r;
        }

        public void computePhysics(float sx, float sy, float dT, float dTC) {

            final float m = 1000.0f;
            final float gx = -sx * m;
            final float gy = -sy * m;

            final float invm = 1.0f / m;
            final float ax = gx * invm;
            final float ay = gy * invm;

            final float dTdT = dT * dT;
            final float x = mPosX + mOneMinusFriction * dTC * (mPosX - mLastPosX) + mAccelX
                    * dTdT;
            final float y = mPosY + mOneMinusFriction * dTC * (mPosY - mLastPosY) + mAccelY
                    * dTdT;
            mLastPosX = mPosX;
            mLastPosY = mPosY;
            mPosX = x;
            mPosY = y;
            mAccelX = ax;
            mAccelY = ay;
        }

        public void resolveCollisionWithBounds() {
            //float barrierX, barrierY, barrierX2, barrierY2;
            //float coordX, coordX2, coordY, coordY2;
            final float xmax = mHorizontalBound;
            final float ymax = mVerticalBound;
            float x = mPosX;
            float y = mPosY;
            if (x > xmax) {
                mPosX = xmax;
            } else if (x < -xmax) {
                mPosX = -xmax;
            }
            if (y > ymax) {
                mPosY = ymax;
            } else if (y < -ymax) {
                mPosY = -ymax;
            }

            for (int b = 0; b < listBarriers.size(); b++) {
                Barrier barrier = listBarriers.get(b);
                if (barrier != null) {

                }
            }
        }
    }

    //initial array
    class ParticleSystem {
        private Particle mBalls[] = new Particle[3];

        ParticleSystem() {
            for (int i = 0; i < mBalls.length; i++) {
                mBalls[i] = new Particle();
            }
        }

        public void removeParticle(int index) {
            for (int i = 0; i < mBalls.length; i++) {
                if (i == index) {
                    mBalls[i] = null;
                }
            }
        }

        private void updatePositions(float sx, float sy, long timestamp) {
            final long t = timestamp;
            if (mLastT != 0) {
                final float dT = (float) (t - mLastT) * (1.0f / 1000000000.0f);
                if (mLastDeltaT != 0) {
                    final float dTC = dT / mLastDeltaT;
                    final int count = mBalls.length;
                    for (int i = 0; i < count; i++) {
                        Particle ball = mBalls[i];
                        if (ball != null) {
                            ball.computePhysics(sx, sy, dT, dTC);
                        }
                    }
                }
                mLastDeltaT = dT;
            }
            mLastT = t;
        }

        // Resolve collisions
        public void update(float sx, float sy, long now) {

            updatePositions(sx, sy, now);

            final int NUM_MAX_ITERATIONS = 10;
            boolean more = true;
            final int count = mBalls.length;
            for (int k = 0; k < NUM_MAX_ITERATIONS && more; k++) {
                more = false;
                for (int i = 0; i < count; i++) {
                    Particle curr = mBalls[i];
                    if (curr != null) {
                        for (int j = i + 1; j < count; j++) {
                            Particle ball = mBalls[j];
                            if (ball != null) {
                                //if (ball.mPosX != -1 && ball.mPosY != -1) {
                                float dx = ball.mPosX - curr.mPosX;
                                float dy = ball.mPosY - curr.mPosY;
                                float dd = dx * dx + dy * dy;
                                // Check for collisions with additional entropy
                                if (dd <= sBallDiameter2) {
                                    dx += ((float) Math.random() - 0.5f) * 0.0001f;
                                    dy += ((float) Math.random() - 0.5f) * 0.0001f;
                                    dd = dx * dx + dy * dy;

                                    final float d = (float) Math.sqrt(dd);
                                    final float c = (0.5f * (sBallDiameter - d)) / d;
                                    curr.mPosX -= dx * c;
                                    curr.mPosY -= dy * c;
                                    ball.mPosX += dx * c;
                                    ball.mPosY += dy * c;
                                    more = true;
                                }
                            }
                        }
                        curr.resolveCollisionWithBounds();
                    }
                }
            }
        }

        public int getParticleCount() {
            return mBalls.length;
        }

        public float getPosX(int i) {
            return mBalls[i].mPosX;
        }

        public float getPosY(int i) {
            return mBalls[i].mPosY;
        }
    }

    public void startSimulation() {
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    public void stopSimulation() {
        mSensorManager.unregisterListener(this);
        stopEvent = true;
    }

    public void setInit(SensorManager mSensorManager, Display mDisplay, Activity activity) {
        this.mSensorManager = mSensorManager;
        this.mDisplay = mDisplay;
        this.activity = activity;

        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mXDpi = metrics.xdpi;
        mYDpi = metrics.ydpi;
        mMetersToPixelsX = mXDpi / 0.0254f;
        mMetersToPixelsY = mYDpi / 0.0254f;

        Bitmap ball = BitmapFactory.decodeResource(getResources(), R.drawable.soccer80);
        final int dstWidth = (int) (sBallDiameter * mMetersToPixelsX + 0.5f); //0.5 cm on screen
        final int dstHeight = (int) (sBallDiameter * mMetersToPixelsY + 0.5f);
        mBitmap = Bitmap.createScaledBitmap(ball, dstWidth, dstHeight, true);
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inDither = true;
        opts.inPreferredConfig = Bitmap.Config.RGB_565;

        widthPixels = metrics.widthPixels;
        heightPixels = metrics.heightPixels;

        activeRings = new ArrayList<RingPoint>();
        RingPoint ringPoint = new RingPoint(mDisplay.getWidth() / 3, mDisplay.getHeight() / 3, 100, Color.CYAN);
        activeRings.add(ringPoint);

        activeTarget = BitmapFactory.decodeResource(getResources(), R.drawable.target150, opts);

        listBarriers = new ArrayList<>();
        Barrier barrier = new Barrier(100, 100, 150, 150, 0);
        listBarriers.add(barrier);

    }

    public PinBallView(Context context) {
        super(context);
    }

    public PinBallView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PinBallView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setListener(OnPinballClickedListener pinballListener) {
        this.pinballListener = pinballListener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!stopEvent) {
            int pointerIndex = event.getActionIndex();
            int pointerId = event.getPointerId(pointerIndex);
            int maskedAction = event.getActionMasked();

            switch (maskedAction) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN: {
                    // We have a new pointer. Lets add it to the list of pointers
                    PointF f = new PointF();
                    f.x = event.getX(pointerIndex);
                    f.y = event.getY(pointerIndex);
                    Toast.makeText(getContext(), "Object:" + f.x + " x " + f.y, Toast.LENGTH_SHORT).show();
                    break;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mXOrigin = (w - mBitmap.getWidth()) * 0.5f;
        mYOrigin = (h - mBitmap.getHeight()) * 0.5f;
        mHorizontalBound = ((w / mMetersToPixelsX - sBallDiameter) * 0.5f);
        mVerticalBound = ((h / mMetersToPixelsY - sBallDiameter) * 0.5f);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
            return;

        switch (mDisplay.getRotation()) {
            case Surface.ROTATION_0:
                mSensorX = event.values[0];
                mSensorY = event.values[1];
                break;
            case Surface.ROTATION_90:
                mSensorX = -event.values[1];
                mSensorY = event.values[0];
                break;
            case Surface.ROTATION_180:
                mSensorX = -event.values[0];
                mSensorY = -event.values[1];
                break;
            case Surface.ROTATION_270:
                mSensorX = event.values[1];
                mSensorY = -event.values[0];
                break;
        }
        mSensorTimeStamp = event.timestamp;
        mCpuTimeStamp = System.nanoTime();
    }

    @Override
    protected void onDraw(Canvas canvas) {

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG); // to show text above the bg gradient
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);

        Barrier barrier;
        boolean coincidenceFound;
        int ringX, ringY, ringRadius;
        RingPoint ringPoint;

        for (int r = 0; r < activeRings.size(); r++) {
            ringPoint = activeRings.get(r);
            if (ringPoint != null) {
                canvas.drawBitmap(activeTarget, ringPoint.getX() - ringPoint.getRadius(), ringPoint.getY() - ringPoint.getRadius(), null);
            }
        }

        for (int b = 0; b < listBarriers.size(); b++) {
            barrier = listBarriers.get(b);
            if (barrier != null) {
                paint.setColor(Color.BLACK);
                paint.setStrokeWidth(1);
                canvas.drawRect(barrier.getX(), barrier.getY(), barrier.getX2(), barrier.getY2(), paint);
                paint.setStrokeWidth(0);
                paint.setColor(Color.BLUE);
                canvas.drawRect(barrier.getX() + 1, barrier.getY() + 1, barrier.getX2() - 1, barrier.getY2() - 1, paint);
            }
        }

        // compute the new position of our object, based on accelerometer data and present time.

        final ParticleSystem particleSystem = mParticleSystem;
        final long now = mSensorTimeStamp + (System.nanoTime() - mCpuTimeStamp);
        final float sx = mSensorX;
        final float sy = mSensorY;
        particleSystem.update(sx, sy, now);
        final float xc = mXOrigin;
        final float yc = mYOrigin;
        final float xs = mMetersToPixelsX;
        final float ys = mMetersToPixelsY;
        final Bitmap bitmap = mBitmap;
        final int count = particleSystem.getParticleCount();
        for (int i = 0; i < count; i++) {
            if (particleSystem.mBalls[i] != null) {

                //Transform canvas : oordinate system matches the sensors coordinate system with the origin in the center of the screen and the unit is the meter.

                float x = xc + particleSystem.getPosX(i) * xs;
                float y = yc - particleSystem.getPosY(i) * ys;
                //canvas.drawBitmap(bitmap, x, y, null); //old idea to draw ball

                for (int b = 0; b < listBarriers.size(); b++) {
                    barrier = listBarriers.get(b);
                    if (barrier != null) {
                        if (barrier.getX() - 1 <= x && x <= barrier.getX2() + 1) {
                            if (barrier.getX() <= x && x <= barrier.getX2()) {
                                if (x >= barrier.getX()) {
                                    x = barrier.getX();
                                } else if (x <= barrier.getX()) {
                                    x = barrier.getX();
                                } else if (x <= barrier.getX2()) {
                                    x = barrier.getX2();
                                } else if (x >= barrier.getX2()) {
                                    x = barrier.getX2();
                                }
                            }
                        }
                        if (barrier.getY() - 1 <= x && x <= barrier.getY2() + 1) {
                            if (barrier.getY() <= y && y <= barrier.getY2()) {
                                if (y >= barrier.getY()) {
                                    y = barrier.getY();
                                } else if (y <= barrier.getY()) {
                                    y = barrier.getY();
                                } else if (y <= barrier.getY2()) {
                                    y = barrier.getY2();
                                } else if (y >= barrier.getY2()) {
                                    y = barrier.getY2();
                                }
                            }
                        }
                    }
                }

                coincidenceFound = false;
                for (int r = 0; r < activeRings.size(); r++) {
                    ringPoint = activeRings.get(r);
                    if (ringPoint != null) {
                        ringX = ringPoint.getX();
                        ringY = ringPoint.getY();
                        ringRadius = ringPoint.getRadius();
                        if ((Math.abs(ringX - x) <= ringRadius - 1) && (Math.abs(ringY - y) <= ringRadius - 1)) {
                            coincidenceFound = true;
                            particleSystem.removeParticle(i);
                            countCoincidence += 1;
                        }
                    }
                }
                if (!coincidenceFound) {
                    canvas.drawBitmap(bitmap, x, y, null);
                }

            }
        }
        if (count == countCoincidence) {
            pinballListener.onGameRequestClicked(countCoincidence);
        }

        invalidate();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
