// 项目页面批量截图（使用系统 Chrome，无需下载浏览器）
const { chromium } = require('playwright-core');
const fs = require('fs');
const path = require('path');

const OUT = 'E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/_shots';
const BASE = 'http://127.0.0.1:5173';
const API = 'http://127.0.0.1:8080/api';

const USERS = {
  user: { username: 'user01', password: '123456', role: 1 },
  craftsman: { username: 'zhangjian', password: '123456', role: 2 },
  admin: { username: 'admin', password: '123456', role: 3 }
};

async function login(u) {
  const r = await fetch(`${API}/portal/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: u.username, password: u.password })
  });
  const j = await r.json();
  if (j.code !== 200) throw new Error('login fail ' + u.username + ' ' + JSON.stringify(j).slice(0, 200));
  return j.data;
}

(async () => {
  fs.mkdirSync(OUT, { recursive: true });
  const auth = {};
  for (const [k, u] of Object.entries(USERS)) auth[k] = await login(u);
  console.log('login ok:', Object.keys(auth).join(','));

  const browser = await chromium.launch({
    executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe',
    headless: true,
    args: ['--force-device-scale-factor=1', '--hide-scrollbars']
  });

  const shots = JSON.parse(process.argv[2] || '[]');
  for (const s of shots) {
    const ctx = await browser.newContext({
      viewport: { width: s.w || 1440, height: s.h || 900 },
      deviceScaleFactor: 2,
      locale: 'zh-CN'
    });
    const page = await ctx.newPage();
    if (s.auth) {
      const d = auth[s.auth];
      const payload = {
        accessToken: d.accessToken,
        refreshToken: d.refreshToken,
        userInfo: {
          id: String(d.user.id),
          username: d.user.username,
          nickname: d.user.nickname || d.user.username,
          avatar: d.user.avatar || '',
          role: d.user.role
        }
      };
      await page.addInitScript(([k, v]) => {
        localStorage.setItem(k, v);
      }, ['heritage_auth', JSON.stringify(payload)]);
    }
    const url = BASE + s.path;
    try {
      await page.goto(url, { waitUntil: 'networkidle', timeout: 45000 });
    } catch (e) {
      await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 45000 });
    }
    await page.waitForTimeout(s.wait || 2500);
    if (s.scroll) {
      await page.evaluate((y) => window.scrollTo(0, y), s.scroll);
      await page.waitForTimeout(800);
    }
    if (s.click) {
      try {
        const loc = page.locator(s.click).first();
        await loc.click({ timeout: 8000 });
        await page.waitForTimeout(2000);
        try { await page.waitForSelector('.el-dialog', { state: 'visible', timeout: 6000 }); } catch (e) {}
        await page.waitForTimeout(1200);
      } catch (e) { console.log('  click miss:', s.click, e.message.slice(0, 60)); }
    }
    if (s.full) {
      await page.screenshot({ path: path.join(OUT, s.name + '.png'), fullPage: true });
    } else {
      await page.screenshot({ path: path.join(OUT, s.name + '.png') });
    }
    const title = await page.title();
    console.log('OK', s.name, '|', url, '|', title);
    await ctx.close();
  }
  await browser.close();
  console.log('ALL DONE');
})().catch(e => { console.error('FATAL', e.message); process.exit(1); });
