package ball.pinball;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getName();
    TextView tv_output;
    private PinBallView pinBallView;
    private SensorManager mSensorManager;
    private WindowManager mWindowManager;
    private Display mDisplay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_output = (TextView) findViewById(R.id.tv_output);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {

            mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            mDisplay = mWindowManager.getDefaultDisplay();

            pinBallView = (PinBallView) findViewById(R.id.pinBallArea);
            pinBallView.setInit(mSensorManager, mDisplay, this);
            pinBallView.setListener(pinballListener);
        } else {
            tv_output.setText("This device don't have an accelerometer.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        pinBallView.startSimulation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        pinBallView.stopSimulation();
    }

    OnPinballClickedListener pinballListener = new OnPinballClickedListener() {
        @Override
        public void onGameRequestClicked(int clicksNumber) {
            Log.wtf(TAG, "Profile clicked: " + clicksNumber);
            tv_output.setText("Game finished.");
            pinBallView.stopSimulation();
        }
    };

}
