<template>
  <div class="forbidden">
    <div class="box">
      <div class="code">403</div>
      <div class="text">没有访问权限</div>
      <div class="sub">当前账号的角色无法访问该页面，请切换对应角色的账号。</div>
      <div class="btns">
        <el-button type="primary" @click="$router.replace('/')">回首页</el-button>
        <el-button @click="$router.back()">返回上一页</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.forbidden { min-height: 70vh; display: flex; align-items: center; justify-content: center; }
.box { text-align: center; }
.code { font-size: 84px; font-weight: 800; color: var(--h-primary); opacity: 0.85; line-height: 1; }
.text { font-size: 20px; font-weight: 600; margin-top: 10px; }
.sub { color: var(--h-text-second); font-size: 13px; margin: 8px 0 20px; }
.btns { display: flex; gap: 12px; justify-content: center; }
</style>
