# -*- coding: utf-8 -*-
"""稳健的报告填充库：
- 每步都用 doc_find 按锚点文本实时定位，绝不使用过期坐标
- replace_para: 找到锚文本所在段落，整段替换为新文本
- 段落范围为 [para_start, para_end)，即 begin 段落起、含段落结束符
"""
import subprocess, json, time

SKILL = r"E:/workbuddy/resources/app.asar.unpacked/resources/plugins/workbuddy-builtin/skills/tencent-local-office-edit"
FID = "C:/Users/limingfeng/Desktop/2026_09_08_2.开题报告_姓名学号/工程实训报告_李明峰23100025.docx"


def call(tool, args, fid=None):
    a = dict(args)
    if fid or FID:
        a.setdefault('file_id', fid or FID)
    p = subprocess.run(['python3', 'edsdk.py', 'call', tool, '--json', json.dumps(a, ensure_ascii=False)],
                       capture_output=True, text=True, encoding='utf-8', cwd=SKILL)
    return p.stdout.strip()


def struct():
    r = call('doc_resolve_document_structure', {'mode': 'full', 'limit': 0, 'text_preview_length': 100})
    i = r.find('{"effective_mode')
    d = json.loads(r[i:])
    return d['nodes']


def find(text, nth=0):
    r = call('doc_find', {'text': text})
    try:
        d = json.loads(r[r.find('{'):])
        locs = d.get('locations', [])
        return locs[nth] if len(locs) > nth else None
    except Exception:
        return None


def para_range(anchor, nth=0):
    """返回锚文本所在段落的 [start, end) DOC 坐标"""
    loc = find(anchor, nth)
    if not loc:
        return None
    b = loc['begin']
    nodes = struct()
    for n in nodes:
        if n.get('type') == 'Table':
            if n.get('start_index', -1) <= b <= n.get('end_index', -1):
                return None
        else:
            if n.get('start_index', -1) <= b < n.get('end_index', -1):
                return (n['start_index'], n['end_index'])
    return None


def replace_para(anchor, newtext, nth=0):
    """把锚文本所在整段替换为 newtext（整段含段落结束符一起换）。"""
    rng = para_range(anchor, nth)
    if not rng:
        return False, 'anchor not found: ' + anchor[:20]
    b, e = rng
    r = call('doc_replace_text', {'text': newtext, 'ranges': [{'begin': b, 'end': e}]})
    return ('ok' in r), r[-100:]


def replace_text(anchor, newtext, nth=0):
    """只替换锚文本本身"""
    loc = find(anchor, nth)
    if not loc:
        return False, 'anchor not found: ' + anchor[:20]
    r = call('doc_replace_text', {'text': newtext,
                                  'ranges': [{'begin': loc['begin'], 'end': loc['end']}]})
    return ('ok' in r), r[-100:]


def do(pairs):
    """pairs: [(anchor, newtext, mode), ...] mode='para'|'text'"""
    ok = 0
    for it in pairs:
        anchor, newtext = it[0], it[1]
        mode = it[2] if len(it) > 2 else 'para'
        if mode == 'para':
            r = replace_para(anchor, newtext)
        else:
            r = replace_text(anchor, newtext)
        if r[0]:
            ok += 1
        else:
            print('  FAIL', anchor[:34], '|', r[1][:70])
    print(f'  -> {ok}/{len(pairs)} ok')
    return ok
