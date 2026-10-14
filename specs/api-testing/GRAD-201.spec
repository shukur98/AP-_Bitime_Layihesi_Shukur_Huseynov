# GRAD-201 — Virtual kart buraxmaq

> EPIC-2 | POST /cards.
> JUnit testleri: `src/test/java/tests/`. Fixture isteyen ve state-changing testler README-de aciqlanib.

## AC-3 low boundary 0.99
tags: api, grad201, smoke
* G201 initialAmount "0.99" fails validation

## AC-3 high boundary 1000.01
tags: api, grad201, smoke
* G201 initialAmount "1000.01" fails validation

## AC-4 precision 10.555
tags: api, grad201, smoke
* G201 initialAmount "10.555" fails validation

## AC-4 amount string
tags: api, grad201, smoke
* G201 initialAmount "text-50" fails validation

## 404 missing account
tags: api, grad201, smoke
* G201 unknown account returns ACCOUNT_NOT_FOUND

