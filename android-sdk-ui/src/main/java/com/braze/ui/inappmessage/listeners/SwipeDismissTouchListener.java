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
import android.content.res.Resources;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

/**
 * A {@link android.view.View.OnTouchListener} that makes any {@link android.view.View} dismissable when the
 * user swipes (drags her finger) horizontally across the view. Optionally supports vertical
 * swipe dismissal in a single configured direction (up or down).
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

  /**
   * Specifies which vertical swipe direction, if any, should trigger a dismiss.
   */
  public enum VerticalDismissDirection {
    /** No vertical swipe dismissal. */
    NONE,
    /** Dismiss when the user swipes upward. */
    UP,
    /** Dismiss when the user swipes downward. */
    DOWN
  }

  // Cached ViewConfiguration and system-wide constant values
  private final int mSlop;
  private final int mMinFlingVelocity;
  private final int mMaxFlingVelocity;
  private final long mAnimationTime;

  // Fixed properties
  private final View mView;
  private final DismissCallbacks mCallbacks;
  private final VerticalDismissDirection mVerticalDismissDirection;
  private int mViewWidth = 1; // 1 and not 0 to prevent dividing by zero
  private int mViewHeight = 1;

  // Transient properties — horizontal
  private float mDownX;
  private float mDownY;
  private boolean mSwiping;
  private int mSwipingSlop;
  private final Object mToken;
  private VelocityTracker mVelocityTracker;
  private float mTranslationX;

  // Transient properties — vertical
  private boolean mSwipingVertically;
  private int mSwipingVerticalSlop;
  private float mTranslationY;

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
   * Constructs a new swipe-to-dismiss touch listener for the given view with horizontal-only dismissal.
   *
   * @param view      The view to make dismissable.
   * @param token     An optional token/cookie object to be passed through to the callback.
   * @param callbacks The callback to trigger when the user has indicated that she would like to
   *                  dismiss this view.
   */
  public SwipeDismissTouchListener(View view, Object token, DismissCallbacks callbacks) {
    this(view, token, callbacks, VerticalDismissDirection.NONE);
  }

  /**
   * Constructs a new swipe-to-dismiss touch listener for the given view.
   *
   * @param view                     The view to make dismissable.
   * @param token                    An optional token/cookie object to be passed through to the callback.
   * @param callbacks                The callback to trigger when the user has indicated that she would like to
   *                                 dismiss this view.
   * @param verticalDismissDirection The vertical direction that should also trigger a dismiss, or
   *                                 {@link VerticalDismissDirection#NONE} for horizontal-only.
   */
  public SwipeDismissTouchListener(View view, Object token, DismissCallbacks callbacks,
                                   VerticalDismissDirection verticalDismissDirection) {
    long animationTime;
    ViewConfiguration vc = ViewConfiguration.get(view.getContext());
    mSlop = vc.getScaledTouchSlop();
    mMinFlingVelocity = vc.getScaledMinimumFlingVelocity() * 16;
    mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
    try {
      final Resources resources = view.getContext().getResources();
      animationTime = resources.getInteger(android.R.integer.config_shortAnimTime);
    } catch (Resources.NotFoundException e) {
      // Defaulted to https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/core/res/res/values/config.xml#145
      animationTime = 200;
    }

    mAnimationTime = animationTime;
    mView = view;
    mToken = token;
    mCallbacks = callbacks;
    mVerticalDismissDirection = verticalDismissDirection;
  }

  @Override
  @SuppressWarnings({"checkstyle:operatorwrap", "checkstyle:cyclomaticcomplexity"})
  public boolean onTouch(View view, MotionEvent motionEvent) {
    // Offset because the view is translated during swipe
    motionEvent.offsetLocation(mTranslationX, mTranslationY);

    if (mViewWidth < 2) {
      mViewWidth = mView.getWidth();
    }
    if (mViewHeight < 2) {
      mViewHeight = mView.getHeight();
    }

    switch (motionEvent.getActionMasked()) {
      case MotionEvent.ACTION_DOWN: {
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

        mVelocityTracker.addMovement(motionEvent);
        mVelocityTracker.computeCurrentVelocity(1000);

        if (mSwipingVertically) {
          handleVerticalActionUp(motionEvent);
        } else {
          handleHorizontalActionUp(motionEvent);
        }

        mVelocityTracker.recycle();
        mVelocityTracker = null;
        resetTransientState();
        break;
      }

      case MotionEvent.ACTION_CANCEL: {
        if (mVelocityTracker == null) {
          break;
        }

        mView.animate()
            .translationX(0)
            .translationY(0)
            .alpha(1)
            .setDuration(mAnimationTime)
            .setListener(null);
        mVelocityTracker.recycle();
        mVelocityTracker = null;
        resetTransientState();
        break;
      }

      case MotionEvent.ACTION_MOVE: {
        if (mVelocityTracker == null) {
          break;
        }

        mVelocityTracker.addMovement(motionEvent);
        float deltaX = motionEvent.getRawX() - mDownX;
        float deltaY = motionEvent.getRawY() - mDownY;

        if (!mSwiping && !mSwipingVertically) {
          if (Math.abs(deltaX) > mSlop && Math.abs(deltaY) < Math.abs(deltaX) / 2) {
            mSwiping = true;
            mSwipingSlop = (deltaX > 0 ? mSlop : -mSlop);
            requestDisallowParentIntercept(motionEvent);
          } else if (mVerticalDismissDirection != VerticalDismissDirection.NONE
              && Math.abs(deltaY) > mSlop
              && Math.abs(deltaX) < Math.abs(deltaY) / 2
              && isVerticalSwipeInAllowedDirection(deltaY)) {
            mSwipingVertically = true;
            mSwipingVerticalSlop = (deltaY > 0 ? mSlop : -mSlop);
            requestDisallowParentIntercept(motionEvent);
          }
        }

        if (mSwiping) {
          mTranslationX = deltaX;
          mView.setTranslationX(deltaX - mSwipingSlop);
          return true;
        }
        if (mSwipingVertically) {
          mTranslationY = deltaY;
          mView.setTranslationY(deltaY - mSwipingVerticalSlop);
          return true;
        }
        break;
      }
    }
    return false;
  }

  private boolean isVerticalSwipeInAllowedDirection(float deltaY) {
    if (mVerticalDismissDirection == VerticalDismissDirection.DOWN) {
      return deltaY > 0;
    }
    if (mVerticalDismissDirection == VerticalDismissDirection.UP) {
      return deltaY < 0;
    }
    return false;
  }

  private void handleHorizontalActionUp(MotionEvent motionEvent) {
    float deltaX = motionEvent.getRawX() - mDownX;
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
      dismiss = (velocityX < 0) == (deltaX < 0);
      dismissRight = velocityX > 0;
    }
    if (dismiss) {
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
      mView.animate()
          .translationX(0)
          .alpha(1)
          .setDuration(mAnimationTime)
          .setListener(null);
    }
  }

  private void handleVerticalActionUp(MotionEvent motionEvent) {
    float deltaY = motionEvent.getRawY() - mDownY;
    float velocityY = mVelocityTracker.getYVelocity();
    float absVelocityY = Math.abs(velocityY);
    float absVelocityX = Math.abs(mVelocityTracker.getXVelocity());
    boolean dismiss = false;
    boolean dismissDown = false;
    if (Math.abs(deltaY) > mViewHeight / 2) {
      dismiss = true;
      dismissDown = deltaY > 0;
    } else if (mMinFlingVelocity <= absVelocityY && absVelocityY <= mMaxFlingVelocity
        && absVelocityX < absVelocityY) {
      dismiss = (velocityY < 0) == (deltaY < 0);
      dismissDown = velocityY > 0;
    }

    if (dismiss && isVerticalSwipeInAllowedDirection(dismissDown ? 1 : -1)) {
      mView.animate()
          .translationY(dismissDown ? mViewHeight : -mViewHeight)
          .alpha(0)
          .setDuration(mAnimationTime)
          .setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
              performDismiss();
            }
          });
    } else {
      mView.animate()
          .translationY(0)
          .alpha(1)
          .setDuration(mAnimationTime)
          .setListener(null);
    }
  }

  private void requestDisallowParentIntercept(MotionEvent motionEvent) {
    mView.getParent().requestDisallowInterceptTouchEvent(true);
    MotionEvent cancelEvent = MotionEvent.obtain(motionEvent);
    cancelEvent.setAction(MotionEvent.ACTION_CANCEL
        | (motionEvent.getActionIndex() << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
    mView.onTouchEvent(cancelEvent);
    cancelEvent.recycle();
  }

  private void resetTransientState() {
    mTranslationX = 0;
    mTranslationY = 0;
    mDownX = 0;
    mDownY = 0;
    mSwiping = false;
    mSwipingVertically = false;
  }

  /**
   * Animates the dismissed view to zero-height and then fires the dismiss callback.
   */
  public void performDismiss() {
    final ViewGroup.LayoutParams lp = mView.getLayoutParams();
    final int originalHeight = mView.getHeight();

    ValueAnimator animator = ValueAnimator.ofInt(originalHeight, 1).setDuration(mAnimationTime);

    animator.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        mCallbacks.onDismiss(mView, mToken);
        mView.setAlpha(1f);
        mView.setTranslationX(0);
        mView.setTranslationY(0);
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
