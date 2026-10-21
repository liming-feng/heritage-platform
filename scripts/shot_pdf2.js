const { chromium } = require('playwright-core');
(async () => {
  const b = await chromium.launch({ executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe', headless: true, args: ['--disable-gpu'] });
  const pages = [{p:4,n:'toc'},{p:48,n:'ch5a'},{p:51,n:'ch5b'},{p:55,n:'ch5c'},{p:64,n:'test'}];
  for (const it of pages) {
    const p = await b.newPage({ viewport: { width: 1150, height: 1350 } });
    await p.goto(`file:///E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/_report_tmp/final3.pdf#page=${it.p}&zoom=105`, { waitUntil: 'load', timeout: 60000 });
    await p.waitForTimeout(5000);
    await p.screenshot({ path: `_report_tmp/pg_${it.n}.png` });
    console.log('shot', it.n, it.p);
    await p.close();
  }
  await b.close();
})();
