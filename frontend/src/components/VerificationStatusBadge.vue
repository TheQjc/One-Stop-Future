<script setup>
import { computed } from "vue";

const props = defineProps({
  status: {
    type: String,
    default: "UNVERIFIED",
  },
});

const statusMap = {
  UNVERIFIED: { label: "未认证", className: "unverified" },
  PENDING: { label: "审核中", className: "pending" },
  VERIFIED: { label: "已认证", className: "verified" },
};

const viewModel = computed(() => statusMap[props.status] || statusMap.UNVERIFIED);
</script>

<template>
  <span class="verification-badge" :class="viewModel.className">
    {{ viewModel.label }}
  </span>
</template>

<style scoped>
.verification-badge {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  border-radius: var(--cp-radius-pill);
  border: 1px solid transparent;
  font-size: var(--cp-text-sm);
  font-weight: 600;
}

.verification-badge.unverified {
  background: rgba(24, 38, 63, 0.06);
  color: var(--cp-ink-soft);
}

.verification-badge.pending {
  background: rgba(184, 130, 35, 0.14);
  color: var(--cp-warning);
}

.verification-badge.verified {
  background: rgba(76, 122, 116, 0.14);
  color: var(--cp-teal-deep);
}
</style>
