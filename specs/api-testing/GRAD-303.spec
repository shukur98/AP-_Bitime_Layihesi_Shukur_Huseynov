# GRAD-303 — Odenis tarixcesi filtr sehifeleme

> EPIC-3 | GET /bill-payments.
> JUnit testleri: `src/test/java/tests/`. Fixture isteyen ve state-changing testler README-de aciqlanib.

## AC-1 parametrsiz siyahi
tags: api, grad303, smoke
* G303 default payment history has page one and limit ten

## AC-4 limit=0
tags: api, grad303, smoke
* G303 invalid "limit" query "0" is rejected

## AC-4 limit=51
tags: api, grad303, smoke
* G303 invalid "limit" query "51" is rejected

## AC-4 page=0
tags: api, grad303, smoke
* G303 invalid "page" query "0" is rejected

## AC-4 page=abc
tags: api, grad303, smoke
* G303 invalid "page" query "abc" is rejected

## AC-5 invalid month
tags: api, grad303, smoke
* G303 invalid "from" query "2026-13-01" is rejected

## AC-6 inverted dates
tags: api, grad303, smoke
* G303 reversed dates are rejected

