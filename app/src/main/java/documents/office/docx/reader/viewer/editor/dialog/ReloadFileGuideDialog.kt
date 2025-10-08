package documents.office.docx.reader.viewer.editor.dialog

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.view.*
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import documents.office.docx.reader.viewer.editor.databinding.GuideSpotlightBinding

class ReloadFileGuideDialog(
    context: Context,
    private val targetView: View
) : Dialog(context) {

    private var binding: GuideSpotlightBinding = GuideSpotlightBinding.inflate(LayoutInflater.from(context))
    private fun startSwipeDownAnimation(arrowView: ImageView) {
        val swipeDownAnim = TranslateAnimation(0f, 0f, 0f, 120f).apply {
            duration = 600
            repeatMode = TranslateAnimation.REVERSE
            repeatCount = TranslateAnimation.INFINITE
        }
        arrowView.startAnimation(swipeDownAnim)

    }
    private fun startSwipeDownCycleAnimation(arrowView: ImageView) {
        val distance = 120f * arrowView.resources.displayMetrics.density

        val translateDown = ObjectAnimator.ofFloat(arrowView, "translationY", 0f, distance).apply {
            duration = 600
        }

        val delay = ValueAnimator.ofFloat(0f, 0f).apply {
            duration = 400 // thời gian dừng
        }

        val fadeOut = ObjectAnimator.ofFloat(arrowView, "alpha", 1f, 0f).apply {
            duration = 400
        }

        val reset = Runnable {
            arrowView.translationY = 0f
            arrowView.alpha = 1f
            // gọi lại để lặp
            startSwipeDownCycleAnimation(arrowView)
        }

        val animatorSet = AnimatorSet()
        animatorSet.playSequentially(translateDown, delay, fadeOut)
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                arrowView.postDelayed(reset, 200) // delay trước khi bắt đầu lại
            }
        })

        animatorSet.start()
    }


    init {
        setContentView(binding.root)
        setCancelable(true)
        setCanceledOnTouchOutside(true)
        val insetsController = WindowCompat.getInsetsController(window!!, window!!.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.navigationBars())
        // Dialog full màn hình và trong suốt
        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        // dismiss when clicking outside
        binding.root.setOnClickListener { dismiss() }

        binding.btnGotIt.setOnClickListener {
            dismiss()
        }

        // Hiển thị sau khi targetView layout xong
        targetView.post {
            // Tính vùng đục lỗ quanh target
            binding.spotlightOverlay.setHoleAroundView(targetView, margin = 12)
            binding.spotlightOverlay.stopBlinkingAndClearHole()
            // Đặt mũi tên vào vị trí tương đối với targetView
            val loc = IntArray(2)
            targetView.getLocationInWindow(loc)
            val targetX = loc[0]
            val targetY = loc[1]

            val arrow = binding.arrowImage
            arrow.post {
                arrow.x = targetX + targetView.width / 2f - arrow.width / 2f
                arrow.y = targetY - arrow.height - 12f

                startSwipeDownCycleAnimation(arrow)
            }
        }
    }
}
