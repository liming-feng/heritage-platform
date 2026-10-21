<template>
  <div class="notfound">
    <el-result icon="warning" title="404" sub-title="页面走丢了，可能是地址输错了">
      <template #extra>
        <el-button type="primary" @click="$router.replace('/')">返回首页</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup></script>

<style scoped>
.notfound {
  min-height: 70vh;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
