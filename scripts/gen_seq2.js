// gen_seq2.js —— 预约审核与核销顺序图
const fs = require('fs');
const sharp = require('sharp');
function esc(s){return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');}
const FONT='Microsoft YaHei,SimHei,sans-serif';

function seqAudit(){
  const W=1360, H=1080;
  const lanes=['匠人 / 管理员','CraftsmanBooking\n/ AdminController','BookingService\nImpl','BookingMapper\n/ MySQL','MessageService','用户'];
  const n=lanes.length;
  const padL=70, padR=70, topY=190, botY=H-80;
  const span=(W-padL-padR);
  const step=span/(n-1);
  const xs = lanes.map((_,i)=>padL+step*i);

  let s=`<rect width="${W}" height="${H}" fill="#ffffff"/>`;
  s+=`<text x="${W/2}" y="48" font-family="${FONT}" font-size="24" font-weight="bold" fill="#1f3a68" text-anchor="middle">预约审核与核销顺序图</text>`;

  // 生命线头部
  lanes.forEach((L,i)=>{
    const lines=L.split('\n');
    const hh = lines.length>1 ? 68 : 54;
    s+=`<rect x="${xs[i]-92}" y="${topY-hh}" width="184" height="${hh}" fill="#E8F0FE" stroke="#5B8FF9" stroke-width="2" rx="6"/>`;
    lines.forEach((t,k)=>{
      s+=`<text x="${xs[i]}" y="${topY-hh+ (lines.length>1? (k===0?26:50) : 33)}" font-family="${FONT}" font-size="${t.startsWith('Crafts')||t.startsWith('BookingS')?14:16}" fill="#1f3a68" text-anchor="middle" font-weight="bold">${esc(t)}</text>`;
    });
    // 生命线
    s+=`<line x1="${xs[i]}" y1="${topY}" x2="${xs[i]}" y2="${botY}" stroke="#9aa5b5" stroke-width="1.6" stroke-dasharray="8,6"/>`;
  });

  // 消息定义：from, to, text, type(sync/async/self/dashed)
  const msgs=[
    {f:0,t:1,y:230,text:'1. audit(bookingId, {pass/reject, reason})',c:'#3a6fb0'},
    {f:1,t:2,y:282,text:'2. audit(craftsmanId, dto)',c:'#3a6fb0'},
    {f:2,t:2,y:334,text:'3. 校验操作人权限与预约归属'},
    {f:2,t:3,y:390,text:'4. SELECT status FROM booking WHERE id = ?',c:'#3a6fb0'},
    {f:3,t:2,y:438,text:'5. 返回当前状态（待审核）',c:'#c96a2b',dash:true},
    {f:2,t:2,y:492,text:'6. 状态机校验 transfer(待审核 → 已通过)'},
    {f:2,t:3,y:548,text:'7. UPDATE booking SET status=1, audit_time=NOW()',c:'#3a6fb0'},
    {f:2,t:3,y:600,text:'8. INSERT booking_status_log（操作留痕）',c:'#3a6fb0'},
    {f:3,t:2,y:652,text:'9. 事务提交成功',c:'#c96a2b',dash:true},
    {f:2,t:4,y:706,text:'10. sendNotice(userId, "预约已通过")',c:'#7a5cc0'},
    {f:2,t:5,y:760,text:'11. 生成核销码并推送站内消息',c:'#7a5cc0'},
    {f:1,t:0,y:814,text:'12. 返回审核结果 Result<BookingVO>',c:'#2f8f5b',dash:true},
    {f:3,t:2,y:868,text:'13. 现场核销：UPDATE status=4（已核销）',c:'#3a6fb0'},
    {f:2,t:0,y:922,text:'14. 核销成功，返回 CheckinResultVO',c:'#2f8f5b',dash:true},
  ];

  msgs.forEach(m=>{
    const x1=xs[m.f], x2=xs[m.t];
    const col = m.c || '#c96a2b';
    if(m.f===m.t){
      // 自调用
      const d=34;
      s+=`<path d="M ${x1} ${m.y} h ${d} v 32 h -${d}" fill="none" stroke="${col}" stroke-width="1.8"/>`;
      s+=`<polygon points="${x1},${m.y+32} ${x1+11},${m.y+25} ${x1+11},${m.y+39}" fill="${col}"/>`;
      s+=`<text x="${x1+d+10}" y="${m.y+12}" font-family="${FONT}" font-size="15" fill="#20344f">${esc(m.text)}</text>`;
    } else {
      const dir = x2>x1?1:-1;
      s+=`<line x1="${x1}" y1="${m.y}" x2="${x2-dir*10}" y2="${m.y}" stroke="${col}" stroke-width="1.9" ${m.dash?'stroke-dasharray="8,5"':''}/>`;
      s+=`<polygon points="${x2},${m.y} ${x2-dir*13},${m.y-7} ${x2-dir*13},${m.y+7}" fill="${col}"/>`;
      const mid=(x1+x2)/2;
      s+=`<text x="${mid}" y="${m.y-9}" font-family="${FONT}" font-size="15" fill="#20344f" text-anchor="middle">${esc(m.text)}</text>`;
    }
  });

  s+=`<text x="${padL}" y="${H-30}" font-family="${FONT}" font-size="14" fill="#909399">说明：实线为同步调用，虚线为返回消息；每次状态流转均写入 booking_status_log 表留痕。</text>`;
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}">${s}</svg>`;
}

(async ()=>{
  const svg = seqAudit();
  fs.writeFileSync('report_figs/fig4-5_booking_audit_seq.svg', svg);
  await sharp(Buffer.from(svg)).png().toFile('report_figs/fig4-5_booking_audit_seq.png');
  console.log('ok fig4-5_booking_audit_seq.png');
})();
