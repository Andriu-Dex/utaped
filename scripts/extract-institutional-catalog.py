"""Extract official activity references without modifying their Office sources."""
import hashlib
import json
import re
from pathlib import Path
from xml.etree import ElementTree as ET
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'docs/templates'
W = {'w': 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}
X = {'s': 'http://schemas.openxmlformats.org/spreadsheetml/2006/main'}

def workbook(path):
    with ZipFile(path) as archive:
        strings = [''.join(t.text or '' for t in item.findall('.//s:t', X)) for item in ET.fromstring(archive.read('xl/sharedStrings.xml')).findall('s:si', X)] if 'xl/sharedStrings.xml' in archive.namelist() else []
        document = ET.fromstring(archive.read('xl/worksheets/sheet1.xml'))
        for row in document.findall('.//s:sheetData/s:row', X):
            values = {}
            for cell in row:
                raw = cell.find('s:v', X)
                value = raw.text or '' if raw is not None else ''.join(t.text or '' for t in cell.findall('.//s:t', X))
                values[re.sub(r'\d+', '', cell.attrib['r'])] = strings[int(value)] if cell.get('t') == 's' else value
            yield row.attrib['r'], values

def main():
    reference = json.loads((ROOT / 'backend/src/main/resources/development/institutional-reference.json').read_text(encoding='utf-8'))
    groups = {g['key']: g['name'] for g in reference['groups']}
    applicability = {**{i: ['diseno'] for i in range(1, 8)}, 11: ['tutor'], 12: ['uoc', 'virtual', 'club-telecom', 'club-industrial', 'club-ti', 'club-software', 'club-robotica'], 14: ['eventos'], 15: ['group'], 16: ['graduados'], 17: ['vinculacion'], 18: ['investigacion'], 21: ['vinculacion'], 23: ['practicas'], 29: ['redes'], 30: ['redes']}
    entries = []
    filename = 'PLAN DE MEJORAS - CARRERAS.docx'
    with ZipFile(SOURCE / filename) as archive:
        document = ET.fromstring(archive.read('word/document.xml'))
        context = ['', '', '']
        for row in document.findall('.//w:tbl/w:tr', W):
            cells = [' '.join(''.join(t.text or '' for t in p.findall('.//w:t', W)) for p in cell.findall('w:p', W)).strip() for cell in row.findall('w:tc', W)]
            if len(cells) < 5 or not cells[3].isdigit(): continue
            context = [cells[i] or context[i] for i in range(3)]
            indicator = int(re.search(r'\d+', context[2])[0])
            entries.append({'key': 'improvement-plan-2026-' + cells[3], 'title': cells[4], 'category': 'IMPROVEMENT_PLAN', 'suggestedGroups': [groups[key] for key in applicability.get(indicator, [])], 'context': {'sourceKey': 'improvement-plan-2026-' + cells[3], 'sourceFile': filename + ' / actividad ' + cells[3], 'objective': context[0], 'criterion': context[1], 'indicator': context[2], 'sourceYear': 2026}})
    filename = 'Matriz_POA_2026.xlsx'
    for number, cells in workbook(SOURCE / filename):
        if number == '1' or not cells.get('C'): continue
        key = 'poa-2026-row-' + number
        entries.append({'key': key, 'title': cells['C'], 'category': 'POA', 'suggestedGroups': [groups[key] for key in ['group', 'vinculacion', 'graduados']], 'context': {'sourceKey': key, 'sourceFile': filename + ' / Matriz POA / fila ' + number, 'objective': cells.get('A'), 'strategy': cells.get('B'), 'measure': cells.get('D'), 'plannedCompliance': cells.get('E'), 'sourceYear': 2026}})
    for activity in reference['activities']:
        if activity['category'] != 'IMPROVEMENT_ACTION': continue
        key = 'improvement-action-2026-' + activity['key']
        entries.append({'key': key, 'title': activity['title'], 'category': 'IMPROVEMENT_ACTION', 'careerCode': 'TI' if activity['source'].startswith('PROPUESTAS') else None, 'suggestedGroups': [groups[activity['groupKey']]], 'context': {'sourceKey': key, 'sourceFile': activity['source'], 'sourceYear': 2026}})
    names = ['Actividades.docx', 'PLAN DE MEJORAS - CARRERAS.docx', 'Matriz_POA_2026.xlsx', 'PROPUESTAS DE MEJORA CARRERA DE TI.xlsx']
    names = sorted(set(names + [e['context']['sourceFile'].split(' / ')[0] for e in entries]))
    assert all(len(e['title']) <= 500 for e in entries)
    payload = {'revision': '2026-10-30', 'sources': [{'file': name, 'sha256': hashlib.sha256((SOURCE / name).read_bytes()).hexdigest()} for name in names], 'entries': entries}
    target = ROOT / 'backend/src/main/resources/planning/institutional-activities-2026.json'
    target.parent.mkdir(exist_ok=True)
    target.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    assert sum(e['category'] == 'IMPROVEMENT_PLAN' for e in entries) == 67
    assert sum(e['category'] == 'POA' for e in entries) == 14
    print(f'{len(entries)} referencias extraídas; originales intactos.')

if __name__ == '__main__': main()
