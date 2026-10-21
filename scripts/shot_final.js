const { chromium } = require('playwright-core');
(async () => {
  const b = await chromium.launch({ executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe', headless: true, args: ['--disable-gpu'] });
  for (const it of [{p:5,n:'toc'},{p:47,n:'f1'},{p:53,n:'f2'}]) {
    const p = await b.newPage({ viewport: { width: 1150, height: 1350 } });
    await p.goto(`file:///E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/_report_tmp/final4.pdf#page=${it.p}&zoom=105`, { waitUntil: 'load', timeout: 60000 });
    await p.waitForTimeout(5000);
    await p.screenshot({ path: `_report_tmp/fin_${it.n}.png` });
    console.log('ok', it.n);
    await p.close();
  }
  await b.close();
})();
