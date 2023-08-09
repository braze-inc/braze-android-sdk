package com.appboy.sample.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class FeedFragmentActivity extends AppCompatActivity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(com.braze.ui.R.layout.com_braze_feed_activity);
    setTitle("DroidGirl");
  }
}
