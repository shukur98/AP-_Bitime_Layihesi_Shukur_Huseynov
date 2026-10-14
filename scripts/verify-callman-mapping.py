#!/usr/bin/env python3
"""Offline source-vs-manifest verification. Makes NO HTTP requests and exposes NO tokens."""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ORIGINAL = ROOT / 'postman' / 'Final_Project_CallMan_FLAT_Run_All_117.postman_collection.json'
MANIFEST = ROOT / 'src/test/resources/workflow/callman-117.json'
items = json.loads(ORIGINAL.read_text(encoding='utf-8'))['item']
rows = json.loads(MANIFEST.read_text(encoding='utf-8'))
assert len(items) == len(rows) == 117, '117 CallMan requests required'
valid_kinds = {'equals', 'exists', 'length', 'variable', 'validationField', 'header', 'headerVar'}
pm_blocks = 0
js_length_checks = 0
for i, (original, row) in enumerate(zip(items, rows), 1):
    req = original['request']
    expected = {
        'name': original['name'],
        'method': req['method'],
        'url': req['url']['raw'],
        'body': req.get('body', {}).get('raw'),
        'headers': [{'key': h['key'], 'value': h['value']} for h in req.get('header', [])],
    }
    assert row['number'] == i, f'Number mismatch at {i}'
    for key, value in expected.items():
        assert row[key] == value, f'{i}: CallMan {key} no longer matches the manifest'
    scripts = ['\n'.join(ev['script'].get('exec', [])) for ev in original.get('event', [])
               if ev.get('listen') == 'test']
    count = sum(text.count('pm.test(') for text in scripts)
    assert row['sourcePmTestBlocks'] == count, f'{i}: pm.test count differs'
    pm_blocks += count
    script_text = '\n'.join(scripts)
    status_match = re.search(r'pm\.expect\(pm\.response\.code\)\.to\.equal\((\d+)\)', script_text)
    assert status_match is not None, f'{i}: source HTTP status assertion not found'
    assert int(status_match.group(1)) == row['expectedStatus'], f'{i}: HTTP status migrated incorrectly'
    for chk in row['assertions']:
        kind = chk['kind']
        assert kind in valid_kinds, f'{i}: unknown assertion kind {kind}'
        if kind in {'equals', 'exists', 'length', 'variable'}:
            path = chk['path']
            assert re.fullmatch(r'[A-Za-z_][A-Za-z_0-9]*(?:\.[A-Za-z_][A-Za-z_0-9]*)*', path), (
                f'{i}: unsupported migrated assertion path {path}')
            if path.endswith('.length'):
                js_length_checks += 1
                assert kind == 'equals', f'{i}: JS length property used with incompatible operator'
            if '.length.' in path:
                raise AssertionError(f'{i}: JS length property may only appear at path end')
    assert 200 <= int(row['expectedStatus']) < 500, f'{i}: status code out of range'
assert pm_blocks == 337, f'Expected 337 source pm.test blocks, found {pm_blocks}'
print(f'[PRE-FLIGHT PASS] 117 request contracts, 337 pm.test blocks, '
      f'{js_length_checks} translated JS .length property checks')
print('[NOTE] This verifies migration metadata and expression grammar, NOT live HTTP or full JS assertion parity.')
