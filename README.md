# Onfit-Android

# 1주차 과제 - 프로젝트 개발 환경 & 협업 규칙 정리

---

## ✅ 기술 스택

- 언어: Kotlin  
- 아키텍처: MVVM  
- 플랫폼: Android SDK 35  
- 비동기 처리: Coroutine  
- Android Studio 버전: 2024.3.2 Meercat  
- UI: ViewBinding, XML Layout  
- API 연동: Retrofit2  

---

## ✅ 사용할 라이브러리

**Coroutine**
```
org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3
```

**ViewModel & LiveData**
```
androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0  
androidx.lifecycle:lifecycle-livedata-ktx:2.7.0  
androidx.lifecycle:lifecycle-runtime-ktx:2.7.0
```

**ViewBinding**
```kotlin
buildFeatures {
    viewBinding true
}
```

**Retrofit2**
```
com.squareup.retrofit2:retrofit:2.11.0  
com.squareup.retrofit2:converter-gson:2.11.0
```

---

## ✅ 브랜치 전략

- 각자 맡은 **기능 단위**로 브랜치를 만들어서 작업함
- 브랜치 이름은 `feature/기능명` 형식으로 통일

예시:
```
feature/add-cloth
feature/login
```

---

## ✅ Issue 작성 규칙

- 제목 앞에 `[기능]`, `[버그]` 등 명시해서 어떤 이슈인지 구분
- 라벨은 `feature`, `bug`, `question` 등 상황에 맞게 붙이기
- 본문에는 무슨 작업을 할 건지랑, 체크리스트를 같이 적어두기

### 📋 Issue 템플릿
```yml
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

---

## ✅ PR(Pull Request) 컨벤션

- 제목도 Issue처럼 `[기능] 로그인 기능 구현` 등으로 명확하게
- PR 본문엔 작업 요약 + 한 일 정리 + 체크리스트 작성

### 📋 PR 템플릿
```md
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

## ✅ 커밋 메시지 규칙 (Conventional Commits 형식)

| 타입 | 의미 |
|------|------|
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 리팩토링 (기능은 그대로인데 코드 개선) |
| `docs` | 문서 수정 (README 등) |
| `style` | 코드 포맷팅 관련 |
| `test` | 테스트 코드 관련 |
| `chore` | 설정이나 기타 작업 등 |

**커밋 예시**
```
feat: 로그인 화면 UI 구현  
fix: 날짜 오류 수정  
refactor: DiaryViewModel 구조 리팩토링  
docs: README에 커밋 규칙 추가
```

---

## ✅ 코드 컨벤션 정리

- **중괄호 `{`**는 무조건 같은 줄에 붙여서 씀  
  ```kotlin
  if (isLoggedIn) {
      // ...
  }
  ```

- **들여쓰기**는 `Tab` 키 기준으로 하고, 4칸 들여쓰기  
- **주석**은 웬만하면 `//` 한 줄 주석 쓰기 (길어지면 `/** */`)

- **XML 뷰 ID 네이밍**
  - 형식: `역할_뷰타입약자`  
  - 예: `photo_iv`, `title_tv`, `save_btn`, `input_et`  

- **PR Merge 규칙**
  - PR은 팀원 전원 확인하고 이상 없을 때만 merge
  - 대상 브랜치는 `develop` 또는 `main`

---

## ✅ 안드로이드 개발 환경

- Android Studio: 2024.3.2 Meercat  
- targetSdk: 34 ~ 35  
- minSdk: 21  
- 테스트는 에뮬레이터 + 실제 기기 둘 다 사용함
