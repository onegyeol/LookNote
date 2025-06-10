package com.example.looknote

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.ViewContainer
import com.kizitonwose.calendar.view.MonthDayBinder
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

class MainActivity : AppCompatActivity() {

    // 위치 정보 가져올 때 사용하는 변수
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 100

    // 날짜별로 사진 uri 저장하는 맵
    private val noteImageMap = mutableMapOf<LocalDate, String?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // 위치 서비스 초기화
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 캘린더 뷰 관련 변수들
        val calendarView = findViewById<CalendarView>(R.id.calendarView)
        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(12)
        val endMonth = currentMonth.plusMonths(12)

        // 캘린더 범위 설정 (1년 전 ~ 1년 후)
        calendarView.setup(startMonth, endMonth, DayOfWeek.SUNDAY)
        calendarView.scrollToMonth(currentMonth)

        // 월, 년 표시하는 텍스트뷰
        val monthText = findViewById<TextView>(R.id.monthLabel)
        val yearText = findViewById<TextView>(R.id.yearLabel)

        // 캘린더 월이 바뀔 때마다 월, 년도 텍스트 바꿔줌
        calendarView.monthScrollListener = { month ->
            val monthName = month.yearMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }
            monthText.text = monthName
            yearText.text = month.yearMonth.year.toString()
        }

        // 오늘 날짜 가져오기
        val today = LocalDate.now()

        // 날짜 하나하나에 대한 뷰 바인딩
        calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View): DayViewContainer = DayViewContainer(view)

            override fun bind(container: DayViewContainer, day: CalendarDay) {
                container.textView.text = day.date.dayOfMonth.toString()

                // 오늘 날짜면 배경색 다르게 표시
                if (day.date == today) {
                    container.textView.setBackgroundResource(R.drawable.today)
                    container.textView.setTextColor(getColor(R.color.white))
                } else {
                    container.textView.background = null
                    container.textView.setTextColor(getColor(R.color.black))
                }

                // 해당 날짜에 사진 있으면 이미지 보여주기
                val imageView = container.view.findViewById<ImageView>(R.id.dayImage)
                val imageUri = noteImageMap[day.date]

                if (imageUri != null) {
                    imageView.visibility = View.VISIBLE
                    Glide.with(imageView.context)
                        .load(Uri.parse(imageUri))
                        .into(imageView)
                } else {
                    imageView.visibility = View.GONE
                }

                // 날짜 클릭하면
                container.textView.setOnClickListener {
                    val selectedDate = day.date.toString()

                    // DB에서 해당 날짜 데이터 있는지 확인
                    lifecycleScope.launch {
                        val db = LookNoteDB.getInstance(this@MainActivity)
                        val existingNote = db.lookNoteDao().getByDate(selectedDate)

                        if (existingNote != null) {
                            // 이미 데이터 있으면 상세 페이지로 이동
                            val intent = Intent(this@MainActivity, DetailLookActivity::class.java).apply {
                                putExtra("date", selectedDate)
                                putExtra("imageUri", existingNote.imageUri)
                                putExtra("top", existingNote.top)
                                putExtra("bottom", existingNote.bottom)
                                putExtra("shoes", existingNote.shoes)
                                putExtra("etc", existingNote.etc)
                                putExtra("memo", existingNote.memo)
                            }
                            startActivity(intent)
                        } else {
                            // 없으면 새로 입력하는 화면으로 이동
                            val intent = Intent(this@MainActivity, AddLookActivity::class.java)
                            intent.putExtra("selected_date", selectedDate)
                            startActivity(intent)
                        }
                    }
                }
            }
        }

        // 저장된 노트 불러와서 캘린더에 반영
        loadSavedNotes(calendarView)

        // 오늘 날짜 텍스트뷰에 표시
        val todayDateText = findViewById<TextView>(R.id.todayDate)
        val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
        todayDateText.text = "날짜 : ${dateFormat.format(Date())}\n"

        // 날씨 정보 요청 (위치 권한 체크)
        checkLocationPermission()

        // 화면에 시스템 바(상단, 하단) 패딩 적용
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // DB에 저장된 전체 노트 로그로 확인 (디버깅용)
        lifecycleScope.launch {
            val notes = LookNoteDB.getInstance(this@MainActivity).lookNoteDao().getAll()
            notes.forEach {
                Log.d("DB확인", "날짜=${it.date}, 이미지=${it.imageUri} 상의=${it.top}, 하의=${it.bottom}, 신발=${it.shoes}, 기타=${it.etc}, 메모=${it.memo}")
            }
        }
    }

    // 저장된 사진들 캘린더에 반영하는 함수
    private fun loadSavedNotes(calendarView: CalendarView) {
        val db = LookNoteDB.getInstance(this)
        lifecycleScope.launch {
            db.lookNoteDao().getAllFlow().collect { notes ->
                noteImageMap.clear()
                notes.forEach { note ->
                    val date = LocalDate.parse(note.date)
                    noteImageMap[date] = note.imageUri
                }
                calendarView.notifyCalendarChanged()
            }
        }
    }

    // 위치 권한 체크 함수
    private fun checkLocationPermission() {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            fetchLocationAndWeather()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(permission), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    // 위치 받아와서 날씨 정보 가져오는 함수
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun fetchLocationAndWeather() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                fetchWeatherData(it.latitude, it.longitude)
            } ?: run {
                Toast.makeText(this, "위치 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 날씨 API 요청해서 화면에 보여주는 함수
    private fun fetchWeatherData(lat: Double, lon: Double) {
        val apiKey = "api" // 발급받은 api 키 (openweather)
        val url = "https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&appid=$apiKey&units=metric&lang=kr"

        val request = Request.Builder().url(url).build()
        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "날씨 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: return
                println("날씨 응답: $body")

                if (!response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "날씨 API 오류: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                    return
                }

                try {
                    val json = JSONObject(body)

                    val weatherDescription = json.getJSONArray("weather")
                        .getJSONObject(0)
                        .getString("description")

                    val main = json.getJSONObject("main")
                    val temp = main.getDouble("temp")
                    val tempMin = main.getDouble("temp_min")
                    val tempMax = main.getDouble("temp_max")

                    runOnUiThread {
                        val weatherText = findViewById<TextView>(R.id.todayWeather)
                        weatherText.text =
                            "날씨 : $weatherDescription\n\n" +
                                    "현재 기온 : ${"%.1f".format(temp)}°C\n" +
                                    "최저 기온 : ${"%.1f".format(tempMin)}°C / 최고 기온 : ${"%.1f".format(tempMax)}°C"
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "날씨 데이터를 파싱하는 중 오류 발생", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    // 위치 권한 요청 결과 처리
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchLocationAndWeather()
        } else {
            Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // 날짜 하나에 해당하는 뷰 컨테이너 (캘린더용)
    inner class DayViewContainer(view: View) : ViewContainer(view) {
        val textView: TextView = view.findViewById(R.id.dayText)
        val imageView: ImageView = view.findViewById(R.id.dayImage)
    }
}
