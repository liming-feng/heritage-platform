import subprocess, json, sys, os

SDK_DIR = r"E:/workbuddy/resources/app.asar.unpacked/resources/plugins/workbuddy-builtin/skills/workbuddy-local-office-edit"
FID = "C:/Users/limingfeng/Desktop/2026_09_08_2.开题报告_姓名学号/工程实训报告_李明峰23100025_.docx"
IMG = "E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/_shots_w"
os.chdir(SDK_DIR)

def call(tool, **kw):
    args = ["python3", "edsdk.py", "call", tool, f"file_id={FID}"]
    js = {}
    for k, v in kw.items():
        if isinstance(v, (dict, list)):
            js[k] = v
        else:
            args.append(f"{k}={v}")
    if js:
        args += ["--json", json.dumps(js, ensure_ascii=False)]
    out = subprocess.run(args, capture_output=True, text=True, encoding="utf-8").stdout
    try:
        d = json.loads(out.strip().splitlines()[-1])
    except Exception:
        print("RAW:", out[:300]); raise
    if "ok" not in str(d.get("message", "")):
        print("FAIL:", tool, json.dumps(d, ensure_ascii=False)[:300])
    return d

CAPFMT = {"font_family": "Times New Roman", "font_size": 12}
CAPPARA = {"jc": "center", "first_line_indent": 0, "line_spacing": 1.5, "line_spacing_rule": 1}

def addfig(idx, img, w, h, cap):
    r = call("doc_insert_image", idx=idx, image_path=f"{IMG}/{img}.png", w=w, h=h)
    li = r["last_edit_index"]
    r = call("doc_insert_paragraph_with_text", idx=li, text="")
    ni = r["next_index"]
    r = call("doc_insert_paragraph_with_text", idx=ni, text=cap,
             text_format=CAPFMT, paragraph_property=CAPPARA)
    print(f"  + {cap}  -> next {r['next_index']}")
    return r["next_index"]

# (插入点, [(图片, 宽, 高, 题注), ...]) —— 从文档后部往前的顺序
GROUPS = [
    (28700, [("36_ad_config", 520, 177, "图5-20 预约规则配置页面")]),
    (28379, [("33_ad_goods", 520, 325, "图5-17 商品管理页面"),
             ("25_cz_orders", 520, 323, "图5-18 订单管理页面")]),
    (28064, [("31_ad_users", 520, 325, "图5-14 用户管理页面"),
             ("32_ad_craftsmans", 520, 325, "图5-15 匠人资质审核页面"),
             ("37_ad_logs", 520, 239, "图5-16 操作日志页面")]),
    (27851, [("30_ad_dashboard", 520, 325, "图5-13 管理端数据总览页面")]),
    (27462, [("12_my_bookings", 520, 310, "图5-11 我的预约记录页面"),
             ("12b_booking_qrcode", 520, 301, "图5-12 到场核销码弹窗")]),
    (27170, [("10_course_list", 520, 325, "图5-8 体验课程列表页面"),
             ("11b_course_detail", 520, 325, "图5-9 课程详情与预约入口"),
             ("11c_book_dialog", 520, 325, "图5-10 预约信息填写弹窗")]),
    (26868, [("08_goods_list", 520, 325, "图5-4 非遗商城商品列表页面"),
             ("09_goods_detail", 520, 325, "图5-5 商品详情页面"),
             ("16_cart", 520, 310, "图5-6 购物车页面"),
             ("17_checkout", 520, 325, "图5-7 订单确认与结算页面")]),
    (25460, [("06_register", 520, 325, "图5-2 用户注册页面"),
             ("13_profile", 520, 244, "图5-3 个人中心页面")]),
]

for idx, figs in GROUPS:
    print(f"@ {idx}")
    cur = idx
    for img, w, h, cap in figs:
        cur = addfig(cur, img, w, h, cap)

print("ALL FIGURES INSERTED")
