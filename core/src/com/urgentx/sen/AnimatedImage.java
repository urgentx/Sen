package com.urgentx.sen;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

/**
 * Created by Barco on 17-Sep-16.
 */
public class AnimatedImage extends Image {
    protected Animation animation = null;
    private float stateTime = 0;

    public AnimatedImage(Animation animation){
        super(animation.getKeyFrame(0));
        this.animation = animation;
    }

    @Override
    public void act(float delta){
        ((TextureRegionDrawable)getDrawable()).setRegion(animation.getKeyFrame(stateTime+=delta, true));
        super.act(delta);
    }

}
