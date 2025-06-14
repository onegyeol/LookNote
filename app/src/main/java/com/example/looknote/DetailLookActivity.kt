package com.example.looknote

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch
class DetailLookActivity : AppCompatActivity() {

    private var currentDate: String? = null
    private lateinit var textDate: TextView
    private lateinit var imageView: ImageView
    private lateinit var topText: TextView
    private lateinit var bottomText: TextView
    private lateinit var shoesText: TextView
    private lateinit var etcText: TextView
    private lateinit var memoText: TextView

    companion object {
        private const val REQUEST_EDIT = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_look)

        textDate = findViewById(R.id.textDate)
        imageView = findViewById(R.id.imagePreview)
        topText = findViewById(R.id.textTop)
        bottomText = findViewById(R.id.textBottom)
        shoesText = findViewById(R.id.textShoes)
        etcText = findViewById(R.id.textEtc)
        memoText = findViewById(R.id.textMemo)
        val deleteButton = findViewById<Button>(R.id.buttonDelete)

        currentDate = intent.getStringExtra("date")

        loadNoteFromDB()

        deleteButton.setOnClickListener {
            currentDate?.let { selectedDate ->
                lifecycleScope.launch {
                    val dao = LookNoteDB.getInstance(this@DetailLookActivity).lookNoteDao()
                    dao.deleteByDate(selectedDate)
                    Toast.makeText(this@DetailLookActivity, "삭제", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        findViewById<Button>(R.id.buttonEdit).setOnClickListener {
            val intent = Intent(this, EditLookActivity::class.java).apply {
                // ✅ 여기서 수정
                putExtra("date", currentDate)
                putExtra("imageUri", intent.getStringExtra("imageUri"))
                putExtra("top", topText.text.toString().removePrefix("👕 상의: "))
                putExtra("bottom", bottomText.text.toString().removePrefix("👖 하의: "))
                putExtra("shoes", shoesText.text.toString().removePrefix("👟 신발: "))
                putExtra("etc", etcText.text.toString().removePrefix("🧢 기타: "))
                putExtra("memo", memoText.text.toString())
            }
            startActivityForResult(intent, REQUEST_EDIT)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_EDIT && resultCode == Activity.RESULT_OK) {
            loadNoteFromDB()  // 👉 여기서 DB에서 새로 불러옴
        }
    }

    private fun loadNoteFromDB() {
        currentDate?.let { date ->
            lifecycleScope.launch {
                LookNoteDB.getInstance(this@DetailLookActivity)
                    .lookNoteDao()
                    .getAllFlow()
                    .collect { noteList ->
                        val note = noteList.find { it.date == currentDate }
                        note?.let {
                            textDate.text = it.date
                            topText.text = "👕 상의: ${it.top}"
                            bottomText.text = "👖 하의: ${it.bottom}"
                            shoesText.text = "👟 신발: ${it.shoes}"
                            etcText.text = "🧢 기타: ${it.etc}"
                            memoText.text = it.memo

                            Glide.with(this@DetailLookActivity)
                                .load(Uri.parse(it.imageUri))
                                .skipMemoryCache(true) // 캐시 무시
                                .into(imageView)
                        }
                    }
            }

        }
    }
}
