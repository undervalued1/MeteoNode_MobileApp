package com.example.meteonode.ui.fragments

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView

open class BaseFragment : Fragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Анимация появления для всех дочерних фрагментов
        view.postDelayed({
            animateViews()
        }, 100)
    }

    protected fun animateViews() {
        view?.let { rootView ->
            val cards = getAllCardViews(rootView as? ViewGroup ?: return)

            cards.forEachIndexed { index, cardView ->
                cardView.alpha = 0f
                cardView.translationY = 50f
                cardView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(400)
                    .setStartDelay((index * 100).toLong())
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }
        }
    }

    private fun getAllCardViews(viewGroup: ViewGroup): List<View> {
        val cards = mutableListOf<View>()
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is MaterialCardView) {
                cards.add(child)
            } else if (child is ViewGroup) {
                cards.addAll(getAllCardViews(child))
            }
        }
        return cards
    }

    protected fun animateClick(view: View) {
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }



    protected fun animatePulse(view: View) {
        view.animate()
            .scaleX(1.05f)
            .scaleY(1.05f)
            .setDuration(200)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start()
            }
            .start()
    }

    protected fun animateSlideInFromBottom(view: View, delay: Long = 0) {
        view.alpha = 0f
        view.translationY = 100f
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400)
            .setStartDelay(delay)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    protected fun animateSlideInFromLeft(view: View, delay: Long = 0) {
        view.alpha = 0f
        view.translationX = -100f
        view.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(400)
            .setStartDelay(delay)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    protected fun animateSlideInFromRight(view: View, delay: Long = 0) {
        view.alpha = 0f
        view.translationX = 100f
        view.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(400)
            .setStartDelay(delay)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }
}