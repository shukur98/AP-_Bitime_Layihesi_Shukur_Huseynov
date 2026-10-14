# İcra sübutları

**Offline:** `bash scripts/preflight.sh`; təsdiqi `evidence/preflight.log`. Bu yoxlamada canlı HTTP request göndərilmir.

**Canlı JUnit workflow + Allure:** `bash scripts/run-117-allure.sh --new-sandbox`; `target/surefire-reports/` və `reports/allure-report/index.html`.

**Standart JUnit:** `bash scripts/run-local.sh`; nəticələr `target/surefire-reports/`.

**Gauge:** `bash scripts/run-gauge.sh`; nəticə `reports/html-report/index.html`.

Bu paketdə cari təqdimat kodunun yeni canlı API icra logu mövcud deyil. Statik testlərin PASS olması HTTP nəticəsini sübut etmir. Yekun PASS göstəricisi yalnız final kod ilə yaranmış Allure/Surefire hesabatı əsasında doldurulmalıdır.

**Kod sübutları:** `tests/FullCallManWorkflowIT.java` (117 request və assertion-lar), `utils/AzerbaijaniCollation.java` (sıralama), `src/test/resources/schema/` (response schema), `specs/api-testing/GRAD-301.spec` (Gauge acceptance), `docs/Request_Mapping.csv` (expected result), `docs/AC_Traceability.csv` (AC ↔ TC).
