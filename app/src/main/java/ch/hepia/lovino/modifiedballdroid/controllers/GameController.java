package ch.hepia.lovino.modifiedballdroid.controllers;


import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.pubnub.api.PNConfiguration;
import com.pubnub.api.PubNub;
import com.pubnub.api.callbacks.SubscribeCallback;
import com.pubnub.api.models.consumer.PNStatus;
import com.pubnub.api.models.consumer.pubsub.PNMessageResult;
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult;

import java.util.ArrayList;
import java.util.Arrays;

import ch.hepia.lovino.modifiedballdroid.GameActivity;
import ch.hepia.lovino.modifiedballdroid.models.Ball;
import ch.hepia.lovino.modifiedballdroid.models.BallDirection;
import ch.hepia.lovino.modifiedballdroid.models.Bonus;
import ch.hepia.lovino.modifiedballdroid.models.DifficultyLevel;
import ch.hepia.lovino.modifiedballdroid.models.Game;
import ch.hepia.lovino.modifiedballdroid.models.Platform;
import ch.hepia.lovino.modifiedballdroid.models.PointArea;
import ch.hepia.lovino.modifiedballdroid.models.Score;
import ch.hepia.lovino.modifiedballdroid.models.Time;
import ch.hepia.lovino.modifiedballdroid.models.db.DBHelper;
import ch.hepia.lovino.modifiedballdroid.views.GameSurfaceView;

import static android.media.AudioRecord.READ_BLOCKING;
import static ch.hepia.lovino.modifiedballdroid.models.db.DBContract.ScoreEntry;

public class GameController {
    private static final int TIMER_SECONDS = 60;
    private GameActivity context;
    private DifficultyLevel difficulty;
    private GameSurfaceView view;
    private float xAccel = 0;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Game game;
    private Ball ball;
    private Score score;
    private Time time;
    private boolean paused = true;
    private TimerThread timer;
    private ArrayList<Bonus> bonusesToRemove;
    private float msg;
    AudioRecord recorder;

    private final SensorEventListener sensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            xAccel = -event.values[0];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            //Do nothing for now
        }
    };

    public GameController(GameActivity context, DifficultyLevel difficulty) {
        this.context = context;
        this.difficulty = difficulty;
        this.view = new GameSurfaceView(context, this);
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            this.accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        }
        this.bonusesToRemove = new ArrayList<>();
        this.timer = new TimerThread(10 * 1000, this);

        int audioSource = MediaRecorder.AudioSource.MIC;
        int sampleRateInHz = 44100;
        int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_FLOAT;


        int minBufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
        recorder = new AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat,
                minBufferSize);

        PNConfiguration pnConfiguration = new PNConfiguration();
        pnConfiguration.setSubscribeKey("sub-c-ff3ab45a-1999-11e9-b4a6-026d6924b094");
        pnConfiguration.setPublishKey("pub-c-76e2c24e-a651-4638-872d-fcf099117218");
        PubNub pubnub = new PubNub(pnConfiguration);
        String channelName = "yourPersonalChannel";
        pubnub.addListener(new SubscribeCallback() {
            @Override
            public void status(PubNub pubnub, PNStatus status) {
            }
            @Override
            public void message(PubNub pubnub, PNMessageResult message) {
                // Handle new message stored in message.message
                if (message.getChannel() != null) {
                    // Message has been received on channel group stored in
                    // message.getChannel()
                }
                else {
                    // Message has been received on channel stored in
                    // message.getSubscription()
                }
                //JsonElement receivedMessageObject = message.getMessage();
                // extract desired parts of the payload, using Gson
                msg = Float.valueOf(message.getMessage().getAsJsonObject().get("msg").getAsString());
                Log.i("msg","Message: "+ msg);
            }

            @Override
        public void presence(PubNub pubnub, PNPresenceEventResult presence) {
        }
    });

        pubnub.subscribe().channels(Arrays.asList(channelName)).execute();



        //recorder.startRecording();


    }

    public void update() {
        if (paused) return;
        this.time.setTimeRemaining((int) timer.getRemainingTime() / 1000);
        xAccel = msg;
        //updateOnVoice();
        this.ball.incrementSpeedX(xAccel);
        this.ball.incrementSpeedY();
        this.ball.updatePosition();
        if (this.ball.getX() > (this.view.getSurfaceWidth() - this.ball.getRadius())) {
            this.ball.setX(this.view.getSurfaceWidth() - this.ball.getRadius());
            this.ball.reboundX();
        }
        if (this.ball.getX() < this.ball.getRadius()) {
            this.ball.setX(this.ball.getRadius());
            this.ball.reboundX();
        }
        BallDirection direction = ball.getDirection();
        for (Platform p : this.game.getPlatforms()) {
            if (ball.getBoundingRect().intersect(p.getBoundingRect())) {
                switch (direction) {
                    case N:
                        reboundBottom(ball, p);
                        break;
                    case NE:
                        reboundBottom(ball, p);
                        reboundLeft(ball, p);
                        break;
                    case E:
                        reboundLeft(ball, p);
                        break;
                    case SE:
                        reboundTop(ball, p);
                        reboundLeft(ball, p);
                        break;
                    case S:
                        reboundTop(ball, p);
                        break;
                    case SW:
                        reboundTop(ball, p);
                        reboundRight(ball, p);
                        break;
                    case W:
                        reboundRight(ball, p);
                        break;
                    case NW:
                        reboundBottom(ball, p);
                        reboundRight(ball, p);
                        break;
                    case STILL:
                        break;
                }
            }
        }

        for (PointArea pointArea : this.game.getPointsAreas()) {
            if (ball.getBoundingRect().intersect(pointArea.getBoundingRect())) {
                score.increment(pointArea.getPoints());
                this.ball.putToStart();
            }
        }

        this.bonusesToRemove.forEach(game::removeBonus);
        this.bonusesToRemove.clear();

        for (Bonus bonus : this.game.getBonuses()) {
            if (ball.getBoundingRect().intersect(bonus.getBoundingRect())) {
                Log.v("BONUS", "Hit a bonus of " + bonus.getSeconds());
                bonusesToRemove.add(bonus);
                timer.addToTime(bonus.getSeconds());
            }
        }
    }

    public void updateOnVoice(){
        float[] audioData = new float[512];
        recorder.read(audioData, 0, 512, READ_BLOCKING);

        double sum = 0;
        for (int i = 0; i < audioData.length; i++) {
            sum += Math.abs(audioData[i]);
        }
        float mean = (float) (sum / audioData.length);

        //System.out.println(mean);


        if(mean < 0.05){
            xAccel = 0;
        }
        if(mean > 0.05){
            xAccel = 10;
        }
        if(mean > 0.1){
            xAccel = 20;
        }
        if(mean > 0.2){
            xAccel = 30;
        }


    }

    private void reboundTop(Ball ball, Platform p) {
        if (Math.abs(this.ball.getY() - p.getBoundingRect().top) < ball.getRadius()) {
            this.ball.reboundY();
            this.ball.setY(p.getBoundingRect().top - ball.getRadius());
        }
    }

    private void reboundBottom(Ball ball, Platform p) {
        if (Math.abs(this.ball.getY() - p.getBoundingRect().bottom) < ball.getRadius()) {
            this.ball.reboundY();
            this.ball.setY(p.getBoundingRect().bottom + ball.getRadius());
        }
    }

    private void reboundLeft(Ball ball, Platform p) {
        if (Math.abs(this.ball.getX() - p.getBoundingRect().left) < ball.getRadius()) {
            this.ball.reboundX();
            this.ball.setX(p.getBoundingRect().left - ball.getRadius());
        }
    }

    private void reboundRight(Ball ball, Platform p) {
        if (Math.abs(this.ball.getX() - p.getBoundingRect().right) < ball.getRadius()) {
            this.ball.reboundX();
            this.ball.setX(p.getBoundingRect().right + ball.getRadius());
        }
    }

    public void start() {
        this.game = new Game(this.difficulty, 0, TIMER_SECONDS, this.view.getSurfaceWidth(), this.view.getSurfaceHeight());
        this.ball = game.getBall();
        this.score = game.getScore();
        this.time = game.getTime();
        this.timer = new TimerThread(this.time.getTimeRemaining() * 1000, this);
        this.timer.start();
    }

    public Game getGame() {
        return game;
    }

    public void resumeGame() {
        //this.sensorManager.registerListener(this.sensorListener, this.accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        paused = false;
        if (this.time != null) {
            this.timer = new TimerThread(this.time.getTimeRemaining() * 1000, this);
            this.timer.start();
        }
    }

    public void pauseGame() {
        this.sensorManager.unregisterListener(this.sensorListener, this.accelerometer);
        paused = true;
        this.timer.stopTimer();
        try {
            this.timer.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public GameSurfaceView getView() {
        return view;
    }

    public boolean isPaused() {
        return paused;
    }

    public void endGame() {
        Log.w("GAME", "Game is over");
        pauseGame();
        try {
            this.timer.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (score.getScore() > 0)
            saveScore();
        context.showEndOfGame(score.getScore());
    }

    private void saveScore() {
        DBHelper dbHelper = new DBHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ScoreEntry.COLUMN_DIFFICULTY, difficulty.ordinal());
        values.put(ScoreEntry.COLUMN_SCORE, score.getScore());
        db.insert(ScoreEntry.TABLE_NAME, null, values);
    }
}
