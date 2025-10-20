package com.appboy.sample;

import android.app.Activity;
import android.view.View;
import android.widget.Toast;

import com.braze.models.inappmessage.IInAppMessage;
import com.braze.ui.inappmessage.InAppMessageOperation;
import com.braze.ui.inappmessage.listeners.IInAppMessageManagerListener;

import java.util.Map;

public class CustomInAppMessageManagerListener implements IInAppMessageManagerListener {
  private final Activity mActivity;

  public CustomInAppMessageManagerListener(Activity activity) {
    mActivity = activity;
  }

  private Boolean shouldDrop = true;

  @Override
  public InAppMessageOperation beforeInAppMessageDisplayed(IInAppMessage inAppMessage) {
    if (shouldDrop) {
      shouldDrop = false;
      return InAppMessageOperation.REENQUEUE;
    } else {
      shouldDrop = true;
      return InAppMessageOperation.DISPLAY_NOW;
    }
  }

  @Override
  public void onInAppMessageDismissed(IInAppMessage inAppMessage) {
    if (inAppMessage.getExtras() != null && !inAppMessage.getExtras().isEmpty()) {
      Map<String, String> extras = inAppMessage.getExtras();
      StringBuilder keyValuePairs = new StringBuilder("Dismissed in-app message with extras payload containing [");
      for (String key : extras.keySet()) {
        keyValuePairs.append(" '").append(key).append(" = ").append(extras.get(key)).append('\'');
      }
      keyValuePairs.append(']');
      Toast.makeText(mActivity, keyValuePairs.toString(), Toast.LENGTH_LONG).show();
    } else {
      Toast.makeText(mActivity, "The in-app message was dismissed.", Toast.LENGTH_LONG).show();
    }
  }

  @Override
  public void beforeInAppMessageViewOpened(View inAppMessageView, IInAppMessage inAppMessage) { }

  @Override
  public void afterInAppMessageViewOpened(View inAppMessageView, IInAppMessage inAppMessage) { }

  @Override
  public void beforeInAppMessageViewClosed(View inAppMessageView, IInAppMessage inAppMessage) { }

  @Override
  public void afterInAppMessageViewClosed(IInAppMessage inAppMessage) { }
}
