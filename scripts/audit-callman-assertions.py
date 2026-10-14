#!/usr/bin/env python3
"""Conservative, source-grounded comparison of 337 Postman pm.test blocks with Java checks.

Important: this is a static *evidence index*, not a JS interpreter and not a PASS report.
Unknown assertion bodies are marked REVIEW_REQUIRED instead of assumed covered.
Makes no HTTP calls or changes to sandbox state.
"""
import csv
import json
import re
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
COLLECTION = ROOT / 'postman' / 'Final_Project_CallMan_FLAT_Run_All_117.postman_collection.json'
MANIFEST = ROOT / 'src/test/resources/workflow/callman-117.json'
JAVA = ROOT / 'src/test/java/tests/FullCallManWorkflowIT.java'
OUTPUT = ROOT / 'docs/Assertion_Mapping.csv'


def to_literal(text):
    text = text.strip()
    if text in ('null', 'true', 'false'):
        return {'null': None, 'true': True, 'false': False}[text]
    if text.startswith(('"', "'")) and text.endswith(text[0]):
        return text[1:-1]
    try:
        return int(text) if re.fullmatch(r'-?\d+', text) else float(text)
    except ValueError:
        return object()


def grade(number, assertion, manifest_row, code):
    name, expr = assertion
    status = manifest_row['expectedStatus']
    if re.search(r'pm\.expect\(pm\.response\.code\)\.to\.equal\(' + str(status) + r'\)', expr):
        return 'EXACT_STATUS', 'runRow: assertEquals(expected, response.statusCode())'
    error = manifest_row.get('errorCode', '')
    if error and f'pm.expect(pm.response.json().code).to.equal("{error}")' in expr:
        return 'EXACT_ERROR_CODE', 'runRow: assertEquals(errorCode, jsonPath(code))'
    # Literal field expectations are reliably translated when every field in the source
    # block has a matching 'equals' check in the manifest. No partial-credit given.
    field_equalities = re.findall(r'pm\.expect\(d\.([\w.]+)\)\.to\.equal\(([^\)]*)\)', expr)
    if field_equalities:
        translated = {ch['path']: ch['value'] for ch in manifest_row.get('assertions', [])
                      if ch.get('kind') == 'equals'}
        if all(path in translated and translated[path] == to_literal(want)
               for path, want in field_equalities) and 'pm.expect(' == 'pm.expect(':
            # Blocks may include additional, unrecognized expectations. Do not mark exact
            # unless every pm.expect is one of the matched field equalities.
            if expr.count('pm.expect(') == len(field_equalities):
                return 'EXACT_LITERAL', 'structuralChecks: all source d.field == literal checks in manifest'
    exists_paths = [ch['path'] for ch in manifest_row.get('assertions', [])
                    if ch.get('kind') == 'exists']
    if re.search(r'pm\.expect\(d\.(\w+)\)\.to\.exist', expr):
        path = re.search(r'pm\.expect\(d\.(\w+)\)\.to\.exist', expr).group(1)
        if path in exists_paths:
            return 'EXACT_EXISTS', 'structuralChecks: exists '+path
    if number == 30 and 'pm.response.text()).to.equal("")' in expr:
        return 'EXACT_EMPTY_BODY', 'runRow: status=204 asserts response string empty'
    field_validation = [ch['field'] for ch in manifest_row.get('assertions', [])
                        if ch.get('kind') == 'validationField']
    if field_validation and 'details.some' in expr and any(v in expr for v in field_validation):
        return 'MANIFEST_SEMANTIC', 'structuralChecks: validationField, inspect JS expression for full parity'
    # Label known handwritten assertions only where the exact source intent is
    # represented by an explicit branch in businessInvariants (not inferred from PASS).
    manual = {
        1: [('typeof d.token', 'AuthHelper.token(1): nonblank token; runRow length>10')],
        2: [('typeof d.token', 'AuthHelper.token(2): nonblank token; runRow length>10')],
        5: [('id).to.match', 'businessInvariants: beneficiary ID regex'),
            ('iban.indexOf', 'businessInvariants: normalized IBAN has no spaces'),
            ('typeof d.ibanMasked', 'businessInvariants: masked IBAN string'),
            ('headers.get("Location")', 'businessInvariants: Location header')],
        6: [('id).to.match', 'businessInvariants: ben_ ID regex'),
            ('iban.indexOf', 'businessInvariants: normalized IBAN'),
            ('typeof d.ibanMasked', 'businessInvariants: masked IBAN'),
            ('headers.get("Location")', 'businessInvariants: Location header'),
            ('environment.get("benIban2")', 'businessInvariants: normalized IBAN2')],
        7: [('id).to.match', 'businessInvariants: ben_ ID regex'),
            ('iban.indexOf', 'businessInvariants: normalized IBAN'),
            ('typeof d.ibanMasked', 'businessInvariants: masked IBAN'),
            ('headers.get("Location")', 'businessInvariants: Location header')],
        8: [('d.items.some', 'businessInvariants: beneficiary ID presence')],
        9: [('d.items.some', 'businessInvariants: case-insensitive search')],
        10: [('d.items.some', 'businessInvariants: nickname search')],
        11: [('d.items.every', 'businessInvariants: all currency AZN'),
             ('d.total).to.equal(d.items.length)', 'businessInvariants: matching total vs items')],
        28: [('d.items.every', 'businessInvariants: fullName type for sorted records'),
             ('localeCompare(b,"az")', 'businessInvariants: explicit Azerbaijani primary alphabet comparator (not full ICU parity)')],
        20: [('d.id).to.exist', 'businessInvariants: beneficiary id regex')],
        21: [('d.id).to.exist', 'businessInvariants: beneficiary id regex')],
        22: [('d.id).to.exist', 'businessInvariants: beneficiary id regex')],
        23: [('d.id).to.exist', 'businessInvariants: beneficiary id regex')],
        24: [('d.id).to.exist', 'businessInvariants: beneficiary id regex')],
        25: [('d.id).to.exist', 'businessInvariants: beneficiary id regex')],
        26: [('d.id).to.exist', 'businessInvariants: beneficiary id regex')],
        29: [('d.items).to.eql([])', 'businessInvariants: no match empty items')],
        31: [('d.items.some', 'businessInvariants: deleted item must be absent')],
        34: [('d.items.some', 'businessInvariants: surviving beneficiary remains')],
        42: [('maskedPan).to.match', 'businessInvariants: masked PAN regex'),
             ('headers.get("Location")', 'businessInvariants: card Location'),
             ('expiry exactly 3 years', 'businessInvariants: Baku expiry')],
        43: [('maskedPan).to.match', 'businessInvariants: masked PAN regex'),
             ('headers.get("Location")', 'businessInvariants: card Location'),
             ('expiry exactly 3 years', 'businessInvariants: Baku expiry')],
        44: [('maskedPan).to.match', 'businessInvariants: masked PAN regex'),
             ('headers.get("Location")', 'businessInvariants: card Location'),
             ('expiry exactly 3 years', 'businessInvariants: Baku expiry')],
        59: [('d.items.every', 'businessInvariants: exact catalog provider set'),
             ('["azerisiq"', 'businessInvariants: exact catalog provider IDs'),
             ('d.items.every', 'businessInvariants: six biller catalog fields')],
        60: [('d.items.map', 'businessInvariants: MOBILE provider set')],
        61: [('d.items.map', 'businessInvariants: INTERNET provider set')],
        62: [('d.items.map', 'businessInvariants: UTILITY provider set')],
        64: [('headers.get("Location")', 'businessInvariants: payment Location'),
             ('receiptNo).to.match', 'businessInvariants: exact receiptNo regex')],
        65: [('firstPaymentId', 'businessInvariants: idempotency same payment ID'),
             ('firstReceiptNo', 'businessInvariants: idempotency same receipt'),
             ('Idempotent-Replay', 'businessInvariants: replay header')],
        66: [('availableBalance', 'businessInvariants: 749.50 balance'),
             ('spentToday', 'businessInvariants: 50.50 spent')],
        101: [('spentToday>50.5', 'businessInvariants: spent increase'),
              ('Math.abs', 'businessInvariants: balance arithmetic')],
        104: [('d.items).to.eql([])', 'businessInvariants: empty items array')],
        114: [('paymentsByBakuDate', 'businessInvariants: Baku-day count for created payments')],
        102: [('response history schema', 'businessInvariants: textual fields and numeric total'),
              ('Date.parse', 'businessInvariants: newest first')],
        115: [('d.items.every', 'businessInvariants: only citynet results')],
        116: [('d.items.map', 'businessInvariants: exact Card B payment IDs')],
        117: [('d.items).to.eql([])', 'businessInvariants: zero items')],
    }
    for snippet, evidence in manual.get(number, []):
        if snippet in expr or snippet in name:
            return 'HANDWRITTEN_SEMANTIC', evidence + ' (review JavaScript parity)'
    return 'REVIEW_REQUIRED', 'Source pm.test has no statically proven 1:1 Java match; manual review required'


def main():
    items = json.loads(COLLECTION.read_text(encoding='utf-8'))['item']
    manifest = json.loads(MANIFEST.read_text(encoding='utf-8'))
    java = JAVA.read_text(encoding='utf-8')
    assert len(items) == len(manifest) == 117
    assert 'businessInvariants' in java and 'structuralChecks' in java
    rows = []
    for n, (item, spec) in enumerate(zip(items, manifest), 1):
        test_lines = []
        for ev in item.get('event', []):
            if ev.get('listen') == 'test':
                test_lines.extend(ev['script'].get('exec', []))
        assertions = []
        for line in test_lines:
            if 'pm.test(' not in line:
                continue
            match = re.match(r'\s*pm\.test\((\"|\')(.*?)\1,\s*(.*)\);\s*$', line)
            if not match:
                raise ValueError(f'Unparsed pm.test: request #{n}: {line}')
            assertions.append((match.group(2), match.group(3)))
        assert len(assertions) == spec['sourcePmTestBlocks'], (n, len(assertions))
        task = re.search(r'\bG(101|102|103|201|202|203|204|301|302|303)\b', item['name'])
        task_id = 'GRAD-' + task.group(1) if task else 'AUTH'
        for j, assertion in enumerate(assertions, 1):
            coverage, evidence = grade(n, assertion, spec, java)
            rows.append([n, task_id, j, item['name'], assertion[0], coverage, evidence,
                         'FINAL_LIVE_RUN_NOT_ATTACHED', assertion[1]])
    assert len(rows) == 337, len(rows)
    OUTPUT.parent.mkdir(exist_ok=True)
    with OUTPUT.open('w', newline='', encoding='utf-8-sig') as f:
        writer = csv.writer(f)
        writer.writerow(['Request_No','GRAD_Task','Assert_No','CallMan_Request','pm.test_Name',
                         'Static_Evidence_Class','Java_Evidence','Evidence_Reference', 'Original_JavaScript_Assertion'])
        writer.writerows(rows)
    counts = Counter(r[5] for r in rows)
    print(f'[AUDIT] {len(rows)} source pm.test blocks / 117 requests')
    for key in ['EXACT_STATUS','EXACT_ERROR_CODE','EXACT_LITERAL','EXACT_EXISTS','EXACT_EMPTY_BODY','MANIFEST_SEMANTIC',
                'HANDWRITTEN_SEMANTIC','REVIEW_REQUIRED']:
        print(f'  {key}: {counts.get(key,0)}')
    print('[AUDIT] Static evidence is conservative; HANDWRITTEN_SEMANTIC is NOT byte-for-byte JS parity.')
    return counts

if __name__ == '__main__':
    main()
