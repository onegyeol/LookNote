package com.example.looknote

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class AddLookActivity : AppCompatActivity() {

    // 이미지 미리보기, 업로드 버튼 등 뷰 선언
    private lateinit var imagePreview: ImageView
    private lateinit var buttonUpload: Button
    private lateinit var editTop: EditText
    private lateinit var editBottom: EditText
    private lateinit var editShoes: EditText
    private lateinit var editEtc: EditText
    private lateinit var editMemo: EditText
    private lateinit var buttonSave: Button

    // 선택한 이미지의 Uri와 날짜 저장 변수
    private var imageUri: Uri? = null
    private lateinit var selectedDate: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_look)

        // 인텐트로 날짜 받기 (없으면 오늘 날짜로 설정)
        selectedDate = intent.getStringExtra("selected_date") ?: java.time.LocalDate.now().toString()

        // 뷰 연결
        imagePreview = findViewById(R.id.imagePreview)
        buttonUpload = findViewById(R.id.buttonUpload)
        editTop = findViewById(R.id.editTop)
        editBottom = findViewById(R.id.editBottom)
        editShoes = findViewById(R.id.editShoes)
        editEtc = findViewById(R.id.editEtc)
        editMemo = findViewById(R.id.editMemo)
        buttonSave = findViewById(R.id.buttonSave)

        // 이미지 업로드 버튼 클릭 시 갤러리에서 이미지 선택
        buttonUpload.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 1001) // 1001은 결과 구분용 코드
        }

        // 저장 버튼 클릭 시 입력값을 LookNoteEntity에 담아서 DB에 저장
        buttonSave.setOnClickListener {
            val note = LookNoteEntity(
                date = selectedDate,
                imageUri = imageUri?.toString(),
                top = editTop.text.toString(),
                bottom = editBottom.text.toString(),
                shoes = editShoes.text.toString(),
                etc = editEtc.text.toString(),
                memo = editMemo.text.toString()
            )

            // 코루틴으로 Room DB에 비동기로 저장
            lifecycleScope.launch {
                val db = LookNoteDB.getInstance(this@AddLookActivity)
                db.lookNoteDao().insert(note)
                Toast.makeText(this@AddLookActivity, "저장 완료!", Toast.LENGTH_SHORT).show()
                finish() // 저장 후 액티비티 종료
            }
        }
    }

    // 이미지 선택 결과 처리 함수
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // 이미지 선택이 정상적으로 완료됐을 때
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK) {
            val selectedImageUri = data?.data ?: return // 선택한 이미지의 Uri

            // 이미지를 앱 내부 저장소로 복사
            val inputStream = contentResolver.openInputStream(selectedImageUri)
            val file = File(filesDir, "selected_image_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()

            // FileProvider를 통해 content:// 형태의 Uri 얻기 (보안상 필요)
            val contentUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )

            // DB에는 contentUri.toString()을 저장
            imageUri = contentUri
            imagePreview.setImageURI(contentUri) // 이미지뷰에 미리보기로 보여주기
        }
    }
}
