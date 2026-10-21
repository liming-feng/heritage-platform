// gen_uc.js v2 —— 修正布局
const fs = require('fs');
const sharp = require('sharp');

function esc(s){return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');}

function usecase(opts){
  const {system, actor, cases, W=1180} = opts;
  const n = cases.length;
  const topPad = 150;      // 标题区高度
  const botPad = 60;
  const rY = 32;
  const step = 96;         // 每个用例的行距
  const H = topPad + step*n + botPad;
  const boxX = 430, boxY = 46, boxW = W-boxX-56, boxH = H-92;
  const rX = 132;

  let s = '';
  s += `<rect x="0" y="0" width="${W}" height="${H}" fill="#ffffff"/>`;
  s += `<rect x="${boxX}" y="${boxY}" width="${boxW}" height="${boxH}" fill="#fbfcfe" stroke="#5B8FF9" stroke-width="2.5" rx="8"/>`;
  s += `<rect x="${boxX+boxW/2-165}" y="${boxY}" width="330" height="48" fill="#ffffff" stroke="#5B8FF9" stroke-width="2.5" rx="6"/>`;
  s += `<text x="${boxX+boxW/2}" y="${boxY+32}" font-family="Microsoft YaHei,SimHei,sans-serif" font-size="22" font-weight="bold" fill="#1f3a68" text-anchor="middle">${esc(system)}</text>`;

  // actor 垂直居中
  const ay = H/2 - 70;
  const ax = 175;
  s += `<circle cx="${ax}" cy="${ay}" r="26" fill="#fff" stroke="#333" stroke-width="2.5"/>`;
  s += `<line x1="${ax}" y1="${ay+26}" x2="${ax}" y2="${ay+118}" stroke="#333" stroke-width="2.5"/>`;
  s += `<line x1="${ax-48}" y1="${ay+52}" x2="${ax+48}" y2="${ay+52}" stroke="#333" stroke-width="2.5"/>`;
  s += `<line x1="${ax}" y1="${ay+118}" x2="${ax-38}" y2="${ay+186}" stroke="#333" stroke-width="2.5"/>`;
  s += `<line x1="${ax}" y1="${ay+118}" x2="${ax+38}" y2="${ay+186}" stroke="#333" stroke-width="2.5"/>`;
  s += `<text x="${ax}" y="${ay+228}" font-family="Microsoft YaHei,SimHei,sans-serif" font-size="21" font-weight="bold" fill="#1f3a68" text-anchor="middle">${esc(actor)}</text>`;

  const cy0 = boxY + topPad - 40;
  const colors = ['#E8F0FE','#E6F4EA','#FFF3E0','#F3E8FD','#FDE8E8','#E0F2F1','#FDF6E3','#EDE7F6'];
  const cx = boxX + boxW/2;
  for(let i=0;i<n;i++){
    const cy = cy0 + step*i;
    const c = colors[i%colors.length];
    s += `<ellipse cx="${cx}" cy="${cy}" rx="${rX}" ry="${rY}" fill="${c}" stroke="#5B8FF9" stroke-width="1.8"/>`;
    s += `<text x="${cx}" y="${cy+7}" font-family="Microsoft YaHei,SimHei,sans-serif" font-size="17" fill="#20344f" text-anchor="middle">${esc(cases[i])}</text>`;
    s += `<line x1="${ax+46}" y1="${ay+50}" x2="${cx-rX-4}" y2="${cy}" stroke="#9aa5b5" stroke-width="1.3"/>`;
  }
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}">${s}</svg>`;
}

const figs = {
  'fig2-1_user_uc': usecase({
    system:'非遗手工艺品展示与预约体验平台',
    actor:'普通用户',
    cases:['注册登录','首页与作品浏览','手工商城购物','购物车与下单','优惠券领取使用','体验课程预约','我的预约与核销码','评价互动','消息与收藏','个人中心']
  }),
  'fig2-2_craftsman_uc': usecase({
    system:'非遗手工艺品展示与预约体验平台',
    actor:'非遗匠人',
    cases:['入驻申请','商品发布与编辑','规格库存管理','订单发货处理','体验课程开设','预约申请审核','现场核销签到','评价回复','销售数据查看']
  }),
  'fig2-3_admin_uc': usecase({
    system:'非遗手工艺品展示与预约体验平台',
    actor:'平台管理员',
    cases:['用户与权限管理','匠人资质审核','商品与课程审核','预约监管与强制取消','预约规则配置','分类与轮播管理','资讯公告发布','优惠券运营','数据统计可视化','操作日志审计']
  })
};

(async ()=>{
  for(const [name, svg] of Object.entries(figs)){
    const out = `report_figs/${name}`;
    fs.writeFileSync(out+'.svg', svg);
    await sharp(Buffer.from(svg)).png().toFile(out+'.png');
    console.log('ok', out+'.png');
  }
})();
