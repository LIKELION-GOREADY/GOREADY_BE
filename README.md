## 서울과학기술대학교 멋쟁이사자처럼 장기 프로젝트 "외출준비 (Go Ready)"
2024.09.13 ~ 2024.11.28
<br><br>

## 📌 서비스 소개 - 당신의 외출준비, 탭 2번이면 끝나요!
<img width="1019" alt="image" src="https://github.com/user-attachments/assets/e4d98b18-f175-480a-b48b-14b3040c0dd8">




# 👥 Developers
|  오세연  |  이소라  |   노경인   |   박신형   |                                                                                                   
| :-----: | :----: | :------: |:--------: | 
| <img src="https://avatars.githubusercontent.com/oosedus?v=4" width=90px alt="오세연"/> |<img src="https://avatars.githubusercontent.com/leeesoraaa?v=4" width=90px alt="이소라"/>  | <img src="https://avatars.githubusercontent.com/kyinn1307?v=4" width=90px alt="노경인"/> | <img src="https://avatars.githubusercontent.com/shinh09?v=4" width=90px alt="박신형"/> |  
| [@oosedus](https://github.com/oosedus) | [@leeesoraaa](https://github.com/leeesoraaa) |  [@kyinn1307](https://github.com/kyinn1307) |  [@shinh09](https://github.com/shinh09)  | 
| 💻 Backend Lead | 💻 Backend | 🕸️ Frontend Lead | 🕸️ Frontend |

<br><br>


# 📄 API 명세서

## 1. 기온/강수 조회 API

| **항목**             | **설명**                          |
|----------------------|----------------------------------|
| **기능명**           | 기온/강수 조회                     |
| **기능 설명**         | 현재 기온 및 강수 정보를 제공합니다.  |
| **Method**           | `GET`                           |
| **Query Parameters** | `lat` (위도), `lon` (경도)        |

### End Point

```http
GET /weather?lat={latitude}&lon={longitude}
```

### Response Example
#### ✅ Success (200 OK)
```json
{
    "status": 200,
    "message": "날씨 조회 성공입니다.",
    "data": {
        "status": "hot",
        "isUmbrella": true,
        "hightemp": 25,
        "lowtemp": 13,
        "difftemp": 2,
        "currenttemp": 22,
        "rainper": 70
    }
}
```

## 2. 미세먼지 조회 API
| **항목**             | **설명**                          |
|----------------------|----------------------------------|
| **기능명**           | 미세먼지 조회                    |
| **기능 설명**         | 기능 설명	마스크 착용 유무와 경보 여부 확인에 사용합니다.  |
| **Method**           | `GET`                           |
| **Query Parameters** | `lat` (위도), `lon` (경도)        |

### End Point
```http
GET /mask?lan={latitude}&lon={longitude}
```

### Response Example
#### ✅ Success (200 OK)
```json
{
    "status": 200,
    "message": "미세먼지 조회 성공입니다.",
    "data": {
        "alert": true,
        "isMask": true,
        "address": "공릉동"
    }
}
```
<br><br><br>

# 💻 Stack

### 🛠️ BackEnd

**Language & Framework**  
<img src="https://img.shields.io/badge/Java-007396?style=flat&logo=Java&logoColor=white" />
<img src="https://img.shields.io/badge/Spring Boot-6DB33F?style=flat&logo=SpringBoot&logoColor=white" /> 
<img src="https://img.shields.io/badge/Spring Security-6DB33F?style=flat&logo=SpringSecurity&logoColor=white" />

**Database**  
<img src="https://img.shields.io/badge/Redis-DC382D?style=flat&logo=Redis&logoColor=white" />

**Build Tool**  
<img src="https://img.shields.io/badge/Gradle-02303A?style=flat&logo=Gradle&logoColor=white" />

**Cloud & Hosting**  
<img src="https://img.shields.io/badge/Amazon EC2-FF9900?style=flat&logo=AmazonEC2&logoColor=white" />
<img src="https://img.shields.io/badge/Amazon Route 53-232F3E?style=flat&logo=AmazonRoute53&logoColor=white" />

**Containerization & CI/CD**  
<img src="https://img.shields.io/badge/Docker-2496ED?style=flat&logo=Docker&logoColor=white" /> 
<img src="https://img.shields.io/badge/GitHub Actions-2088FF?style=flat&logo=GitHubActions&logoColor=white" />

**Network & Security**  
<img src="https://img.shields.io/badge/Nginx-009639?style=flat&logo=nginx&logoColor=white" />
<img src="https://img.shields.io/badge/Certbot-9ACD32?style=flat&logo=letsencrypt&logoColor=white" />

<br>

### 🌐 FrontEnd
**Language & Framework**  
<img src="https://img.shields.io/badge/React-61DAFB?style=flat&logo=React&logoColor=white" />
<img src="https://img.shields.io/badge/JavaScript-F7DF1E?style=flat&logo=JavaScript&logoColor=black" />
<img src="https://img.shields.io/badge/Axios-5A29E4?style=flat&logo=Axios&logoColor=white" />
<img src="https://img.shields.io/badge/Styled Components-DB7093?style=flat&logo=styled-components&logoColor=white" />

**Deployment**  
<img src="https://img.shields.io/badge/vercel-000000?style=flat&logo=vercel&logoColor=white" />

<br>

### 🌎 Co-Work
[<img src="https://img.shields.io/badge/GitHub-181717?style=flat&logo=GitHub&logoColor=white" />]
<img src="https://img.shields.io/badge/Notion-000000?style=flat&logo=Notion&logoColor=white" />
<br><br>


# 🏛️ System Architecture
![goready drawio](https://github.com/user-attachments/assets/c4415421-3e06-4080-b761-bbfe77a55c1c)


<br><br>

# 📏 Convention

**commit convention** <br>
`conventionType: 구현한 내용` <br><br>

**convention Type** <br>
| convention type | description |
| --- | --- |
| `feat` | 새로운 기능 구현 |
| `add` | 파일 및 코드 추가 |
| `chore` | 부수적인 코드 수정 및 기타 변경사항 |
| `docs` | 문서 추가 및 수정, 삭제 |
| `fix` | 버그 수정 |
| `rename` | 파일 및 폴더 이름 변경 |
| `test` | 테스트 코드 추가 및 수정, 삭제 |
| `refactor` | 코드 리팩토링 |

## Branch
### 
- `컨벤션명/#이슈번호`
- pull request를 통해 develop branch에 merge 후, branch delete
- 부득이하게 develop branch에 직접 commit 해야 할 경우, `!hotfix:` 사용

<br><br><br>
