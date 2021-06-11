// For data export
// C:\Users\Oskars\Downloads\adb>adb -d shell "run-as com.example.oskars.xyzregister_v2 cat /data/data/com.example.oskars.xyzregister_v2/files/accData.txt" > accData.txt


package io.github.introml.activityrecognition;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener, TextToSpeech.OnInitListener {
    private float[] results;
    private static final int N_SAMPLES = 200;
    private static List<Float> x, y, z;
    private static List<Long> timestamps;
    private static String filename;
    private static String[] labels = {"Downstairs", "Limping", "Standing", "Upstairs", "Walking", ""};
    private TextView downstairsTextView, joggingTextView, sittingTextView, standingTextView, upstairsTextView, walkingTextView;
    private TextView firstTextView, secondTextView, thirdTextView, forthTextView, fiftTextView, sixtTextView;
    private TextView logTextView;
    private TextToSpeech textToSpeech;
    private TensorFlowClassifier classifier;
    private Switch ClassifierSwitch;
    private Switch SoundSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ClassifierSwitch = (Switch) findViewById(R.id.classifierSwitch);
        SoundSwitch = (Switch) findViewById(R.id.soundSwitch);
        ClassifierSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked){
                    onPause();
                }
                else {
                    onResume();
                }
            }
        });
        prepareTextviews();
        setTextviews();
        x = new ArrayList<>();
        y = new ArrayList<>();
        z = new ArrayList<>();
        timestamps = new ArrayList<>();
        classifier = new TensorFlowClassifier(getApplicationContext());
        textToSpeech = new TextToSpeech(this, this);
        textToSpeech.setLanguage(Locale.US);
        cleanFile(getBaseContext());
    }

    @Override
    public void onInit(int status) {
    }

    protected void onPause() {
        getSensorManager().unregisterListener(this);
        super.onPause();
    }

    protected void onResume() {
        super.onResume();
        getSensorManager().registerListener(this, getSensorManager().getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            activityPrediction();
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            x.add(event.values[0]);
            y.add(event.values[1]);
            z.add(event.values[2]);
            String dataLine = String.format("%d,%s,%s,%s%n",
                    timestamp.getTime(), x.get(x.size() - 1), y.get(y.size() - 1), z.get(z.size() - 1));
            writeToFile(dataLine, getBaseContext());
            timestamps.add(timestamp.getTime());
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void activityPrediction() {
        if (x.size() == N_SAMPLES && y.size() == N_SAMPLES && z.size() == N_SAMPLES) {
            new Thread(new secondThread(x, y, z, timestamps)).start();
        }
    }

    private void cleanFile(Context context) {
        try {
            Date date = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd");
            filename = "accData-" + dateFormat.format(date) + ".txt";
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(filename, Context.MODE_PRIVATE));
            outputStreamWriter.write("");
            outputStreamWriter.close();
            logTextView.setText(String.format("File (%s) cleaned", filename));
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private SensorManager getSensorManager() {
        return (SensorManager) getSystemService(SENSOR_SERVICE);
    }

    private void prepareTextviews() {
        downstairsTextView = (TextView) findViewById(R.id.downstairs_prob);
        joggingTextView = (TextView) findViewById(R.id.jogging_prob);
        sittingTextView = (TextView) findViewById(R.id.sitting_prob);
        standingTextView = (TextView) findViewById(R.id.standing_prob);
        upstairsTextView = (TextView) findViewById(R.id.upstairs_prob);
        walkingTextView = (TextView) findViewById(R.id.walking_prob);
        firstTextView = (TextView) findViewById(R.id.downstairs_title);
        secondTextView = (TextView) findViewById(R.id.jogging_title);
        thirdTextView = (TextView) findViewById(R.id.sitting_title);
        forthTextView = (TextView) findViewById(R.id.standing_title);
        fiftTextView = (TextView) findViewById(R.id.upstairs_title);
        sixtTextView = (TextView) findViewById(R.id.walking_title);
        logTextView = (TextView) findViewById(R.id.log);
    }

    private void setTextviews() {
        firstTextView.setText(labels[0]);
        secondTextView.setText(labels[1]);
        thirdTextView.setText(labels[2]);
        forthTextView.setText(labels[3]);
        fiftTextView.setText(labels[4]);
        sixtTextView.setText(labels[5]);
    }

    private static float round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

    private float[] toFloatArray(List<Float> list) {
        int i = 0;
        float[] array = new float[list.size()];
        for (Float f : list) {
            array[i++] = (f != null ? f : Float.NaN);
        }
        return array;
    }

    private void writeToFile(String data, Context context) {
        try {
            Date date = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd");
            filename = "accData-" + dateFormat.format(date) + ".txt";
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(filename, Context.MODE_APPEND));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    class secondThread implements Runnable {
        List<Float> data = new ArrayList<>();
        private List<Float> x;
        private List<Float> y;
        private List<Float> z;
        private List<Long> timestamps;

        secondThread(List<Float> x, List<Float> y, List<Float> z, List<Long> timestamps) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.timestamps = timestamps;
            data.addAll(x);
            data.addAll(y);
            data.addAll(z);
        }

        @Override
        public void run() {
            List<Float> new_data = new ArrayList<>();
            for (int j = 0; j < 200; j++) {
                new_data.add(data.get(j));
                new_data.add(data.get(200 + j));
                new_data.add(data.get(400 + j));
            }

            results = classifier.predictProbabilities(toFloatArray(new_data));
            timestamps.clear();
            x.clear();
            y.clear();
            z.clear();
            float max = -1;
            int idx = -1;
            for (int i = 0; i < results.length; i++) {
                if (results[i] > max) {
                    idx = i;
                    max = results[i];
                }
            }
            if(SoundSwitch.isChecked() == true){
                textToSpeech.speak(labels[idx], TextToSpeech.QUEUE_ADD, null, null);
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Date date = new Date();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("mm-ss");
                    downstairsTextView.setText(Float.toString(round(results[0], 2)));
                    joggingTextView.setText(Float.toString(round(results[1], 2)));
                    sittingTextView.setText(Float.toString(round(results[2], 2)));
                    standingTextView.setText(Float.toString(round(results[3], 2)));
                    upstairsTextView.setText(Float.toString(round(results[4], 2)));
                    walkingTextView.setText(Float.toString(round(results[5], 2)));
                    logTextView.setText(dateFormat.format(date));
                }
            });
        }
    }
}
