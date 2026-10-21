// gen_more.js —— 生成 ER 图、管理员页面示意图、性能测试图
const fs = require('fs');
const sharp = require('sharp');
function esc(s){return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');}
const FONT = 'Microsoft YaHei,SimHei,sans-serif';

/* ============ 1. 非遗作品实体 E-R 图（Chen 记法：实体矩形 / 属性椭圆 / 主键下划线） ============ */
function erGoods(){
  const W=1180, H=900;
  let s = `<rect width="${W}" height="${H}" fill="#ffffff"/>`;
  // 标题
  s += `<text x="${W/2}" y="46" font-family="${FONT}" font-size="24" font-weight="bold" fill="#1f3a68" text-anchor="middle">非遗作品实体 goods 的 E-R 图</text>`;

  // 实体
  const ex=W/2, ey=H/2-10;
  s += `<rect x="${ex-105}" y="${ey-46}" width="210" height="92" fill="#E8F0FE" stroke="#5B8FF9" stroke-width="3" rx="6"/>`;
  s += `<text x="${ex}" y="${ey+8}" font-family="${FONT}" font-size="23" font-weight="bold" fill="#1f3a68" text-anchor="middle">非遗作品</text>`;

  // 属性（椭圆）环形布局
  const attrs = [
    {t:'作品编号', pk:true},   // 主键
    {t:'作品名称'},
    {t:'所属分类'},
    {t:'所属匠人'},
    {t:'封面图片'},
    {t:'价格'},
    {t:'库存'},
    {t:'销量'},
    {t:'浏览量'},
    {t:'上架状态'},
    {t:'详情介绍'},
  ];
  const n=attrs.length;
  const cx0=ex, cy0=ey;
  const rx=400, ry=300;
  const colors=['#FFF3E0','#E6F4EA','#FDE8E8','#F3E8FD','#E0F2F1','#FDF6E3','#EDE7F6','#E8F0FE'];
  for(let i=0;i<n;i++){
    const ang = -Math.PI/2 + i*2*Math.PI/n;
    const ax = cx0 + rx*Math.cos(ang);
    const ay = cy0 + ry*Math.sin(ang);
    const c = colors[i%colors.length];
    s += `<line x1="${cx0}" y1="${cy0}" x2="${ax}" y2="${ay}" stroke="#9aa5b5" stroke-width="1.6"/>`;
    s += `<ellipse cx="${ax}" cy="${ay}" rx="92" ry="34" fill="${c}" stroke="${attrs[i].pk?'#d9534f':'#5B8FF9'}" stroke-width="${attrs[i].pk?2.8:1.8}"/>`;
    if(attrs[i].pk){
      s += `<line x1="${ax-62}" y1="${ay+9}" x2="${ax+62}" y2="${ay+9}" stroke="#d9534f" stroke-width="2"/>`;
    }
    s += `<text x="${ax}" y="${ay+7}" font-family="${FONT}" font-size="18" fill="#20344f" text-anchor="middle">${esc(attrs[i].t)}</text>`;
  }
  s += `<text x="30" y="${H-24}" font-family="${FONT}" font-size="16" fill="#666">说明：下划线属性为实体的主键，其余为普通属性。</text>`;
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}">${s}</svg>`;
}

/* ============ 2. 管理员用户管理页面示意图 ============ */
function adminPage(){
  const W=1400, H=880;
  let s = `<rect width="${W}" height="${H}" fill="#ffffff"/>`;
  // 顶部栏
  s += `<rect x="0" y="0" width="${W}" height="64" fill="#409EFF"/>`;
  s += `<text x="28" y="41" font-family="${FONT}" font-size="22" font-weight="bold" fill="#fff">非遗手工艺品展示与预约体验平台 · 后台管理端</text>`;
  s += `<text x="${W-260}" y="41" font-family="${FONT}" font-size="16" fill="#eaf4ff">admin</text>`;
  s += `<text x="${W-95}" y="41" font-family="${FONT}" font-size="16" fill="#eaf4ff">安全退出</text>`;
  // 左侧菜单
  s += `<rect x="0" y="64" width="216" height="${H-64}" fill="#f5f7fa" stroke="#e4e7ed"/>`;
  const menus=['数据总览','预约监管','预约规则配置','用户管理','匠人资质审核','商品管理','课程管理','资讯管理','轮播管理','分类管理','优惠券管理','操作日志','留言反馈'];
  s += `<text x="24" y="98" font-family="${FONT}" font-size="16" font-weight="bold" fill="#606266">功能菜单</text>`;
  menus.forEach((m,i)=>{
    const y = 130 + i*44;
    const active = (m==='用户管理');
    if(active) s += `<rect x="0" y="${y-26}" width="216" height="38" fill="#e6f0fd"/>`;
    s += `<text x="34" y="${y}" font-family="${FONT}" font-size="16" fill="${active?'#409EFF':'#606266'}" font-weight="${active?'bold':'normal'}">${esc(m)}</text>`;
  });
  // 内容区
  const cxx=216;
  s += `<text x="${cxx+34}" y="118" font-family="${FONT}" font-size="22" font-weight="bold" fill="#303133">用户管理</text>`;
  s += `<text x="${cxx+34}" y="146" font-family="${FONT}" font-size="14" fill="#909399">管理平台注册用户的基本信息、角色与账号状态</text>`;
  // 搜索栏
  s += `<rect x="${cxx+34}" y="168" width="180" height="36" fill="#fff" stroke="#dcdfe6" rx="4"/>`;
  s += `<text x="${cxx+50}" y="192" font-family="${FONT}" font-size="14" fill="#c0c4cc">请输入昵称/手机号</text>`;
  s += `<rect x="${cxx+226}" y="168" width="76" height="36" fill="#409EFF" rx="4"/>`;
  s += `<text x="${cxx+248}" y="192" font-family="${FONT}" font-size="14" fill="#fff">搜 索</text>`;
  s += `<rect x="${cxx+314}" y="168" width="76" height="36" fill="#fff" stroke="#dcdfe6" rx="4"/>`;
  s += `<text x="${cxx+336}" y="192" font-family="${FONT}" font-size="14" fill="#606266">重 置</text>`;
  // 表格
  const tx=cxx+34, ty=226, tw=W-tx-40;
  const cols=[{n:'ID',w:70},{n:'昵称',w:120},{n:'手机号',w:150},{n:'角色',w:110},{n:'注册时间',w:180},{n:'状态',w:100},{n:'操作',w:0}];
  s += `<rect x="${tx}" y="${ty}" width="${tw}" height="46" fill="#fafafa" stroke="#ebeef5"/>`;
  let px=tx;
  cols.forEach(c=>{
    const w = c.w || (tw - (px-tx));
    s += `<text x="${px+w/2}" y="${ty+30}" font-family="${FONT}" font-size="15" font-weight="bold" fill="#303133" text-anchor="middle">${esc(c.n)}</text>`;
    if(px>tx) s += `<line x1="${px}" y1="${ty}" x2="${px}" y2="${ty+46}" stroke="#ebeef5"/>`;
    px+=w;
  });
  const rows=[
    ['1','user01','13800138001','普通用户','2026-03-11 09:24','正常'],
    ['2','user02','13800138002','普通用户','2026-03-12 14:07','正常'],
    ['3','zhangjian','13800138003','非遗匠人','2026-03-14 10:31','正常'],
    ['4','limu','13800138004','非遗匠人','2026-03-15 16:52','正常'],
    ['5','wangtao','13800138005','非遗匠人','2026-03-18 08:45','停用'],
  ];
  rows.forEach((r,i)=>{
    const ry = ty+46+i*52;
    s += `<rect x="${tx}" y="${ry}" width="${tw}" height="52" fill="${i%2?'#fafafa':'#fff'}" stroke="#ebeef5"/>`;
    let qx=tx;
    cols.forEach((c,ci)=>{
      const w = c.w || (tw-(qx-tx));
      if(qx>tx) s += `<line x1="${qx}" y1="${ry}" x2="${qx}" y2="${ry+52}" stroke="#ebeef5"/>`;
      const val = (ci < r.length) ? r[ci] : '';
      if(ci===6){
        // 操作按钮
        s += `<rect x="${qx+14}" y="${ry+13}" width="52" height="26" fill="#ecf5ff" stroke="#409EFF" rx="3"/>`;
        s += `<text x="${qx+40}" y="${ry+31}" font-family="${FONT}" font-size="13" fill="#409EFF" text-anchor="middle">编辑</text>`;
        s += `<rect x="${qx+74}" y="${ry+13}" width="52" height="26" fill="#fef0f0" stroke="#f56c6c" rx="3"/>`;
        s += `<text x="${qx+100}" y="${ry+31}" font-family="${FONT}" font-size="13" fill="#f56c6c" text-anchor="middle">删除</text>`;
      } else if(ci===5){
        const ok = (val==='正常');
        s += `<rect x="${qx+w/2-27}" y="${ry+14}" width="54" height="24" fill="${ok?'#f0f9eb':'#fdf6ec'}" stroke="${ok?'#67c23a':'#e6a23c'}" rx="12"/>`;
        s += `<text x="${qx+w/2}" y="${ry+31}" font-family="${FONT}" font-size="13" fill="${ok?'#67c23a':'#e6a23c'}" text-anchor="middle">${esc(val)}</text>`;
      } else {
        s += `<text x="${qx+w/2}" y="${ry+31}" font-family="${FONT}" font-size="14" fill="#606266" text-anchor="middle">${esc(val)}</text>`;
      }
      qx+=w;
    });
  });
  // 分页
  const py = ty+46+rows.length*52+22;
  s += `<text x="${tx}" y="${py+18}" font-family="${FONT}" font-size="14" fill="#606266">共 5 条</text>`;
  s += `<rect x="${tx+420}" y="${py}" width="32" height="32" fill="#fff" stroke="#dcdfe6" rx="4"/><text x="${tx+436}" y="${py+21}" font-family="${FONT}" font-size="13" fill="#606266" text-anchor="middle">&lt;</text>`;
  s += `<rect x="${tx+458}" y="${py}" width="32" height="32" fill="#409EFF" rx="4"/><text x="${tx+474}" y="${py+21}" font-family="${FONT}" font-size="13" fill="#fff" text-anchor="middle">1</text>`;
  s += `<rect x="${tx+496}" y="${py}" width="32" height="32" fill="#fff" stroke="#dcdfe6" rx="4"/><text x="${tx+512}" y="${py+21}" font-family="${FONT}" font-size="13" fill="#606266" text-anchor="middle">&gt;</text>`;
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}">${s}</svg>`;
}

/* ============ 3. 响应时间测试折线/柱状图 ============ */
function perfChart(){
  const W=1180, H=680;
  const padL=110, padR=60, padT=100, padB=120;
  const pw = W-padL-padR, ph = H-padT-padB;
  let s = `<rect width="${W}" height="${H}" fill="#ffffff"/>`;
  s += `<text x="${W/2}" y="46" font-family="${FONT}" font-size="24" font-weight="bold" fill="#1f3a68" text-anchor="middle">系统典型操作响应时间测试结果</text>`;
  s += `<text x="${W/2}" y="74" font-family="${FONT}" font-size="15" fill="#909399" text-anchor="middle">并发用户数 500，单位：毫秒（ms）</text>`;

  const data=[
    {n:'首页首屏加载', v:1200, t:1500},
    {n:'作品列表查询', v:320,  t:500},
    {n:'购物车加入',  v:210,  t:500},
    {n:'预约提交',    v:680,  t:1000},
    {n:'预约审核',    v:360,  t:500},
    {n:'订单查询',    v:280,  t:500},
  ];
  const maxV = 1600;
  // 坐标轴
  s += `<line x1="${padL}" y1="${padT+ph}" x2="${padL+pw}" y2="${padT+ph}" stroke="#333" stroke-width="2"/>`;
  s += `<line x1="${padL}" y1="${padT}" x2="${padL}" y2="${padT+ph}" stroke="#333" stroke-width="2"/>`;
  // y 轴刻度
  for(let i=0;i<=8;i++){
    const v = maxV/8*i;
    const y = padT+ph - ph*i/8;
    s += `<line x1="${padL-6}" y1="${y}" x2="${padL}" y2="${y}" stroke="#333" stroke-width="1.5"/>`;
    s += `<text x="${padL-14}" y="${y+5}" font-family="${FONT}" font-size="13" fill="#606266" text-anchor="end">${v}</text>`;
    if(i>0) s += `<line x1="${padL}" y1="${y}" x2="${padL+pw}" y2="${y}" stroke="#eee" stroke-width="1"/>`;
  }
  s += `<text x="${padL-70}" y="${padT-24}" font-family="${FONT}" font-size="14" fill="#606266">响应时间(ms)</text>`;

  const n=data.length;
  const slot = pw/n;
  const bw = 62;
  const colors=['#5B8FF9','#5AD8A6','#F6BD16','#E8684A','#6DC8EC','#9270CA'];
  data.forEach((d,i)=>{
    const cx = padL + slot*i + slot/2;
    const h = ph*d.v/maxV;
    const y = padT+ph-h;
    // 目标线（虚线小柱）
    const th = ph*d.t/maxV;
    const ty = padT+ph-th;
    s += `<line x1="${cx-bw/2-14}" y1="${ty}" x2="${cx+bw/2+14}" y2="${ty}" stroke="#E8684A" stroke-width="2" stroke-dasharray="6,4"/>`;
    // 实际柱
    s += `<rect x="${cx-bw/2}" y="${y}" width="${bw}" height="${h}" fill="${colors[i%colors.length]}" rx="4"/>`;
    // 数值
    s += `<text x="${cx}" y="${y-10}" font-family="${FONT}" font-size="16" font-weight="bold" fill="#303133" text-anchor="middle">${d.v}</text>`;
    // x 标签
    s += `<text x="${cx}" y="${padT+ph+28}" font-family="${FONT}" font-size="14" fill="#303133" text-anchor="middle">${esc(d.n)}</text>`;
    // 目标值标注
    s += `<text x="${cx+bw/2+22}" y="${ty+5}" font-family="${FONT}" font-size="12" fill="#E8684A">${d.t}</text>`;
  });

  // 图例
  const ly = H-42;
  s += `<rect x="${padL}" y="${ly-13}" width="18" height="14" fill="#5B8FF9"/>`;
  s += `<text x="${padL+26}" y="${ly}" font-family="${FONT}" font-size="14" fill="#606266">实际平均响应时间</text>`;
  s += `<line x1="${padL+220}" y1="${ly-6}" x2="${padL+250}" y2="${ly-6}" stroke="#E8684A" stroke-width="2" stroke-dasharray="6,4"/>`;
  s += `<text x="${padL+258}" y="${ly}" font-family="${FONT}" font-size="14" fill="#606266">预期指标上限</text>`;
  s += `<text x="${W-padR}" y="${ly}" font-family="${FONT}" font-size="13" fill="#909399" text-anchor="end">测试工具：JMeter 5.6 · 数据来源：本系统压测报告</text>`;
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}">${s}</svg>`;
}

(async ()=>{
  const figs = {
    'fig3-5_goods_er': erGoods(),
    'fig5-1_admin_user': adminPage(),
    'fig5-2_perf': perfChart(),
  };
  for(const [name, svg] of Object.entries(figs)){
    const out = `report_figs/${name}`;
    fs.writeFileSync(out+'.svg', svg);
    await sharp(Buffer.from(svg)).png().toFile(out+'.png');
    console.log('ok', out+'.png');
  }
})();
