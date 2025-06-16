#  LookNote
![image](https://github.com/user-attachments/assets/a1ae6994-1dc7-4cd6-8c18-d8774bfa407a)

저는 평소 패션에 관심이 많아 자연스럽게 매일 어떤 옷을 입었는지 기록하는 습관이 있었습니다. 하지만 기존에는 메모장에 사진만 누끼를 따서 기록하거나 인스타그램 하이라이트에 업로드 하기만할 뿐 시간이 지나면 어떤 옷을 입었고, 어떤 장소에 다녀왔는지 떠올리기 어려웠습니다.
그래서 제가 입은 코디, 방문한 장소, 느꼈던 감정 등을 일기 형식으로 기록하고 예쁘게 꾸며볼 수 있는 앱이 있으면 좋겠다고 생각하여 개발하게 되었습니다.

## 🛠 기술 스택
- **Language**: Kotlin
- **IDE**: Android Studio
- **Room DB** 사용
- **Kizitonwose CalendarView**를 사용해 달력 UI 커스텀
- `PretendardVariable.ttf` 폰트 사용
- **OpenWeather API** 사용 

## 📌 주요 기능
- 날짜별 코디 기록 및 조회
- 사진 + 메모 + 착장 정보 저장
- 위치 기반 날씨 정보 표시
- 사용자 경험을 고려한 커스텀 캘린더 UI

## 🖼️ 프로토타입
![image](https://github.com/user-attachments/assets/e2689015-3551-49c0-8194-9ec38b2a981b)


## 📱 실행 화면
![image](https://github.com/user-attachments/assets/79c0fa30-e1e5-41a0-95e0-d0531450954f)
![image](https://github.com/user-attachments/assets/2a31f2da-aad4-46dd-ac64-07c2ea8ab4a8)


## ⚽트러블슈팅
### 1. Room DB 데이터 변경이 화면에 반영되지 않음

#### 문제

- 데이터를 삽입하거나 삭제한 후에도 UI가 즉시 갱신되지 않음.

#### 원인

- `LiveData`나 `Flow`를 사용하지 않고 단순 `suspend` 함수만 사용했기 때문.
- 즉시 UI 갱신을 원할 경우 `Flow`를 observe해야 함.
- `getAllFlow()` 라는 메서드 추가

#### 해결

```
//@DAO
 @Query("SELECT * FROM look_note ORDER BY id DESC")
fun getAllFlow(): Flow<List<LookNoteEntity>>

lifecycleScope.launch {
    LookNoteDB.getInstance(context).lookNoteDao().getAllFlow().collect { notes ->
        adapter.submitList(notes)
    }
}

```

---

### 2. 글꼴 PretendardVariable.ttf의 weight가 적용되지 않음

#### 문제

- `TextView`에서 `android:fontWeight="700"` 등을 사용해도 weight가 적용되지 않음.

#### 원인

- `fontFamily="@font/pretendard_variable"` 사용 시 `TextAppearance`와 연동이 잘 안 되는 경우 발생.

#### 해결

```
android:textFontWeight="200" // 이렇게 사용하기
```

---

### 3. 새롭게 만든 코틀린 파일이 적용되지 않음

#### 문제

- 수정 버튼을 눌러도 만든 페이지로 이동하지 않음.

#### 원인

- AndroidManifest에 등록이 안 되어 있음.

#### 해결

```
<activity android:name=".EditLookActivity" />
```

---

### 4. 삭제/수정 이후에도 MainActivity에서 변경사항이 보이지 않음

#### 원인

- DB 변경 후 새로 데이터를 로드하지 않음.
- Fragment나 Activity의 `onResume()`에 갱신 코드를 추가하지 않음.

#### 해결

```
override fun onResume() {
    super.onResume()
    loadData() // 이 안에서 DB를 다시 조회하고 RecyclerView 갱신
}

```

---

### 5. 위치 사용 문제

#### 문제

- 휴대폰의 기본 위치가 Google 본사(Mountain View)로 설정돼 있음

#### 해결

- 휴대폰 실행 실행
- 오른쪽 상단 ⋮ → `Location` 메뉴 선택
- 
![image](https://github.com/user-attachments/assets/e0b88fc8-3406-4ff5-82bc-6f78bd2758e0)

- 실행하는 기기의 Location을 한국으로 바꿔준 뒤 `Set Location` 버튼 클릭
