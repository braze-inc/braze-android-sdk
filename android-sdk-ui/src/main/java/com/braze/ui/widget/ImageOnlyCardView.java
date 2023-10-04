package com.braze.ui.widget;

import android.content.Context;
import android.widget.ImageView;

import com.braze.models.cards.ImageOnlyCard;
import com.braze.support.BrazeLogger;
import com.braze.ui.R;
import com.braze.ui.actions.IAction;
import com.braze.ui.feed.view.BaseFeedCardView;

public class ImageOnlyCardView extends BaseFeedCardView<ImageOnlyCard> {
  private static final String TAG = BrazeLogger.getBrazeLogTag(ImageOnlyCardView.class);
  private final ImageView mImage;
  private IAction mCardAction;

  // We set this card's aspect ratio here as a first guess. If the server doesn't send down an
  // aspect ratio, then this value will be the aspect ratio of the card on render.
  private float mAspectRatio = 6f;

  public ImageOnlyCardView(Context context) {
    this(context, null);
  }

  public ImageOnlyCardView(final Context context, ImageOnlyCard card) {
    super(context);
    mImage = (ImageView) getProperViewFromInflatedStub(R.id.com_braze_image_only_card_imageview_stub);
    mImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
    mImage.setAdjustViewBounds(true);

    if (card != null) {
      setCard(card);
    }

    setBackground(getResources().getDrawable(R.drawable.com_braze_card_background, null));
  }

  @Override
  protected int getLayoutResource() {
    return R.layout.com_braze_image_only_card;
  }

  @Override
  public void onSetCard(final ImageOnlyCard card) {
    if (card.getAspectRatio() != 0f) {
      mAspectRatio = card.getAspectRatio();
    }
    setImageViewToUrl(mImage, card.getImageUrl(), mAspectRatio, card);

    mCardAction = getUriActionForCard(card);
    setOnClickListener(view -> handleCardClick(applicationContext, card, mCardAction));
  }
}
