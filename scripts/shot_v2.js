const { chromium } = require('playwright-core');
(async () => {
  const b = await chromium.launch({ executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe', headless: true, args: ['--disable-gpu'] });
  for (const it of [{p:1,n:'a'},{p:2,n:'b'},{p:3,n:'c'},{p:4,n:'d'},{p:5,n:'e'}]) {
    const p = await b.newPage({ viewport: { width: 900, height: 1200 } });
    await p.goto(`file:///E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/_report_tmp/v2.pdf#page=${it.p}&zoom=85`, { waitUntil: 'load', timeout: 60000 });
    await p.waitForTimeout(4000);
    await p.screenshot({ path: `_report_tmp/v2_${it.n}.png` });
    await p.close();
  }
  console.log('done');
  await b.close();
})();
