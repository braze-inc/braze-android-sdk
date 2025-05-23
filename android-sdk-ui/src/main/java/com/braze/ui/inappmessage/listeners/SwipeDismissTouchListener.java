package com.braze.ui.inappmessage.listeners;

/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

/**
 * A {@link android.view.View.OnTouchListener} that makes any {@link android.view.View} dismissable when the
 * user swipes (drags her finger) horizontally across the view.
 *
 * <p><em>For {@link android.widget.ListView} list items that don't manage their own touch events
 * (i.e. you're using
 * {@link android.widget.ListView#setOnItemClickListener(android.widget.AdapterView.OnItemClickListener)}
 * or an equivalent listener on {@link android.app.ListActivity} or
 * {@link android.app.ListFragment}, use {@link SwipeDismissListViewTouchListener} instead.</em></p>
 *
 * <p>Example usage:</p>
 *
 * <pre>
 * view.setOnTouchListener(new SwipeDismissTouchListener(
 *         view,
 *         null, // Optional token/cookie object
 *         new SwipeDismissTouchListener.OnDismissCallback() {
 *             public void onDismiss(View view, Object token) {
 *                 parent.removeView(view);
 *             }
 *         }));
 * </pre>
 *
 * <p>This class Requires API level 12 or later due to use of {@link
 * android.view.ViewPropertyAnimator}.</p>
 *
 * @see SwipeDismissListViewTouchListener
 */
@SuppressWarnings("checkstyle:missingswitchdefault")
public class SwipeDismissTouchListener implements View.OnTouchListener {
  // Cached ViewConfiguration and system-wide constant values
  private final int mSlop;
  private final int mMinFlingVelocity;
  private final int mMaxFlingVelocity;
  private final long mAnimationTime;

  // Fixed properties
  private final View mView;
  private final DismissCallbacks mCallbacks;
  private int mViewWidth = 1; // 1 and not 0 to prevent dividing by zero

  // Transient properties
  private float mDownX;
  private float mDownY;
  private boolean mSwiping;
  private int mSwipingSlop;
  private final Object mToken;
  private VelocityTracker mVelocityTracker;
  private float mTranslationX;

  /**
   * The callback interface used by {@link SwipeDismissTouchListener} to inform its client
   * about a successful dismissal of the view for which it was created.
   */
  public interface DismissCallbacks {
    /**
     * Called to determine whether the view can be dismissed.
     */
    boolean canDismiss(Object token);

    /**
     * Called when the user has indicated they she would like to dismiss the view.
     *
     * @param view The originating {@link View} to be dismissed.
     * @param token The optional token passed to this object's constructor.
     */
    void onDismiss(View view, Object token);
  }

  /**
   * Constructs a new swipe-to-dismiss touch listener for the given view.
   *
   * @param view     The view to make dismissable.
   * @param token    An optional token/cookie object to be passed through to the callback.
   * @param callbacks The callback to trigger when the user has indicated that she would like to
   *                 dismiss this view.
   */
  public SwipeDismissTouchListener(View view, Object token, DismissCallbacks callbacks) {
    ViewConfiguration vc = ViewConfiguration.get(view.getContext());
    mSlop = vc.getScaledTouchSlop();
    mMinFlingVelocity = vc.getScaledMinimumFlingVelocity() * 16;
    mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
    mAnimationTime = view.getContext().getResources().getInteger(
        android.R.integer.config_shortAnimTime);
    mView = view;
    mToken = token;
    mCallbacks = callbacks;
  }

  @Override
  @SuppressWarnings("checkstyle:operatorwrap")
  public boolean onTouch(View view, MotionEvent motionEvent) {
    // offset because the view is translated during swipe
    motionEvent.offsetLocation(mTranslationX, 0);

    if (mViewWidth < 2) {
      mViewWidth = mView.getWidth();
    }

    switch (motionEvent.getActionMasked()) {
      case MotionEvent.ACTION_DOWN: {
        // TODO: ensure this is a finger, and set a flag
        mDownX = motionEvent.getRawX();
        mDownY = motionEvent.getRawY();
        if (mCallbacks.canDismiss(mToken)) {
          mVelocityTracker = VelocityTracker.obtain();
          mVelocityTracker.addMovement(motionEvent);
        }
        return false;
      }

      case MotionEvent.ACTION_UP: {
        if (mVelocityTracker == null) {
          break;
        }

        float deltaX = motionEvent.getRawX() - mDownX;
        mVelocityTracker.addMovement(motionEvent);
        mVelocityTracker.computeCurrentVelocity(1000);
        float velocityX = mVelocityTracker.getXVelocity();
        float absVelocityX = Math.abs(velocityX);
        float absVelocityY = Math.abs(mVelocityTracker.getYVelocity());
        boolean dismiss = false;
        boolean dismissRight = false;
        if (Math.abs(deltaX) > mViewWidth / 2 && mSwiping) {
          dismiss = true;
          dismissRight = deltaX > 0;
        } else if (mMinFlingVelocity <= absVelocityX && absVelocityX <= mMaxFlingVelocity
            && absVelocityY < absVelocityX && mSwiping) {
          // dismiss only if flinging in the same direction as dragging
          dismiss = (velocityX < 0) == (deltaX < 0);
          dismissRight = mVelocityTracker.getXVelocity() > 0;
        }
        if (dismiss) {
          // dismiss
          mView.animate()
              .translationX(dismissRight ? mViewWidth : -mViewWidth)
              .alpha(0)
              .setDuration(mAnimationTime)
              .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                  performDismiss();
                }
              });
        } else if (mSwiping) {
          // cancel
          mView.animate()
              .translationX(0)
              .alpha(1)
              .setDuration(mAnimationTime)
              .setListener(null);
        }
        mVelocityTracker.recycle();
        mVelocityTracker = null;
        mTranslationX = 0;
        mDownX = 0;
        mDownY = 0;
        mSwiping = false;
        break;
      }

      case MotionEvent.ACTION_CANCEL: {
        if (mVelocityTracker == null) {
          break;
        }

        mView.animate()
            .translationX(0)
            .alpha(1)
            .setDuration(mAnimationTime)
            .setListener(null);
        mVelocityTracker.recycle();
        mVelocityTracker = null;
        mTranslationX = 0;
        mDownX = 0;
        mDownY = 0;
        mSwiping = false;
        break;
      }

      case MotionEvent.ACTION_MOVE: {
        if (mVelocityTracker == null) {
          break;
        }

        mVelocityTracker.addMovement(motionEvent);
        float deltaX = motionEvent.getRawX() - mDownX;
        float deltaY = motionEvent.getRawY() - mDownY;
        if (Math.abs(deltaX) > mSlop && Math.abs(deltaY) < Math.abs(deltaX) / 2) {
          mSwiping = true;
          mSwipingSlop = (deltaX > 0 ? mSlop : -mSlop);
          mView.getParent().requestDisallowInterceptTouchEvent(true);

          // Cancel listview's touch
          MotionEvent cancelEvent = MotionEvent.obtain(motionEvent);
          cancelEvent.setAction(MotionEvent.ACTION_CANCEL |
              (motionEvent.getActionIndex() <<
                  MotionEvent.ACTION_POINTER_INDEX_SHIFT));
          mView.onTouchEvent(cancelEvent);
          cancelEvent.recycle();
        }

        if (mSwiping) {
          mTranslationX = deltaX;
          mView.setTranslationX(deltaX - mSwipingSlop);
          // TODO: use an ease-out interpolator or such
//                    mView.setAlpha(Math.max(0f, Math.min(1f,
//                            1f - 2f * Math.abs(deltaX) / mViewWidth)));
          return true;
        }
        break;
      }
    }
    return false;
  }

  public void performDismiss() {
    // Animate the dismissed view to zero-height and then fire the dismiss callback.
    // This triggers layout on each animation frame; in the future we may want to do something
    // smarter and more performant.

    final ViewGroup.LayoutParams lp = mView.getLayoutParams();
    final int originalHeight = mView.getHeight();

    ValueAnimator animator = ValueAnimator.ofInt(originalHeight, 1).setDuration(mAnimationTime);

    animator.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        mCallbacks.onDismiss(mView, mToken);
        // Reset view presentation
        mView.setAlpha(1f);
        mView.setTranslationX(0);
        lp.height = originalHeight;
        mView.setLayoutParams(lp);
      }
    });

    animator.addUpdateListener(valueAnimator -> {
      lp.height = (Integer) valueAnimator.getAnimatedValue();
      mView.setLayoutParams(lp);
    });

    animator.start();
  }
}
