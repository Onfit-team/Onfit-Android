- 기술 스택
    - 언어: 코틀린
    - 아키텍처: MVVM
    - 플랫폼: Android SDK 35
    - 비동기 처리: Coroutine
    - 안드로이드 스튜디오 버전: 2024.3.2 Meercat
    - UI: ViewBinding, XML Layout
    - API 연동: Retrofit2
- 라이브러리
    - **Coroutine**
        
        `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3`
        
    - **ViewModel & LiveData (MVVM)**
        
        `androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0`
        
        `androidx.lifecycle:lifecycle-livedata-ktx:2.7.0`
        
        `androidx.lifecycle:lifecycle-runtime-ktx:2.7.0`
        
    - **ViewBinding**
        
        `buildFeatures { viewBinding true }` 
        
    - **Retrofit2 (API 통신)**
        
        `com.squareup.retrofit2:retrofit:2.11.0`
        
        `com.squareup.retrofit2:converter-gson:2.11.0` 
        

—

브랜치 사용 전략

- 맡은 기능 별로 branch 이름을 정함(feature/add-cloth)

—

## 팀의 **Issue 컨벤션** + 템플릿

| 항목 | 규칙 |
| — | — |
| 제목 | `[기능] 로그인 구현` / `[버그] 홈에서 튕김 발생` 식으로 유형 명시 |
| 라벨 | `bug`, `feature`, `enhancement`, `question`, `docs` 등 사용 |
| 본문 구조 | 어떤 작업을 하고자 하는지, 세부 작업 항목 포함 |

Issue 템플릿

```jsx
name: Feature Request
description: 새로운 기능을 제안합니다
title: "[기능] "
labels: ["feature"]
body:
  - type: textarea
    attributes:
      label: 설명
      description: 어떤 기능을 개발하고 싶은지 자세히 설명해주세요.
    validations:
      required: true
  - type: checkboxes
    attributes:
      label: 체크리스트
      description: 해당 기능을 위해 필요한 작업
      options:
        - label: [ ] UI 설계
        - label: [ ] 기능 로직 구현
        - label: [ ] 테스트 코드 작성
        - label: [ ] 코드 리뷰 요청

```

## 팀의 **PR(Pull Request) 컨벤션** + 템플릿

| 항목 | 규칙 |
| --- | --- |
| 제목 | `[기능] 로그인 구현` / `[버그] 홈에서 튕김 발생` 식으로 유형 명시 |
| 라벨 | `bug`, `feature`, `enhancement`, `question`, `docs` 등 사용 |
| 본문 구조 | 어떤 작업을 하고자 하는지, 세부 작업 항목 포함 |

```jsx
## ✨ 작업 개요
- 어떤 기능을 개발/수정했는지 한줄 요약

## 🔨 작업 내용
- 작업한 상세 내용 정리
- ex) 로그인 화면 UI 구현
- ex) Retrofit으로 API 연동

## ✅ 체크리스트
- [ ] 주석/변수명 정리
- [ ] 세 명 모두 피드백 완료
- [ ] Merge 전 최신 develop 브랜치 반영

## 📸 스크린샷(선택)
- (UI 변화가 있다면 스크린샷 첨부)

## 📌 참고사항
- 관련된 이슈나 설명 등

```

---

### Commit 메시지 규칙 (Conventional Commits 기반)

| 타입 | 설명 |
| --- | --- |
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 코드 리팩토링 (기능 변화 없음) |
| `docs` | 문서 수정 (README 등) |
| `style` | 코드 포맷팅, 세미콜론 누락 등 |
| `test` | 테스트 코드 추가 또는 수정 |
| `chore` | 빌드 설정, 패키지 관리 등 기타 작업 |

---

Commit 메시지 작성 예시

```jsx
feat: 로그인 화면 UI 구현

fix: 날짜 오류로 앱 튕김 현상 수정

refactor: DiaryViewModel 로직 리팩토링

docs: README에 커밋 규칙 추가
```

---

코드 컨벤션

- **중괄호 `{` 사용**
    - 조건문, 반복문, 함수 등에서 여는 중괄호 `{`는 **줄바꿈 없이 동일한 줄에 작성**한다.
        
        ```kotlin
        복사편집
        if (condition) {
            // 수행 코드
        }
        ```
        
- **들여쓰기**
    - 코드 들여쓰기는 **Tab 키 기준 4칸**을 사용한다.
    - 공백(Space) 대신 **Tab 키 사용**을 원칙으로 한다.
- **주석 스타일**
    - 한 줄 설명 주석은 **`//` 스타일**을 기본으로 한다.
    - 주석은 간결하고 명확하게 작성하되, 특별한 설명이 필요할 경우에만 `/** */` 블록 주석 사용을 허용한다.
- **XML 네이밍 규칙**
    - 레이아웃 XML 파일에서 각 View의 id는 **역할명_뷰타입 약자** 형식으로 작성한다.
        - 예: `photo_iv` (ImageView), `title_tv` (TextView), `save_btn` (Button), `input_et` (EditText)
- **PR 병합(Merge) 규칙**
    - Pull Request는 **팀원 전원의 확인 및 승인 후** 머지한다.
    - 코드 리뷰 중 문제가 없을 경우에만 `develop` 또는 `main` 브랜치에 병합(Merge)한다.

```


---

Onfit-Android
├── app/
│   ├── manifests/
│   │   └── AndroidManifest.xml
│   │
│   ├── kotlin+java/
│   │   └── com.example.onfit/
│   │       ├── 📄 MainActivity
│   │       ├── 📄 HomeFragment
│   │       ├── 📄 CalendarFragment
│   │       ├── 📄 CommunityFragment
│   │       ├── 📄 MyPageFragment
│   │       ├── 📄 WardrobeFragment
│   │       ├── 📄 BestItem / SimItem
│   │       ├── 📄 BestOutfitAdapter / SimiliarStyleAdapter / LatestStyleAdapter(RecyclerView 어댑터)
│   │
│   ├── res/
│   │   ├── drawable/
│   │   ├── layout/
│   │   │   ├── activity_main.xml
│   │   │   ├── fragment_home.xml
│   │   │   ├── fragment_calendar.xml
│   │   │   ├── fragment_community.xml
│   │   │   ├── fragment_mypage.xml
│   │   │   ├── fragment_wardrobe.xml
│   │   │   ├── best_outfit_item.xml
│   │   │   └── similiar_style_item.xml
│   │   │
│   │   ├── menu/
│   │   │   └── navigation_menu.xml
│   │   ├── navigation/
│   │   │   └── nav_graph.xml
│   │   └── mipmap/
```
