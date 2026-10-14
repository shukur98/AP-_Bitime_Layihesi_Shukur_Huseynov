# API Graduation Project

API testing project using Java, REST Assured, JUnit 5, Gauge and Allure against the instructor's training service at `https://api.anarabbas.com`. Scope: three epics, ten user stories (GRAD-101–303), 69 acceptance criteria, 158 unique manual test-case IDs, 117 ordered HTTP requests and 10 Gauge specifications. The source CallMan collection holds 337 `pm.test` blocks. Request execution and full JavaScript assertion parity are different metrics.

Run `bash scripts/preflight.sh` for offline mapping checks, `bash scripts/run-117-allure.sh --new-sandbox` for a stateful disposable-sandbox JUnit run with Allure, `bash scripts/run-local.sh` for standard JUnit and `bash scripts/run-gauge.sh` for Gauge. Authentication and session management are automatic. See `docs/QA_Report.html`, `docs/QA_Coverage.xlsx`, `docs/Execution_Evidence.md` and `presentation/APİ_Bitirme_Layihesi.pptx` for the teacher handoff. Offline checks are not reported as live HTTP test passes.
