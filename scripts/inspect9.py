# -*- coding: utf-8 -*-
"""结论 + 参考文献 + 致谢"""
import sys, json
sys.path.insert(0, r'E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/scripts')
from rlib import call


def w(text, s, e):
    r = call('doc_replace_text', {'text': text, 'ranges': [{'begin': s, 'end': e}]})
    return 'ok' in r


def struct():
    r = call('doc_resolve_document_structure', {'mode': 'full', 'limit': 0, 'text_preview_length': 800})
    return json.loads(r[r.find('{"effective_mode'):])['nodes']


def show(lo, hi, tag=''):
    print('=== ' + tag)
    for n in struct():
        if n.get('type') == 'Table':
            continue
        s = n['start_index']
        if lo <= s <= hi:
            t = (n.get('text_preview') or '').strip()
            print(' ', s, n['end_index'], 'H%s' % n.get('heading_level') if n.get('heading_level') else 'P', '|', t[:75])


show(33000, 36000, 'BEFORE')
