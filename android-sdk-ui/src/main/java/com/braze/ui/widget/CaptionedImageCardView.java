package com.braze.ui.widget;

import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;

import com.braze.models.cards.CaptionedImageCard;
import com.braze.ui.R;
import com.braze.ui.feed.view.BaseFeedCardView;
import com.braze.support.BrazeLogger;
import com.braze.ui.actions.IAction;

public class CaptionedImageCardView extends BaseFeedCardView<CaptionedImageCard> {
  private static final String TAG = BrazeLogger.getBrazeLogTag(CaptionedImageCardView.class);
  private final ImageView mImage;
  private final TextView mTitle;
  private final TextView mDescription;
  private final TextView mDomain;
  private IAction mCardAction;

  // We set this card's aspect ratio here as a first guess. If the server doesn't send down an
  // aspect ratio, then this value will be the aspect ratio of the card on render.
  private float mAspectRatio = 4f / 3f;

  public CaptionedImageCardView(Context context) {
    this(context, null);
  }

  public CaptionedImageCardView(final Context context, CaptionedImageCard card) {
    super(context);
    mImage = (ImageView) getProperViewFromInflatedStub(R.id.com_braze_captioned_image_card_imageview_stub);
    mImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
    mImage.setAdjustViewBounds(true);

    mTitle = findViewById(R.id.com_braze_captioned_image_title);
    mDescription = findViewById(R.id.com_braze_captioned_image_description);
    mDomain = findViewById(R.id.com_braze_captioned_image_card_domain);

    if (card != null) {
      setCard(card);
    }

    setBackground(getResources().getDrawable(R.drawable.com_braze_card_background, null));
  }

  @Override
  protected int getLayoutResource() {
    return R.layout.com_braze_captioned_image_card;
  }

  @Override
  public void onSetCard(final CaptionedImageCard card) {
    mTitle.setText(card.getTitle());
    mDescription.setText(card.getDescription());
    setOptionalTextView(mDomain, card.getDomain());
    mCardAction = getUriActionForCard(card);
    setOnClickListener(view -> handleCardClick(applicationContext, card, mCardAction));
    mAspectRatio = card.getAspectRatio();
    setImageViewToUrl(mImage, card.getImageUrl(), mAspectRatio, card);
  }
}
