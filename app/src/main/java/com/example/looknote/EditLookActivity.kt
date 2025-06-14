package com.example.looknote

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class EditLookActivity : AppCompatActivity() {

    private var currentDate: String? = null
    private var currentImageUri: String? = null

    // 이미지 선택할 때 구분용 코드
    private val PICK_IMAGE_REQUEST = 2001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_look)

        val imageView = findViewById<ImageView>(R.id.imagePreview)
        val topEdit = findViewById<EditText>(R.id.editTop)
        val bottomEdit = findViewById<EditText>(R.id.editBottom)
        val shoesEdit = findViewById<EditText>(R.id.editShoes)
        val etcEdit = findViewById<EditText>(R.id.editEtc)
        val memoEdit = findViewById<EditText>(R.id.editMemo)
        val saveButton = findViewById<Button>(R.id.buttonSave)
        val imageEdit = findViewById<Button>(R.id.imageEdit)

        // 전달받은 데이터 불러오기
        currentDate = intent.getStringExtra("date")
        currentImageUri = intent.getStringExtra("imageUri")

        topEdit.setText(intent.getStringExtra("top"))
        bottomEdit.setText(intent.getStringExtra("bottom"))
        shoesEdit.setText(intent.getStringExtra("shoes"))
        etcEdit.setText(intent.getStringExtra("etc"))
        memoEdit.setText(intent.getStringExtra("memo"))

        // 기존 이미지 보여주기
        Glide.with(this)
            .load(Uri.parse(currentImageUri))
            .into(imageView)

        // 사진 변경 버튼 누르면 갤러리에서 이미지 선택
        imageEdit.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        // 업로드 버튼 클릭 시 데이터베이스에 반영
        saveButton.setOnClickListener {
            val top = topEdit.text.toString()
            val bottom = bottomEdit.text.toString()
            val shoes = shoesEdit.text.toString()
            val etc = etcEdit.text.toString()
            val memo = memoEdit.text.toString()

            Log.d("EditLookActivity", "저장값 → date=$currentDate, top=$top, bottom=$bottom, shoes=$shoes, etc=$etc, memo=$memo, imageUri=$currentImageUri")

            currentDate?.let { date ->
                lifecycleScope.launch {
                    LookNoteDB.getInstance(this@EditLookActivity)
                        .lookNoteDao()
                        .updateByDate(
                            date = date,
                            imageUri = currentImageUri ?: "",
                            top = top,
                            bottom = bottom,
                            shoes = shoes,
                            etc = etc,
                            memo = memo
                        )

                    Toast.makeText(this@EditLookActivity, "수정 완료!", Toast.LENGTH_SHORT).show()

                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
        }

    }

    // 사진 선택 결과 처리
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // 사진을 새로 선택했을 때
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            val selectedImageUri = data?.data ?: return

            // 이미지 앱 내부 저장소로 복사 (AddLookActivity 참고)
            val inputStream = contentResolver.openInputStream(selectedImageUri)
            val file = File(filesDir, "edited_image_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()

            // FileProvider로 content:// 형태의 Uri 얻기
            val contentUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )

            // 현재 이미지 Uri를 새로 선택한 걸로 바꿔줌
            currentImageUri = contentUri.toString()

            // 이미지뷰에 새 이미지 보여주기
            val imageView = findViewById<ImageView>(R.id.imagePreview)
            Glide.with(this)
                .load(contentUri)
                .into(imageView)
        }
    }
}
