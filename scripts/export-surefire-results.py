#!/usr/bin/env python3
"""Summarize local JUnit Surefire XML. No network, no environment variables/tokens exported."""
import argparse
import csv
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--reports', default=str(ROOT / 'target/surefire-reports'))
    parser.add_argument('--output', default=str(ROOT / 'docs/LOCAL_JUNIT_RESULTS.csv'))
    args = parser.parse_args()
    paths = sorted(Path(args.reports).glob('TEST-*.xml'))
    if not paths:
        raise SystemExit('[NO RESULT] No Surefire XML reports found. Run Maven tests first.')
    rows = []
    for f in paths:
        root = ET.parse(f).getroot()
        for case in root.iter('testcase'):
            status = ('ERROR' if case.find('error') is not None else
                      'FAIL' if case.find('failure') is not None else
                      'SKIPPED' if case.find('skipped') is not None else 'PASS')
            rows.append([case.get('classname',''), case.get('name',''), status,
                         case.get('time',''), f.name])
    with open(args.output, 'w', encoding='utf-8-sig', newline='') as handle:
        wr=csv.writer(handle)
        wr.writerow(['Class', 'Test Name', 'Execution Status', 'Seconds', 'Surefire XML'])
        wr.writerows(rows)
    counts=Counter(r[2] for r in rows)
    print(f'[SUREFIRE] {len(paths)} XML files; {len(rows)} testcase entries; '
          f'PASS={counts["PASS"]}, FAIL={counts["FAIL"]}, '
          f'ERROR={counts["ERROR"]}, SKIPPED={counts["SKIPPED"]}')
    print('[NOTE] Surefire XML may include reports from multiple invocations. Run "mvn clean test" '
          'for a single clean suite summary.')
    print(f'[OUTPUT] {args.output}')

if __name__ == '__main__': main()
