const { chromium } = require('playwright-core');
(async () => {
  const b = await chromium.launch({ executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe', headless: true });
  const p = await b.newPage({ viewport: { width: 1000, height: 1200 }, deviceScaleFactor: 1 });
  await p.goto('file:///E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/_report_tmp/preview.html', { waitUntil: 'load', timeout: 60000 });
  await p.waitForTimeout(2500);
  // 找到图5-1 附近
  const el = await p.locator('text=图5-1 用户登录页面').first();
  if (await el.count()) {
    await el.scrollIntoViewIfNeeded();
    await p.waitForTimeout(800);
    await p.screenshot({ path: '_report_tmp/html_fig51.png' });
    console.log('captured fig5-1 area');
  } else { console.log('fig5-1 not found'); }
  await b.close();
})();
