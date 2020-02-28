package com.ferreirae.growth;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.amazonaws.amplify.generated.graphql.CreateMenteeMutation;
import com.amazonaws.amplify.generated.graphql.ListMenteesQuery;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferService;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.fetcher.ResponseFetcher;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;

import javax.annotation.Nonnull;

import type.CreateMenteeInput;

import static com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers.NETWORK_FIRST;

public class MainActivity extends AppCompatActivity {

    static String TAG = "mnf";
    AWSAppSyncClient mAWSAppSyncClient;

    static String CHANNEL_ID = "101";
    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getApplicationContext().startService(new Intent(getApplicationContext(), TransferService.class));

        mAWSAppSyncClient = AWSAppSyncClient.builder()
                .context(getApplicationContext())
                .awsConfiguration(new AWSConfiguration(getApplicationContext()))
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // THESE ARE USER FACING
            // DO NOT MESS THIS UP
            CharSequence name = "Channel";
            String description = "description";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Remember to grow!")
                .setContentText("Career growth matters!")
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

// notificationId is a unique int for each notification that you must define
        notificationManager.notify((int)(Math.random() * 100.0), builder.build());

    }

    public void goToMyProfile(View v) {
        // get the name they typed in and figure out if they exist
        EditText usernameEditText = findViewById(R.id.editText);
        final String username = usernameEditText.getText().toString();
        // figure out if that username is in DB already
        ListMenteesQuery query = ListMenteesQuery.builder().build();
        mAWSAppSyncClient.query(query)
                .responseFetcher(NETWORK_FIRST)
                .enqueue(new GraphQLCall.Callback<ListMenteesQuery.Data>() {
                    @Override
                    public void onResponse(@Nonnull Response<ListMenteesQuery.Data> response) {
                        Log.i("mnf", response.data().toString());
                        // populate a hashset with the names from DB: would be good if we needed to check containment more than once
                        for (ListMenteesQuery.Item i : response.data().listMentees().items()) {
                            if (i.name().equals(username)) {
                                // i is the user we were looking for!
                                // move to the next activity and return!
                                Intent intent = new Intent(MainActivity.this, MyProfileActivity.class);
                                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putString("username", username);
                                editor.putString("id", i.id());
                                editor.putStringSet("mentors",
                                        i.mentors() == null ? new HashSet<String>() : new HashSet<String>(i.mentors()));
                                editor.apply();
                                startActivity(intent);
                                return;
                            }
                        }
                        CreateMenteeInput createMenteeInput = CreateMenteeInput.builder()
                                .name(username)
                                .build();
                        mAWSAppSyncClient.mutate(CreateMenteeMutation.builder().input(createMenteeInput).build())
                                .enqueue(new GraphQLCall.Callback<CreateMenteeMutation.Data>() {
                                    @Override
                                    public void onResponse(@Nonnull Response<CreateMenteeMutation.Data> response) {

                                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                        SharedPreferences.Editor editor = preferences.edit();
                                        editor.putString("username", username);
                                        editor.putString("id", response.data().createMentee().id());
                                        editor.putStringSet("mentors", new HashSet<String>());
                                        editor.apply();Intent intent = new Intent(MainActivity.this, MyProfileActivity.class);
                                        startActivity(intent);
                                    }

                                    @Override
                                    public void onFailure(@Nonnull ApolloException e) {

                                    }
                                });
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        Log.e("mnf", e.toString());
                    }
                });

    }


}
