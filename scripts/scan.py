# -*- coding: utf-8 -*-
import zipfile, re, sys, os, glob

p = r"E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/_verify/deliverable.docx"
z = zipfile.ZipFile(p)
names = z.namelist()
xml = z.read('word/document.xml').decode('utf-8', 'ignore')

# strip tags -> plain text per paragraph
paras = re.findall(r'<w:p[ >].*?</w:p>', xml, re.S)
def ptext(x):
    return ''.join(re.findall(r'<w:t[^>]*>(.*?)</w:t>', x, re.S))

texts = [ptext(x) for x in paras]
full = '\n'.join(texts)

print('=== 文件统计 ===')
print('文件大小:', os.path.getsize(p), 'bytes')
print('段落数:', len(paras))
print('图片数:', len([n for n in names if n.startswith('word/media/')]))
zh = re.findall(r'[\u4e00-\u9fff]', full)
print('中文字符数:', len(zh))
print('总字符数:', len(full.replace('\n','')))

print()
print('=== 残留模板标记扫描 ===')
pats = ['微信小程序', r'\*\*', '（注释', 'yhzhgl', 'xinwentongzhi', 'X1', 'X2', 'X3', 'X4',
        'ZZ0', 'ZZ1', 'ZZ2', 'ZZ3', '（请', '此处', '填写', 'TODO', '待补', '学号姓名',
        '[Hyperlink]', '占位']
for pat in pats:
    if pat == r'\*\*':
        hits = [(i, t) for i, t in enumerate(texts) if '**' in t]
    else:
        hits = [(i, t) for i, t in enumerate(texts) if pat in t]
    flag = '!!' if hits else 'ok'
    print(f'[{flag}] {pat}: {len(hits)} 处')
    for i, t in hits[:6]:
        print(f'      p{i}: {t[:110]}')

print()
print('=== 章节结构（标题类段落） ===')
keys = ['摘要','Abstract','第1章','第2章','第3章','第4章','第5章','第6章','结论','参考文献','致谢','目录','目    录']
for i, t in enumerate(texts):
    s = t.strip()
    if not s:
        continue
    for k in keys:
        if s.startswith(k) and len(s) < 60:
            print(f'  p{i}: {s}')
            break
