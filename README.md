# APİ Bitirmə Layihəsi

**REST API test avtomatlaşdırması | Java · REST Assured · JUnit 5 · Gauge · Allure**

Layihə müəllimin `https://api.anarabbas.com` tədris API-si üçün hazırlanıb. Məqsəd Acceptance Criteria əsasında bank əməliyyatlarını imitasiya edən funksiyaları yoxlamaq, testlərin nəticələrini təkrarlana bilən formada hesabatlaşdırmaqdır.

## Funksional əhatə

| EPIC | User story-lər | Yoxlanan funksiyalar |
|---|---|---|
| Saxlanılmış alıcılar | GRAD-101, 102, 103 | Yaratma, axtarış, filtr, sıralama, silmə |
| Virtual kartlar | GRAD-201, 202, 203, 204 | Yaratma, detallar, bloklama və blokdan çıxarma |
| Ödənişlər | GRAD-301, 302, 303 | Təchizatçılar, ödəniş, tarixçə və səhifələmə |

Manual test planı **69 Acceptance Criteria** və **158 unikal test case ID** əhatə edir. Java inteqrasiya axınında **117 HTTP request**, Gauge-də **10 specification**, mənbə CallMan collection-da isə **337 `pm.test` bloku** var. Request sayı ayrıca AC coverage və ya assertion parity göstəricisi deyil.

## Arxitektura

`src/test/java/helper` — request, autentifikasiya və test datası; `tests` — JUnit sınaqları və `FullCallManWorkflowIT`; `imp` — Gauge step implementation; `hooks` — ssenari lifecycle; `utils` — konfiqurasiya və Azərbaycan sıralaması. JSON Schema-lar `src/test/resources/schema/`, workflow manifesti `src/test/resources/workflow/`, test specification-lar `specs/api-testing/` qovluğundadır.

Qorunan sorğularda `POST /auth/login` avtomatik işləyir, əldə olunan token `Authorization: Bearer` başlığında istifadə olunur. Tədris API-sində yeni login yeni boş sandbox açdığı üçün 117-addımlıq axın ayrıca disposable sandbox-da işləyir. `.env` və token cache Git-ə daxil edilmir.

## İcra

**Tələb olunur:** Java 11+, Maven 3.x, Python 3. Gauge üçün Gauge CLI və Java/html-report plugin-ləri, Allure HTML üçün Allure CLI.

```bash
bash scripts/preflight.sh
bash scripts/run-local.sh
bash scripts/run-117-restassured.sh --new-sandbox
bash scripts/run-117-allure.sh --new-sandbox
bash scripts/run-gauge.sh
```

`preflight.sh` yalnız mənbə kolleksiyası və Java mapping-i offline yoxlayır. Canlı request göndərmir.

## Yekun QA sənədləri

- `docs/QA_Report.html` — funksional əhatə, konkret test nümunələri və hesabat statusu;
- `docs/QA_Coverage.xlsx` — task, AC, manual test case, request və assertion matrisi;
- `docs/AC_Traceability.csv`, `docs/Request_Mapping.csv`, `docs/Assertion_Mapping.csv` — yoxlanıla bilən ilkin mapping;
- `docs/Bug_Register.csv` və `docs/Bug_Report.md` — məhsul qüsurlarının qeydiyyatı;
- `docs/Execution_Evidence.md`, `evidence/preflight.log` — yoxlama metodikası və dəlil;
- `presentation/APİ_Bitirme_Layihesi.pptx` — müəllimə təqdimat.

Canlı icra sübutu `target/surefire-reports/`, `target/allure-results/`, `reports/allure-report/` və `reports/html-report/` qovluqlarında testlər işlədildikdən sonra yaranır. **Təsdiqlənmiş icra logu olmadan yekun kod üçün PASS göstərilmir.** Manual TC ID-sinin birbaşa uyğunluğu tapılmırsa, bu, testin mövcud olmaması və ya FAIL olması demək deyil.
