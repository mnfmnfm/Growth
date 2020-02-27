package com.ferreirae.growth;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.amazonaws.amplify.generated.graphql.CreateMenteeMutation;
import com.amazonaws.amplify.generated.graphql.ListMenteesQuery;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.fetcher.ResponseFetcher;

import javax.annotation.Nonnull;

import type.CreateMenteeInput;

import static com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers.NETWORK_FIRST;

public class MainActivity extends AppCompatActivity {

    AWSAppSyncClient mAWSAppSyncClient;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAWSAppSyncClient = AWSAppSyncClient.builder()
                .context(getApplicationContext())
                .awsConfiguration(new AWSConfiguration(getApplicationContext()))
                .build();
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
                                        Intent intent = new Intent(MainActivity.this, MyProfileActivity.class);
                                        startActivity(intent);
                                        return;
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
