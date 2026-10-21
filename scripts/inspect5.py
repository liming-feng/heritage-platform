# -*- coding: utf-8 -*-
"""第5章 前台 — 自下而上修复（坐标从大到小，保证前面的坐标不漂移）"""
import sys, json
sys.path.insert(0, r'E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/scripts')
from rlib import call


def struct():
    r = call('doc_resolve_document_structure', {'mode': 'full', 'limit': 0, 'text_preview_length': 1200})
    return json.loads(r[r.find('{"effective_mode'):])['nodes']


def w(text, s, e):
    r = call('doc_replace_text', {'text': text, 'ranges': [{'begin': s, 'end': e}]})
    return 'ok' in r


def show():
    print('--- layout ---')
    for n in struct():
        if n.get('type') == 'Table':
            continue
        s = n['start_index']
        if 24700 <= s <= 28500:
            t = (n.get('text_preview') or '').strip()
            print(' ', s, n['end_index'], 'H%s' % n.get('heading_level') if n.get('heading_level') else 'P', '|', t[:56])


show()
