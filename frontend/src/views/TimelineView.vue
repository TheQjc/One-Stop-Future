<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { RouterLink, useRoute } from "vue-router";
import { getDecisionTimeline, getLatestDecisionResult } from "../api/decision.js";

const route = useRoute();

const loading = ref({
  latest: true,
  timeline: false,
});
const errorMessage = ref("");
const latest = ref(null);
const timeline = ref(null);
const activeTrack = ref("EXAM");
const initialized = ref(false);

const supportedTracks = [
  { code: "CAREER", label: "Career" },
  { code: "EXAM", label: "Exam" },
  { code: "ABROAD", label: "Abroad" },
];

const explicitAnchorDate = computed(() => {
  const raw = route.query?.anchorDate;
  return typeof raw === "string" ? raw.trim() : "";
});

const canFetchTimeline = computed(() => (
  Boolean(explicitAnchorDate.value)
  || Boolean(latest.value?.hasResult)
));

const timelineItems = computed(() => timeline.value?.items ?? []);

const assessmentRequired = computed(() => (
  !canFetchTimeline.value
  || Boolean(timeline.value?.assessmentRequired)
));

const hasLatestError = computed(() => (
  Boolean(errorMessage.value)
  && !latest.value
  && !explicitAnchorDate.value
));

async function loadLatest() {
  loading.value.latest = true;
  errorMessage.value = "";

  try {
    latest.value = await getLatestDecisionResult();
    if (latest.value?.hasResult && latest.value.recommendedTrack) {
      activeTrack.value = latest.value.recommendedTrack;
    }
  } catch (e) {
    errorMessage.value = e.message || "Latest assessment lookup failed.";
    latest.value = null;
  } finally {
    loading.value.latest = false;
  }
}

async function loadTimeline() {
  if (!canFetchTimeline.value) {
    timeline.value = null;
    return;
  }

  loading.value.timeline = true;
  errorMessage.value = "";

  try {
    timeline.value = await getDecisionTimeline({
      track: activeTrack.value,
      ...(explicitAnchorDate.value ? { anchorDate: explicitAnchorDate.value } : {}),
    });
  } catch (e) {
    errorMessage.value = e.message || "Timeline failed to load. Please retry.";
    timeline.value = null;
  } finally {
    loading.value.timeline = false;
  }
}

function selectTrack(track) {
  if (activeTrack.value === track) {
    return;
  }
  activeTrack.value = track;
}

async function retry() {
  if (hasLatestError.value) {
    await loadLatest();
  }
  await loadTimeline();
}

watch([activeTrack, explicitAnchorDate], async ([nextTrack, nextAnchor], [prevTrack, prevAnchor]) => {
  if (!initialized.value) {
    return;
  }
  if (nextTrack === prevTrack && nextAnchor === prevAnchor) {
    return;
  }
  await loadTimeline();
});

onMounted(async () => {
  await loadLatest();
  await loadTimeline();
  initialized.value = true;
});
</script>

<template>
  <section class="page-stack">
    <article class="section-card timeline-hero">
      <div class="timeline-hero__copy">
        <span class="section-eyebrow">Decision Desk</span>
        <h1 class="hero-title" style="margin-top: 18px;">Timeline</h1>
        <hr class="editorial-rule" />
        <p class="hero-copy">
          The timeline is anchored to your latest assessment session date, unless you provide an explicit anchor.
          Track tabs switch the plan without the backend guessing your direction.
        </p>
      </div>

      <div class="timeline-hero__panel">
        <div class="panel-card timeline-hero__stat">
          <span class="timeline-hero__label">Anchor</span>
          <strong>{{ timeline?.anchorDate || explicitAnchorDate || "Assessment" }}</strong>
        </div>
        <div class="panel-card timeline-hero__stat">
          <span class="timeline-hero__label">Active Track</span>
          <strong>{{ activeTrack }}</strong>
        </div>
        <RouterLink to="/assessment" class="app-link">Open Assessment</RouterLink>
      </div>
    </article>

    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">Tracks</span>
          <h2 class="page-title" style="margin-top: 16px;">Switch the track, keep the anchor stable.</h2>
          <p class="page-subtitle" style="margin-top: 16px;">
            Tabs stay interactive, but the desk will not fetch milestones until an anchor exists.
          </p>
        </div>
      </div>

      <div class="track-tabs" role="tablist" aria-label="Timeline track tabs">
        <button
          v-for="track in supportedTracks"
          :key="track.code"
          type="button"
          class="track-tab"
          role="tab"
          :aria-selected="activeTrack === track.code"
          :class="{ active: activeTrack === track.code }"
          @click="selectTrack(track.code)"
        >
          {{ track.label }}
        </button>
      </div>

      <div v-if="loading.latest" class="empty-state">Loading latest assessment snapshot...</div>
      <div v-else-if="errorMessage" class="field-grid">
        <p class="field-error" role="alert">{{ errorMessage }}</p>
        <button type="button" class="ghost-btn" :disabled="loading.timeline || loading.latest" @click="retry">
          Retry
        </button>
      </div>
      <div v-else-if="assessmentRequired" class="empty-state assessment-required" data-test="timeline-empty">
        <h3 class="assessment-required__title">Complete the assessment first</h3>
        <p class="assessment-required__copy">
          The timeline needs an anchor. Submit the assessment to generate a session date, then return here.
        </p>
        <RouterLink to="/assessment" class="app-btn">Go to Assessment</RouterLink>
      </div>
      <div v-else-if="canFetchTimeline && !timeline" class="empty-state">
        Loading timeline milestones...
      </div>
      <div v-else-if="loading.timeline" class="empty-state">
        Loading timeline milestones...
      </div>
      <div v-else-if="!timelineItems.length" class="empty-state">
        No milestones have been configured for this track yet.
      </div>
      <div v-else class="milestone-grid" data-test="timeline-items">
        <article
          v-for="item in timelineItems"
          :key="item.phaseCode"
          class="panel-card milestone-card"
        >
          <div class="milestone-card__meta">
            <span class="milestone-card__phase">
              {{ item.phaseCode }} / {{ item.phaseLabel }}
            </span>
            <span class="milestone-card__date">
              {{ item.targetDate }}
              <span class="milestone-card__days">
                / {{ item.remainingDays }} days
              </span>
            </span>
          </div>
          <h3 class="milestone-card__title">{{ item.title }}</h3>
          <p class="milestone-card__summary">{{ item.summary }}</p>

          <ul v-if="item.actionChecklist?.length" class="milestone-card__checklist">
            <li v-for="line in item.actionChecklist" :key="line">
              {{ line }}
            </li>
          </ul>

          <p v-if="item.resourceHint" class="milestone-card__hint">
            {{ item.resourceHint }}
          </p>
        </article>
      </div>
    </article>
  </section>
</template>

<style scoped>
.timeline-hero {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: var(--cp-gap-6);
  background:
    linear-gradient(180deg, rgba(255, 252, 247, 0.94), rgba(245, 237, 223, 0.98)),
    radial-gradient(circle at top left, rgba(76, 122, 116, 0.16), transparent 34%),
    radial-gradient(circle at bottom right, rgba(24, 38, 63, 0.1), transparent 40%);
}

.timeline-hero__copy,
.timeline-hero__panel {
  display: grid;
  gap: var(--cp-gap-4);
}

.timeline-hero__panel {
  align-content: end;
}

.timeline-hero__stat {
  min-height: 132px;
  display: grid;
  gap: 8px;
  align-content: end;
}

.timeline-hero__label {
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.timeline-hero__stat strong {
  font-size: clamp(22px, 3vw, 30px);
  font-family: var(--cp-font-display);
  overflow-wrap: anywhere;
}

.track-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cp-gap-3);
  margin-bottom: var(--cp-gap-6);
}

.track-tab {
  min-height: var(--cp-touch-height);
  padding: 0 16px;
  border-radius: var(--cp-radius-pill);
  border: 1px solid var(--cp-line-strong);
  background: rgba(255, 255, 255, 0.72);
  color: var(--cp-ink-soft);
  cursor: pointer;
  transition:
    transform var(--cp-transition),
    border-color var(--cp-transition),
    color var(--cp-transition),
    background-color var(--cp-transition);
}

.track-tab:hover {
  transform: translateY(-1px);
  border-color: rgba(197, 79, 45, 0.3);
  color: var(--cp-ink);
}

.track-tab.active {
  background: rgba(255, 250, 245, 0.9);
  border-color: rgba(197, 79, 45, 0.6);
  color: var(--cp-accent-deep);
}

.assessment-required {
  display: grid;
  gap: 12px;
  justify-items: center;
}

.assessment-required__title {
  margin: 0;
  font-family: var(--cp-font-display);
  font-size: var(--cp-text-xl);
  color: var(--cp-ink);
}

.assessment-required__copy {
  margin: 0;
  max-width: 52ch;
}

.milestone-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cp-gap-4);
}

.milestone-card {
  display: grid;
  gap: 12px;
  min-height: 100%;
}

.milestone-card__meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
}

.milestone-card__phase {
  font-family: var(--cp-font-mono);
  font-size: var(--cp-text-sm);
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--cp-accent-deep);
}

.milestone-card__date {
  font-size: var(--cp-text-sm);
  color: var(--cp-ink-soft);
}

.milestone-card__days {
  font-family: var(--cp-font-mono);
}

.milestone-card__title {
  margin: 0;
  font-family: var(--cp-font-display);
  font-size: 22px;
  line-height: 1.2;
}

.milestone-card__summary {
  margin: 0;
  color: var(--cp-ink-soft);
}

.milestone-card__checklist {
  margin: 0;
  padding-left: 18px;
  display: grid;
  gap: 8px;
}

.milestone-card__hint {
  margin: 0;
  padding: 14px 16px;
  border-radius: var(--cp-radius-sm);
  background: rgba(76, 122, 116, 0.1);
  border: 1px solid rgba(76, 122, 116, 0.18);
  color: var(--cp-teal-deep);
  font-size: var(--cp-text-sm);
}

@media (max-width: 1023px) {
  .timeline-hero,
  .milestone-grid {
    grid-template-columns: 1fr;
  }
}
</style>
