// 交互式截图：点击后等待弹窗出现再截
const { chromium } = require('playwright-core');
const fs = require('fs');
const path = require('path');

const OUT = 'E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/_shots';
const BASE = 'http://127.0.0.1:5173';
const API = 'http://127.0.0.1:8080/api';

async function login(u) {
  const r = await fetch(`${API}/portal/auth/login`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(u)
  });
  const j = await r.json();
  if (j.code !== 200) throw new Error('login fail ' + JSON.stringify(j).slice(0, 150));
  return j.data;
}

(async () => {
  fs.mkdirSync(OUT, { recursive: true });
  const d = await login({ username: 'user01', password: '123456' });
  const payload = {
    accessToken: d.accessToken, refreshToken: d.refreshToken,
    userInfo: { id: String(d.user.id), username: d.user.username, nickname: d.user.nickname || d.user.username, avatar: d.user.avatar || '', role: d.user.role }
  };
  const browser = await chromium.launch({
    executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe',
    headless: true, args: ['--hide-scrollbars']
  });
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 }, deviceScaleFactor: 2, locale: 'zh-CN' });
  const page = await ctx.newPage();
  await page.addInitScript(([k, v]) => localStorage.setItem(k, v), ['heritage_auth', JSON.stringify(payload)]);

  await page.goto(BASE + '/my/bookings', { waitUntil: 'networkidle', timeout: 45000 });
  await page.waitForTimeout(3000);

  // 精确点击「核销码」按钮
  const btn = page.locator('button:has-text("核销码")').first();
  console.log('btn count =', await btn.count());
  await btn.click({ timeout: 8000 });
  try {
    await page.waitForSelector('.el-dialog', { state: 'visible', timeout: 8000 });
    console.log('dialog visible');
  } catch (e) { console.log('dialog not visible:', e.message.slice(0, 80)); }
  await page.waitForTimeout(2500);
  await page.screenshot({ path: path.join(OUT, '12b_booking_qrcode.png') });
  console.log('OK 12b');

  await browser.close();
})().catch(e => { console.error('FATAL', e.message); process.exit(1); });
