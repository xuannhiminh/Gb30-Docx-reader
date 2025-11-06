package documents.office.docx.reader.viewer.editor.screen.start

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import documents.office.docx.reader.viewer.editor.databinding.ActivityFullScreenBinding
import documents.office.docx.reader.viewer.editor.notification.NotificationManager.Companion.CALL_USE_APP_NOTIFICATION_OUT_ID
import documents.office.docx.reader.viewer.editor.screen.base.PdfBaseActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FullScreenActivity : PdfBaseActivity<ActivityFullScreenBinding>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable screen to turn on and show when locked
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        // Keep screen on while activity is visible
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun initView() {
        val currentDate = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        binding.tvDate.text = currentDate
        binding.tvTime.text = currentTime
    }

    override fun initData() {

    }

    override fun initListener() {
        binding.cardDialog.setOnClickListener {
            val intent = Intent(this@FullScreenActivity, SplashActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("${packageName}.notificationID", CALL_USE_APP_NOTIFICATION_OUT_ID)
                putExtra("${packageName}.isFromNotification", true)
            }
            startActivity(intent)
            finish()
        }
        binding.btnClose.setOnClickListener {
            finish()
        }
    }

    override fun viewBinding(): ActivityFullScreenBinding {
        return ActivityFullScreenBinding.inflate(LayoutInflater.from(this))
    }
}
