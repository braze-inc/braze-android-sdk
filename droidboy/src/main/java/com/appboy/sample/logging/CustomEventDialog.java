package com.appboy.sample.logging;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.appboy.sample.R;
import com.braze.Braze;
import com.braze.models.outgoing.BrazeProperties;

public class CustomEventDialog extends CustomLogger {

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.custom_event, container, false);
  }

  @Override
  protected void customLog(String name, BrazeProperties properties) {
    Braze.getInstance(getContext()).logCustomEvent(name, properties);
  }
}
