<template>
  <footer class="site-footer">
    <div class="site-footer__inner">
      <div>
        <p class="site-footer__title">校园成长服务入口 | One-Stop Future</p>
        <p class="site-footer__copy">
          围绕就业、考研与留学决策搭建统一入口，当前阶段先完成首页聚合、手机号登录、个人中心、通知与认证审核骨架。
        </p>
      </div>
      <div class="site-footer__meta">
        <span>技术栈：Vue 3 + Pinia + Vue Router + Axios + Vite</span>
        <span>视觉方向：编辑部式学生决策台</span>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.site-footer {
  border-top: 1px solid var(--cp-line);
  background: rgba(255, 250, 244, 0.72);
}

.site-footer__inner {
  width: min(calc(100% - 32px), var(--cp-max-width));
  margin: 0 auto;
  padding: 28px 0 36px;
  display: flex;
  justify-content: space-between;
  gap: var(--cp-gap-5);
  color: var(--cp-ink-soft);
}

.site-footer__title {
  margin: 0 0 6px;
  color: var(--cp-ink);
  font-weight: 700;
}

.site-footer__copy,
.site-footer__meta {
  margin: 0;
  font-size: var(--cp-text-sm);
}

.site-footer__meta {
  display: grid;
  gap: 4px;
  text-align: right;
}

@media (max-width: 767px) {
  .site-footer__inner {
    width: min(calc(100% - 20px), var(--cp-max-width));
    flex-direction: column;
  }

  .site-footer__meta {
    text-align: left;
  }
}
</style>
