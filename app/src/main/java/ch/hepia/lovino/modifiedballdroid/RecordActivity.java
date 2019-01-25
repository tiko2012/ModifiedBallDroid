package ch.hepia.lovino.modifiedballdroid;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pubnub.api.PNConfiguration;
import com.pubnub.api.PubNub;
import com.pubnub.api.callbacks.PNCallback;
import com.pubnub.api.callbacks.SubscribeCallback;
import com.pubnub.api.enums.PNStatusCategory;
import com.pubnub.api.models.consumer.PNPublishResult;
import com.pubnub.api.models.consumer.PNStatus;
import com.pubnub.api.models.consumer.pubsub.PNMessageResult;
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult;

import java.util.Arrays;

import static android.media.AudioRecord.READ_BLOCKING;
import static android.media.AudioRecord.RECORDSTATE_RECORDING;

public class RecordActivity extends Activity {

    private AudioRecord recorder;
    private PubNub pubnub;

    private String channelName;
    private int xAccel;

    private Button recordButton;
    private Button stopButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);



        recordButton = findViewById(R.id.record_button);
        stopButton = findViewById(R.id.stop_button);

        int audioSource = MediaRecorder.AudioSource.MIC;
        int sampleRateInHz = 44100;
        int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_FLOAT;


        int minBufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
        recorder = new AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat,
                minBufferSize);

        recordButton.setOnClickListener(v -> {
            recorder.startRecording();

            PNConfiguration pnConfiguration = new PNConfiguration();
            pnConfiguration.setSubscribeKey("sub-c-ff3ab45a-1999-11e9-b4a6-026d6924b094");
            pnConfiguration.setPublishKey("pub-c-76e2c24e-a651-4638-872d-fcf099117218");
            pubnub = new PubNub(pnConfiguration);
            channelName = "yourPersonalChannel";

            // create message payload using Gson

            pubnub.addListener(new SubscribeCallback() {
                @Override
                public void status(PubNub pubnub, PNStatus status) {
                    if (status.getCategory() == PNStatusCategory.PNUnexpectedDisconnectCategory) {
                        // This event happens when radio / connectivity is lost
                    } else if (status.getCategory() == PNStatusCategory.PNConnectedCategory) {
                        // Connect event. You can do stuff like publish, and know you'll get it.
                        // Or just use the connected event to confirm you are subscribed for
                        // UI / internal notifications, etc
                        if (status.getCategory() == PNStatusCategory.PNConnectedCategory) {

                        }
                    } else if (status.getCategory() == PNStatusCategory.PNReconnectedCategory) {

                        // Happens as part of our regular operation. This event happens when
                        // radio / connectivity is lost, then regained.
                    } else if (status.getCategory() == PNStatusCategory.PNDecryptionErrorCategory) {
                        // Handle messsage decryption error. Probably client configured to
                        // encrypt messages and on live data feed it received plain text.
                    }
                }

                @Override
                public void message(PubNub pubnub, PNMessageResult message) {
                    // Handle new message stored in message.message
                    if (message.getChannel() != null) {
                        // Message has been received on channel group stored in
                        // message.getChannel()
                    } else {
                        // Message has been received on channel stored in
                        // message.getSubscription()
                    }
                    JsonElement receivedMessageObject = message.getMessage();
                    // extract desired parts of the payload, using Gson
                    String msg = message.getMessage().getAsJsonObject().get("msg").getAsString();
                    //Log.i("msg","Message: "+ msg);
            /*
                log the following items with your favorite logger
                    - message.getMessage()
                    - message.getSubscription()
                    - message.getTimetoken()
            */
                }

                @Override
                public void presence(PubNub pubnub, PNPresenceEventResult presence) {
                }
            });
            pubnub.subscribe().channels(Arrays.asList(channelName)).execute();

            AsyncTask task = new AsyncTask() {
                @Override
                protected Object doInBackground(Object[] objects) {
                    while(recorder.getRecordingState() == RECORDSTATE_RECORDING){
                        updateOnVoice();
                    }
                    return null;
                }
            };
            task.execute();


        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               recorder.stop();
            }
        });


    }


    public void updateOnVoice() {
        float[] audioData = new float[512];
        recorder.read(audioData, 0, 512, READ_BLOCKING);

        double sum = 0;
        for (int i = 0; i < audioData.length; i++) {
            sum += Math.abs(audioData[i]);
        }
        float mean = (float) (sum / audioData.length);

        //System.out.println(mean);


        if (mean < 0.05) {
            xAccel = -15;
        }
        if (mean > 0.05) {
            xAccel = 10;
        }
        if (mean > 0.1) {
            xAccel = 20;
        }
        if (mean > 0.2) {
            xAccel = 30;
        }

        JsonObject messageJsonObject = new JsonObject();
        messageJsonObject.addProperty("msg", String.valueOf(xAccel));
        System.out.println ("Message to send: " + messageJsonObject.toString());







        pubnub.publish().channel(channelName).message(messageJsonObject).async(new PNCallback<PNPublishResult>() {
            @Override
            public void onResponse(PNPublishResult result, PNStatus status) {
                // Check whether request successfully completed or not.
                if (!status.isError()) {
                    // Message successfully published to specified channel.
                }
                // Request processing failed.
                else {
                    // Handle message publish error. Check 'category' property to find out possible issue
                    // because of which request did fail.
                    //
                    // Request can be resent using: [status retry];
                }
            }
        });


    }
}
