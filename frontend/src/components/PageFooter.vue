<template>
  <footer class="site-footer">
    <div class="site-footer__inner">
      <div>
        <p class="site-footer__title">校园成长服务入口 | One-Stop Future</p>
        <p class="site-footer__copy">
          聚合就业机会、学习资源、校园社区与成长档案，为学生提供清晰、可靠的一站式成长服务。
        </p>
      </div>
      <div class="site-footer__meta">
        <span>服务方向：就业发展 · 升学规划 · 资源共享</span>
        <span>© 2026 One-Stop Future. All rights reserved.</span>
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
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: var(--cp-gap-5);
  align-items: center;
  color: var(--cp-ink-soft);
}

.site-footer__title {
  margin: 0 0 6px;
  color: var(--cp-ink);
  font-weight: 700;
  letter-spacing: 0;
}

.site-footer__copy,
.site-footer__meta {
  margin: 0;
  font-size: var(--cp-text-sm);
}

.site-footer__copy {
  max-width: 680px;
}

.site-footer__meta {
  display: grid;
  gap: 4px;
  text-align: right;
  white-space: nowrap;
  color: var(--cp-ink-faint);
}

@media (max-width: 767px) {
  .site-footer__inner {
    width: min(calc(100% - 20px), var(--cp-max-width));
    grid-template-columns: 1fr;
    align-items: start;
  }

  .site-footer__meta {
    text-align: left;
    white-space: normal;
  }
}
</style>
