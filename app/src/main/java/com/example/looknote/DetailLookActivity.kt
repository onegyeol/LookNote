package com.example.looknote

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch

class DetailLookActivity : AppCompatActivity() {

    private var currentDate: String? = null // 현재 날짜 저장

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_look)

        val textDate = findViewById<TextView>(R.id.textDate)
        val imageView = findViewById<ImageView>(R.id.imagePreview)
        val topText = findViewById<TextView>(R.id.textTop)
        val bottomText = findViewById<TextView>(R.id.textBottom)
        val shoesText = findViewById<TextView>(R.id.textShoes)
        val etcText = findViewById<TextView>(R.id.textEtc)
        val memoText = findViewById<TextView>(R.id.textMemo)
        val deleteButton = findViewById<Button>(R.id.buttonDelete)

        val date = intent.getStringExtra("date")
        val imageUri = intent.getStringExtra("imageUri")
        val top = intent.getStringExtra("top")
        val bottom = intent.getStringExtra("bottom")
        val shoes = intent.getStringExtra("shoes")
        val etc = intent.getStringExtra("etc")
        val memo = intent.getStringExtra("memo")

        currentDate = date
        textDate.text = date
        topText.text = "👕 상의: $top"
        bottomText.text = "👖 하의: $bottom"
        shoesText.text = "👟 신발: $shoes"
        etcText.text = "🧢 기타: $etc"
        memoText.text = memo

        Glide.with(this).load(Uri.parse(imageUri)).into(imageView)

        // 삭제 버튼 클릭 시 DB에서 삭제
        deleteButton.setOnClickListener {
            currentDate?.let { selectedDate ->
                lifecycleScope.launch {
                    val dao = LookNoteDB.getInstance(this@DetailLookActivity).lookNoteDao()
                    dao.deleteByDate(selectedDate)
                    Toast.makeText(this@DetailLookActivity, "삭제", Toast.LENGTH_SHORT).show()
                    finish() // 현재 화면 종료
                }
            }
        }

        findViewById<Button>(R.id.buttonEdit).setOnClickListener {
            val intent = Intent(this, EditLookActivity::class.java).apply {
                putExtra("date", date)
                putExtra("imageUri", imageUri)
                putExtra("top", top)
                putExtra("bottom", bottom)
                putExtra("shoes", shoes)
                putExtra("etc", etc)
                putExtra("memo", memo)
            }
            startActivity(intent)
        }


    }
}
