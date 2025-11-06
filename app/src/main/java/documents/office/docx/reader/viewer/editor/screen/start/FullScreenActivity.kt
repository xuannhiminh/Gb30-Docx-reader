package documents.office.docx.reader.viewer.editor.screen.start

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import documents.office.docx.reader.viewer.editor.databinding.ActivityFullScreenBinding
import documents.office.docx.reader.viewer.editor.notification.NotificationManager.Companion.DAILY_FULL_SCREEN_NOTIFICATION_ID
import documents.office.docx.reader.viewer.editor.screen.base.PdfBaseActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FullScreenActivity : PdfBaseActivity<ActivityFullScreenBinding>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Bật hiển thị khi bị khoá màn hình & bật màn hình
        setShowWhenLocked(true)
        setTurnScreenOn(true)
    }

    override fun viewBinding(): ActivityFullScreenBinding {
        return ActivityFullScreenBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        val currentDate = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        binding.tvDate.text = currentDate
        binding.tvTime.text = currentTime
    }

    override fun initListener() {
        binding.cardDialog.setOnClickListener {
            handleCardDialogClick()
        }
        binding.btnClose.setOnClickListener {
            finish()
        }
    }

    private fun handleCardDialogClick() {
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && keyguardManager.isKeyguardLocked) {
            // Thiết bị đang khoá -> yêu cầu mở khoá
            keyguardManager.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() {
                    super.onDismissSucceeded()
                    // Mở khoá thành công -> chuyển tới SplashActivity
                    goToSplashFromNotification()
                }

                override fun onDismissError() {
                    super.onDismissError()
                    // Có lỗi khi mở khoá -> bạn có thể thông báo hoặc fallback
                    goToSplashFromNotification()
                }

                override fun onDismissCancelled() {
                    super.onDismissCancelled()
                    // Người dùng huỷ mở khoá -> bạn có thể xử lý riêng
                }
            })
        } else {
            // Thiết bị không khoá hoặc API < O -> chuyển thẳng
            goToSplashFromNotification()
        }
    }

    private fun goToSplashFromNotification() {
        val intent = Intent(this@FullScreenActivity, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("${packageName}.notificationID", DAILY_FULL_SCREEN_NOTIFICATION_ID)
            putExtra("${packageName}.isFromNotification", true)
        }
        startActivity(intent)
        finish()
    }

    override fun initData() {
        // nếu có dữ liệu nào cần khởi tạo thì thêm vào đây
    }
}