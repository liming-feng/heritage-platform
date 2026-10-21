import subprocess, json, os
SKILL = r"E:/workbuddy/resources/app.asar.unpacked/resources/plugins/workbuddy-builtin/skills/tencent-local-office-edit"
def call(tool, args, fid=None):
    if fid: args = dict(args); args['file_id'] = fid
    p = subprocess.run(['python3','edsdk.py','call',tool,'--json',json.dumps(args,ensure_ascii=False)],
                       capture_output=True, text=True, encoding='utf-8', cwd=SKILL)
    return p.stdout.strip()
def rep(fid, begin, end, text):
    r = call('doc_replace_text', {'text':text,'ranges':[{'begin':begin,'end':end}]}, fid)
    return 'ok' in r, r[-100:]
def do_edits(fid, edits):
    """从后往前执行；edits = [(begin,end,text),...] 按 begin 升序给出"""
    for b,e,t in sorted(edits, key=lambda x:-x[0]):
        ok, msg = rep(fid, b, e, t)
        if not ok: print('FAIL', b, e, msg)
    return True
