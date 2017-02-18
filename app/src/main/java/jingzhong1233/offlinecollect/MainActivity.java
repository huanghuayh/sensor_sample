package jingzhong1233.offlinecollect;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.support.annotation.RequiresApi;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Vector;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

//import event.BusProvider;
import event.BusProvider;
import event.NewLetterEvent;
import event.RecordFinishEvent;

public class MainActivity extends WearableActivity {

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);

    private BoxInsetLayout mContainerView;
    private TextView mTextView;
    private TextView mClockView;


    TextView textX, textY, textZ;
    SensorManager sensorManager;
    //	Sensor mag_sensor;
    Sensor acc_uncalib_sensor;
//    Sensor gyro_uncalib_sensor;
    Sensor grav_uncalib_sensor;
    Sensor game_sensor;

    File acc_file;
//    File gyro_file;
    File grav_file;
    File game_file;
    //	File cali_file;



    public String[] LETTER_LST={"1","2"};

    public String current_letter;

    public String folder_name;


    Vector<double[]> xy_mag_data;

    final String TAG="main";

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
//        mTextView = (TextView) findViewById(R.id.text);
        mClockView = (TextView) findViewById(R.id.clock);


        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        acc_uncalib_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
//        gyro_uncalib_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        game_sensor=sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
        grav_uncalib_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

//        Bus bus=BusProvider.getInstance();
//        bus.register(this);


//        BusProvider.getInstance().unregister(this);
//
//        BusProvider.getInstance().register(this);


        String[] perms = {"android.permission.WRITE_EXTERNAL_STORAGE","android.permission.VIBRATE"};

        int permsRequestCode = 200;

        requestPermissions(perms, permsRequestCode);


    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
//        BusProvider.getInstance().unregister(this);
        super.onExitAmbient();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));
//            mTextView.setTextColor(getResources().getColor(android.R.color.white));
            mClockView.setVisibility(View.VISIBLE);

            mClockView.setText(AMBIENT_DATE_FORMAT.format(new Date()));
        } else {
            mContainerView.setBackground(null);
//            mTextView.setTextColor(getResources().getColor(android.R.color.black));
            mClockView.setVisibility(View.GONE);
        }
    }

    SensorEventListener acc_uncali_Listener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int acc) { }

        public void onSensorChanged(SensorEvent event) {
            write_file(acc_file,event);
        }
    };
    SensorEventListener grav_uncali_Listener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int acc) { }

        public void onSensorChanged(SensorEvent event) {
            write_file(grav_file,event);
        }
    };
//    SensorEventListener gyro_uncali_Listener = new SensorEventListener() {
//        public void onAccuracyChanged(Sensor sensor, int acc) { }
//
//        public void onSensorChanged(SensorEvent event) {
//            write_file(gyro_file,event);
//        }
//    };

    SensorEventListener game_uncali_Listener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int acc) { }

        public void onSensorChanged(SensorEvent event) {
            write_file(game_file,event);
        }
    };

    private void write_file(File f,SensorEvent event){



        long time=event.timestamp;
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        float[] orientationVals=new float[3];
        if(event.sensor.getType()==Sensor.TYPE_GAME_ROTATION_VECTOR){

            float[] mRotationMatrix = new float[16];
            SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);
            SensorManager.getOrientation(mRotationMatrix, orientationVals);

            x = (float) Math.toDegrees(orientationVals[0]);
            y = (float) Math.toDegrees(orientationVals[1]);
            z = (float) Math.toDegrees(orientationVals[2]);

        }


        StringBuilder content=new StringBuilder();
        content.append(time);
        content.append(",");

        content.append(x);
        content.append(",");

        content.append(y);
        content.append(",");

        content.append(z);
        content.append("\n");

        try {
            if (!f.exists()) {
                f.getParentFile().mkdirs();
                f.createNewFile();
            }

            FileWriter fw = new FileWriter(f.getAbsoluteFile(),true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.append(content.toString());
            bw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    LetterCounter m_letter_counter;


    boolean is_collect=false;
    public void writing_train_btn_clicked(View view){


        if(!is_collect){


            String baseDir= Environment.getExternalStorageDirectory().getAbsolutePath();
            Time now = new Time();
            now.setToNow();
            folder_name=baseDir+"/mag_uncali/"+now.year+now.month+now.monthDay+now.hour+now.minute+now.second+"/";

            long[] pattern = {0, 1000};
            Log.d(TAG,"event triggered");
            run_vibration(pattern);


            sensorManager.unregisterListener(acc_uncali_Listener);
            sensorManager.unregisterListener(game_uncali_Listener);
            sensorManager.unregisterListener(grav_uncali_Listener);
            if(acc_file!=null && acc_file.exists()){
                acc_file=null;
            }
            if(game_file!=null && game_file.exists()){
                game_file=null;
            }
            if(grav_file!=null && grav_file.exists()){
                grav_file=null;
            }

            acc_file=new File(folder_name+"acc_"+current_letter+".csv");
            grav_file=new File(folder_name+"grav_"+current_letter+".csv");
            game_file=new File(folder_name+"game_"+current_letter+".csv");

            sensorManager.registerListener(acc_uncali_Listener, acc_uncalib_sensor,
                    SensorManager.SENSOR_DELAY_GAME);
            sensorManager.registerListener(game_uncali_Listener, game_sensor,
                    SensorManager.SENSOR_DELAY_GAME);
            sensorManager.registerListener(grav_uncali_Listener, grav_uncalib_sensor,
                    SensorManager.SENSOR_DELAY_GAME);

            is_collect=true;

        }
        else{
            sensorManager.unregisterListener(acc_uncali_Listener);
            sensorManager.unregisterListener(game_uncali_Listener);
            sensorManager.unregisterListener(grav_uncali_Listener);
            if(acc_file!=null && acc_file.exists()){
                acc_file=null;
            }
            if(game_file!=null && game_file.exists()){
                game_file=null;
            }
            if(grav_file!=null && grav_file.exists()){
                grav_file=null;
            }

            is_collect=false;
        }


    }

    private void run_vibration(long[] pattern){
        Log.d(TAG,"try to vibrate");
        Vibrator vibrator= (Vibrator) this.getSystemService(VIBRATOR_SERVICE);
        if(vibrator.hasVibrator()){
            vibrator.vibrate(pattern,-1);
        }
        else{
            Log.d(TAG, "Do not have vibration service ");
        }
    }



    @Subscribe
    public void onNewLetterEvent(final NewLetterEvent event) {
        // do something
        long[] pattern = {0, 1000};
        Log.d(TAG,"event triggered");
        run_vibration(pattern);


        sensorManager.unregisterListener(acc_uncali_Listener);
        sensorManager.unregisterListener(game_uncali_Listener);
        sensorManager.unregisterListener(grav_uncali_Listener);
        if(acc_file!=null && acc_file.exists()){
            acc_file=null;
        }
        if(game_file!=null && game_file.exists()){
            game_file=null;
        }
        if(grav_file!=null && grav_file.exists()){
            grav_file=null;
        }
        Time now = new Time();
        now.setToNow();
        String baseDir= Environment.getExternalStorageDirectory().getAbsolutePath();
        current_letter=event.get_current_letter();
//        uncali_file=new File(baseDir+"/mag_uncali/"+current_letter+"_"+now.year+now.month+now.monthDay+now.hour+now.minute+now.second+".csv");
//        uncali_file=new File(baseDir+"/mag_uncali/"+now.year+now.month+now.monthDay+now.hour+now.minute+now.second+"/"+current_letter+".csv");
        acc_file=new File(folder_name+"acc_"+current_letter+".csv");
        grav_file=new File(folder_name+"grav_"+current_letter+".csv");
        game_file=new File(folder_name+"game_"+current_letter+".csv");

        sensorManager.registerListener(acc_uncali_Listener, acc_uncalib_sensor,
                SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(game_uncali_Listener, game_sensor,
                SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(grav_uncali_Listener, grav_uncalib_sensor,
                SensorManager.SENSOR_DELAY_GAME);

        TextView tv=(TextView)findViewById(R.id.current_letter);
        tv.setVisibility(View.VISIBLE);
        tv.setText(event.get_current_letter());




    }
    @Subscribe
    public void onRecordFinishEvent(final RecordFinishEvent event) {
        // do something

        Log.d(TAG, "try to end sensor service");

        sensorManager.unregisterListener(acc_uncali_Listener);
        sensorManager.unregisterListener(game_uncali_Listener);
        sensorManager.unregisterListener(grav_uncali_Listener);
        if(acc_file!=null && acc_file.exists()){
            acc_file=null;
        }
        if(game_file!=null && game_file.exists()){
            game_file=null;
        }
        if(grav_file!=null && grav_file.exists()){
            grav_file=null;
        }





    }
}
