const { chromium } = require('playwright-core');
(async () => {
  const b = await chromium.launch({
    executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe',
    headless: true,
    args: ['--enable-features=PdfViewerUpdate', '--disable-gpu']
  });
  const p = await b.newPage({ viewport: { width: 1200, height: 1400 } });
  const url = 'file:///E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/_report_tmp/final.pdf#page=46&zoom=110';
  await p.goto(url, { waitUntil: 'load', timeout: 60000 });
  await p.waitForTimeout(6000);
  await p.screenshot({ path: '_report_tmp/pdf_p46.png' });
  console.log('shot done');
  await b.close();
})();
