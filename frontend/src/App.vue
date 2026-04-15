<script setup>
import NavBar from "./components/NavBar.vue";
import PageFooter from "./components/PageFooter.vue";
</script>

<template>
  <div class="app-shell">
    <div class="ledger-grid" aria-hidden="true" />
    <div class="ledger-orb ledger-orb-left" aria-hidden="true" />
    <div class="ledger-orb ledger-orb-right" aria-hidden="true" />
    <div class="paper-glow paper-glow-left" />
    <div class="paper-glow paper-glow-right" />
    <NavBar />
    <main class="shell-main">
      <router-view />
    </main>
    <PageFooter />
  </div>
</template>

<style scoped>
.ledger-grid,
.ledger-orb {
  pointer-events: none;
  position: fixed;
  inset: 0;
}

.ledger-grid {
  background-image:
    linear-gradient(rgba(24, 38, 63, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(24, 38, 63, 0.03) 1px, transparent 1px);
  background-size: 64px 64px;
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.4), transparent 88%);
}

.ledger-orb {
  filter: blur(18px);
  opacity: 0.7;
}

.ledger-orb-left {
  inset: auto auto 12% -8%;
  width: 22rem;
  height: 22rem;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(76, 122, 116, 0.18), transparent 72%);
}

.ledger-orb-right {
  inset: 8% -10% auto auto;
  width: 28rem;
  height: 28rem;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(197, 79, 45, 0.12), transparent 70%);
}
</style>
