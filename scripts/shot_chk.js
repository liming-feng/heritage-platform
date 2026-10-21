const { chromium } = require('playwright-core');
(async () => {
  const b = await chromium.launch({ executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe', headless: true, args: ['--disable-gpu'] });
  for (const it of [{p:1,n:'p1'},{p:2,n:'p2'},{p:4,n:'p3'},{p:7,n:'p7'}]) {
    const p = await b.newPage({ viewport: { width: 1000, height: 1250 } });
    await p.goto(`file:///E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/_report_tmp/check4.pdf#page=${it.p}&zoom=95`, { waitUntil: 'load', timeout: 60000 });
    await p.waitForTimeout(4500);
    await p.screenshot({ path: `_report_tmp/c_${it.n}.png` });
    console.log('ok', it.n);
    await p.close();
  }
  await b.close();
})();
