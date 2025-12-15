# 🎨 ONFIT - 스타일을 기록하고 연결하는 내 손안의 스마트 옷장

<div align="center">

![Onfit Logo](https://img.shields.io/badge/Onfit-Smart%20Closet-4169E1?style=for-the-badge)
[![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge)](LICENSE)

**한 장의 코디 사진으로 완성되는 스마트 옷장**

[프로젝트 소개](#-프로젝트-소개) • [주요 기능](#-주요-기능) • [기술 스택](#-기술-스택) • [팀원 소개](#-팀원-소개)

</div>

---

## 📖 프로젝트 소개

**Onfit**은 AI 기반의 스마트 옷장 애플리케이션으로, 사용자의 코디 사진을 통해 옷을 자동으로 분석하고 날씨와 취향에 맞는 추천을 제공합니다.

### 🎯 핵심 가치

> "한 장의 코디 사진만으로 당신의 스타일을 완성합니다"

- ✨ **간편한 등록**: 한 장의 코디 사진으로 여러 아이템 자동 인식
- 🤖 **AI 추천**: DALL-E 기반 맞춤형 스타일 생성
- 🌤️ **날씨 연동**: 실시간 날씨 정보 기반 코디 추천
- 👥 **커뮤니티**: 유사한 취향을 가진 사람들과의 연결

---

## ✨ 주요 기능

### 1️⃣ 한 장의 코디 사진으로 완성되는 스마트 옷장

<div align="center">

| 📸 사진 업로드 | 🤖 AI 분석 | 💾 자동 저장 |
|:---:|:---:|:---:|
| 전신 코디 사진 촬영 | YOLOv8 의류 자동 인식 | DALL-E 실사 스타일 생성 |

</div>

- **YOLOv8 의류 부위 인식**: 한 장의 사진에서 상의, 하의, 신발, 액세서리 자동 분류
- **자동 크롭**: 인식된 아이템을 개별적으로 크롭하여 옷장에 저장
- **DALL-E 기반 스타일 이미지 생성**: GPT를 활용한 태깅 및 고품질 스타일 이미지 생성

### 2️⃣ 진짜 나를 위한 날씨 코디 추천

#### 📍 위치 기반 날씨 정보
- 현재 위치를 인식해 지역별 날씨 정보 표시
- 평균 기온, 최고/최저 기온, 강수 확률 등 제공
- '위치 변경' / '내일 날씨' 버튼으로 유동적인 확인 가능

#### 🧥 비슷한 날씨에서 내가 입었던 착장 기록
- 과거 비슷한 기온일 때 내가 착용했던 아웃핏 이미지 표시
- 체감 기록도 함께 표시되어 스스로 피드백 받는 기능

#### 👔 내 옷장에서 오늘 입을 수 있는 추천 코디
- 기온 + 옷장 등록된 아이템을 기반으로 실제로 입을 수 있는 조합 추천
- "다시 추천" 버튼으로 여러 조합 생성 가능

### 3️⃣ 경험을 나누는 실용적 커뮤니티

<div align="center">

| 🏆 인기 코디 | 🎨 유사 스타일 | 📱 구매 연동 |
|:---:|:---:|:---:|
| Outfit TOP 3 공유 | 비슷한 취향 발견 | 아이템 정보 확인 |

</div>

- **비슷한 날씨, 비슷한 취향을 가진 사람들과 연결**: 같은 날씨에 다른 사람들은 무엇을 입었는지 확인
- **구성 아이템의 정보와 구매처까지 바로 확인**: 마음에 드는 아이템 클릭 시 상세 정보 및 구매 링크 제공

---

## 🛠️ 기술 스택

### 📱 Frontend (Android)

| Category | Technologies |
|:---:|:---|
| **Language** | ![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white) |
| **Architecture** | MVVM Pattern |
| **Platform** | Android SDK 35 |
| **Async** | Coroutines |
| **IDE** | Android Studio 2024.3.2 Meerkat |
| **UI** | ViewBinding, XML Layout |
| **Network** | Retrofit2, Gson |

### 🔧 주요 라이브러리

```gradle
dependencies {
    // Coroutines
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    
    // ViewModel & LiveData (MVVM)
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.7.0'
    
    // Retrofit2 (API)
    implementation 'com.squareup.retrofit2:retrofit:2.11.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.11.0'
    
    // ViewBinding
    buildFeatures { viewBinding true }
}
```

---

## 📁 프로젝트 구조

```
Onfit-Android/
├── app/
│   ├── manifests/
│   │   └── AndroidManifest.xml
│   │
│   ├── kotlin+java/
│   │   └── com.example.onfit/
│   │       ├── ui/
│   │       │   ├── MainActivity
│   │       │   ├── HomeFragment
│   │       │   ├── CalendarFragment
│   │       │   ├── CommunityFragment
│   │       │   ├── MyPageFragment
│   │       │   └── WardrobeFragment
│   │       │
│   │       ├── adapter/
│   │       │   ├── BestOutfitAdapter
│   │       │   ├── SimilarStyleAdapter
│   │       │   └── LatestStyleAdapter
│   │       │
│   │       ├── model/
│   │       │   ├── BestItem
│   │       │   └── SimItem
│   │       │
│   │       └── viewmodel/
│   │           └── (ViewModels)
│   │
│   └── res/
│       ├── drawable/
│       ├── layout/
│       │   ├── activity_main.xml
│       │   ├── fragment_home.xml
│       │   ├── fragment_calendar.xml
│       │   ├── fragment_community.xml
│       │   ├── fragment_mypage.xml
│       │   ├── fragment_wardrobe.xml
│       │   ├── best_outfit_item.xml
│       │   └── similar_style_item.xml
│       │
│       ├── menu/
│       │   └── navigation_menu.xml
│       └── navigation/
│           └── nav_graph.xml
```

---

## 👥 팀원 소개

| Name | Responsibilities |
|:---:|:---:|
| 혜윤/윤신혜 | 아이템 등록, outfit 상세 | 
| 원/양다원 | 옷장 flow 전체(옷장 화면, 검색 필터, 아이템 추가, 옷 세부 정보),  캘린더(캘린더 메인 화면, 스타일 별 outfit 화면) |
| 미니오/김민서 | 회원가입, 로그인, 커뮤니티 |

</div>

---

## 📋 개발 규칙

### 🌿 브랜치 전략

- `develop`: 최신 버전
- `feature/[기능명]`: 기능 단위 개발 브랜치 (예: `feature/add-cloth`)

### 💬 Commit 메시지 규칙

| Type | Description |
|:---:|:---|
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 코드 리팩토링 (기능 변화 없음) |
| `docs` | 문서 수정 (README 등) |
| `style` | 코드 포맷팅, 세미콜론 누락 등 |
| `test` | 테스트 코드 추가 또는 수정 |
| `chore` | 빌드 설정, 패키지 관리 등 기타 작업 |

#### 예시
```
feat: 로그인 화면 UI 구현
fix: 날짜 오류로 앱 튕김 현상 수정
refactor: DiaryViewModel 로직 리팩토링
docs: README에 커밋 규칙 추가
```

### 🎯 코드 컨벤션

#### 1. 중괄호 스타일
```kotlin
// ✅ Good
if (condition) {
    // code
}

// ❌ Bad
if (condition)
{
    // code
}
```

#### 2. 들여쓰기
- **Tab 키 기준 4칸** 사용
- 공백(Space) 대신 **Tab 키 사용**

#### 3. 주석 스타일
```kotlin
// 한 줄 주석은 // 사용

/**
 * 복잡한 설명이 필요한 경우
 * 블록 주석 사용
 */
```

#### 4. XML 네이밍 규칙
- 형식: `역할명_뷰타입약자`
```xml
<!-- 예시 -->
<ImageView android:id="@+id/photo_iv" />
<TextView android:id="@+id/title_tv" />
<Button android:id="@+id/save_btn" />
<EditText android:id="@+id/input_et" />
```

### 🔍 Issue & PR 규칙

#### Issue 템플릿
```markdown
### 📝 설명
어떤 기능을 개발하고 싶은지 자세히 설명해주세요.

### ✅ 체크리스트
- [ ] UI 설계
- [ ] 기능 로직 구현
- [ ] 테스트 코드 작성
- [ ] 코드 리뷰 요청
```

#### PR 템플릿
```markdown
## ✨ 작업 개요
- 어떤 기능을 개발/수정했는지 한줄 요약

## 🔨 작업 내용
- 작업한 상세 내용 정리

## ✅ 체크리스트
- [ ] 주석/변수명 정리
- [ ] 세 명 모두 피드백 완료
- [ ] Merge 전 최신 develop 브랜치 반영

## 📸 스크린샷(선택)
- UI 변화가 있다면 스크린샷 첨부

## 📌 참고사항
- 관련된 이슈나 설명
```

#### PR 병합 규칙
- **팀원 전원의 확인 및 승인 후** 머지
- 코드 리뷰 중 문제가 없을 경우에만 병합

---

## 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 있습니다. 자세한 내용은 [LICENSE](LICENSE) 파일을 참조하세요.

---

## 📞 문의

프로젝트에 대한 문의사항이나 제안이 있으시면 Issue를 통해 연락 주세요!

<div align="center">

**Made with ❤️ by Onfit Team**

[⬆ Back to Top](#-onfit---스타일을-기록하고-연결하는-내-손안의-스마트-옷장)

</div>
