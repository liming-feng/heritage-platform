# -*- coding: utf-8 -*-
"""
P2 用户认证模块联调测试
覆盖：注册 / 登录 / 双 Token / 刷新 / 黑名单 / 越权隔离 / 参数校验 / 个人资料
"""
import json
import random
import urllib.error
import urllib.request

BASE = "http://127.0.0.1:8080/api"
OUT = r"E:\workbuddy1\2026-09-17-10-20-47\tools\_authtest.txt"

lines = []
passed = 0
failed = 0


def api(method, path, body=None, token=None):
    data = None
    headers = {}
    if body is not None:
        data = json.dumps(body, ensure_ascii=False).encode("utf-8")
        headers["Content-Type"] = "application/json; charset=utf-8"
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(BASE + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=20) as r:
            raw = r.read().decode("utf-8")
            return r.status, (json.loads(raw) if raw else None)
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8")
        try:
            parsed = json.loads(raw)
        except Exception:
            parsed = None
        return e.code, parsed


def check(name, cond, detail=""):
    global passed, failed
    if cond:
        passed += 1
        lines.append("  [PASS] %s  %s" % (name, detail))
    else:
        failed += 1
        lines.append("  [FAIL] %s  %s" % (name, detail))


def code(r):
    return (r[1] or {}).get("code")


def data(r):
    return (r[1] or {}).get("data") or {}


suffix = random.randint(1000, 9999)
new_user = "test%d" % suffix

# 1. 注册
r = api("POST", "/portal/auth/register", {
    "username": new_user, "password": "abc123456", "nickname": "联调测试用户",
    "phone": "13800001234", "email": "t%d@heritage.com" % suffix})
check("1.注册", r[0] == 200 and code(r) == 200 and data(r).get("accessToken"),
      "HTTP %s code=%s 用户=%s" % (r[0], code(r), new_user))
user_access = data(r).get("accessToken")
user_refresh = data(r).get("refreshToken")

# 2. 重复注册
r = api("POST", "/portal/auth/register", {"username": new_user, "password": "abc123456"})
check("2.重复注册被拒", code(r) == 2002, "code=%s msg=%s" % (code(r), (r[1] or {}).get("message")))

# 3. 参数校验
r = api("POST", "/portal/auth/register", {"username": "ab", "password": "1"})
check("3.参数校验", code(r) == 1001, "code=%s msg=%s" % (code(r), (r[1] or {}).get("message")))

# 4. 管理员登录（验证初始化 bcrypt 密文对应 123456）
r = api("POST", "/portal/auth/login", {"username": "admin", "password": "123456"})
admin_user = data(r).get("user") or {}
check("4.管理员登录", r[0] == 200 and code(r) == 200, "role=%s roleName=%s" % (admin_user.get("role"), admin_user.get("roleName")))
admin_access = data(r).get("accessToken")
admin_refresh = data(r).get("refreshToken")

# 5. Long 精度：id 必须是字符串
check("5.Long序列化为字符串", isinstance(admin_user.get("id"), str), "id 类型=%s 值=%s" % (type(admin_user.get("id")).__name__, admin_user.get("id")))

# 6. 密码错误
r = api("POST", "/portal/auth/login", {"username": "admin", "password": "wrongpwd"})
check("6.密码错误提示", code(r) == 2003, "code=%s msg=%s" % (code(r), (r[1] or {}).get("message")))

# 7. 普通用户登录
r = api("POST", "/portal/auth/login", {"username": new_user, "password": "abc123456"})
u = data(r).get("user") or {}
check("7.普通用户登录", code(r) == 200, "role=%s roleName=%s" % (u.get("role"), u.get("roleName")))
user_access = data(r).get("accessToken")
user_refresh = data(r).get("refreshToken")

# 8. 带 token 取当前用户
r = api("GET", "/portal/user/info", None, user_access)
d = data(r)
check("8.查询当前用户", code(r) == 200 and d.get("username") == new_user,
      "username=%s password字段=%s" % (d.get("username"), d.get("password")))
check("9.密码不外泄", "password" not in d, "返回字段=%s" % list(d.keys()))

# 10. 不带 token
r = api("GET", "/portal/user/info")
check("10.未登录被拦(401)", r[0] == 401, "HTTP %s" % r[0])

# 11. 刷新令牌
r = api("POST", "/portal/auth/refresh", {"refreshToken": user_refresh})
d = data(r)
check("11.刷新令牌", code(r) == 200 and d.get("accessToken"), "新token前缀=%s..." % (d.get("accessToken") or "")[:20])

# 12. 非法 refresh
r = api("POST", "/portal/auth/refresh", {"refreshToken": "not.a.jwt"})
check("12.非法refresh被拒", code(r) == 1006, "code=%s msg=%s" % (code(r), (r[1] or {}).get("message")))

# 13. 普通用户越权访问管理端
r = api("GET", "/admin/user/list", None, user_access)
check("13.越权访问管理端(403)", r[0] == 403, "HTTP %s" % r[0])

# 14. 管理员访问管理端前缀（该接口尚未开发 → 统一 404，业务码 1007，且不打印 ERROR 堆栈）
r = api("GET", "/admin/user/list", None, admin_access)
check("14.不存在接口返回1007", code(r) == 1007, "HTTP %s code=%s msg=%s" % (r[0], code(r), (r[1] or {}).get("message")))

# 14b. 未登录访问管理端前缀 → 401（而不是 404，避免暴露接口是否存在）
r = api("GET", "/admin/user/list")
check("14b.未登录访问管理端(401)", r[0] == 401, "HTTP %s" % r[0])

# 15. 修改资料
r = api("PUT", "/portal/user/info", {"nickname": "改名后的昵称", "gender": 1}, user_access)
d = data(r)
check("15.修改资料", code(r) == 200 and d.get("nickname") == "改名后的昵称",
      "nickname=%s gender=%s" % (d.get("nickname"), d.get("gender")))

# 16. 修改密码（旧密码错）
r = api("PUT", "/portal/user/password", {"oldPassword": "wrong", "newPassword": "newpwd123"}, user_access)
check("16.旧密码错误被拒", code(r) == 2005, "code=%s msg=%s" % (code(r), (r[1] or {}).get("message")))

# 17. 修改密码（正确）
r = api("PUT", "/portal/user/password", {"oldPassword": "abc123456", "newPassword": "newpwd123"}, user_access)
check("17.修改密码成功", code(r) == 200, "msg=%s" % (r[1] or {}).get("message"))

# 18. 用新密码登录
r = api("POST", "/portal/auth/login", {"username": new_user, "password": "newpwd123"})
check("18.新密码可登录", code(r) == 200, "code=%s" % code(r))
user_access = data(r).get("accessToken")
user_refresh = data(r).get("refreshToken")

# 19. 退出登录
r = api("POST", "/portal/auth/logout", {"refreshToken": user_refresh}, user_access)
check("19.退出登录", code(r) == 200, "msg=%s" % (r[1] or {}).get("message"))

# 20. 退出后原 access_token 立即失效
r = api("GET", "/portal/user/info", None, user_access)
check("20.黑名单生效(401)", r[0] == 401, "HTTP %s" % r[0])

# 21. 退出后 refresh_token 也不能用
r = api("POST", "/portal/auth/refresh", {"refreshToken": user_refresh})
check("21.退出后refresh失效", code(r) == 1006, "code=%s" % code(r))

# 22. 带非法 token 仍能登录（认证接口白名单，避免过期后登录死锁）
r = api("POST", "/portal/auth/login", {"username": "admin", "password": "123456"}, "invalid.token.value")
check("22.带非法token仍可登录", code(r) == 200, "code=%s msg=%s" % (code(r), (r[1] or {}).get("message")))

lines.append("")
lines.append("===== 结果: 通过 %d 项, 失败 %d 项 =====" % (passed, failed))

with open(OUT, "w", encoding="utf-8") as f:
    f.write("\n".join(lines))
print("passed=%d failed=%d" % (passed, failed))
