<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { RouterLink, useRouter } from "vue-router";
import {
  getLatestDecisionResult,
  listDecisionQuestions,
  submitDecisionAnswers,
} from "../api/decision.js";

const router = useRouter();

const loading = reactive({
  questions: true,
  latest: true,
  submit: false,
});

const error = reactive({
  questions: "",
  latest: "",
  submit: "",
});

const questionSet = ref({ questions: [] });
const latest = ref(null);
const result = ref(null);

const answers = ref(new Map());

const questionIds = computed(() => questionSet.value.questions.map((q) => q.id));
const totalQuestions = computed(() => questionIds.value.length);
const answeredCount = computed(() => answers.value.size);
const isComplete = computed(() => totalQuestions.value > 0 && answeredCount.value === totalQuestions.value);

const showLatestBlock = computed(() => (
  latest.value?.hasResult
  && !result.value
  && answeredCount.value === 0
));

function optionIdFor(questionId) {
  return answers.value.get(questionId) ?? "";
}

function setAnswer(questionId, optionId) {
  const next = new Map(answers.value);
  next.set(questionId, Number(optionId));
  answers.value = next;
}

function resetForm() {
  answers.value = new Map();
  result.value = null;
  error.submit = "";
}

async function loadQuestions() {
  loading.questions = true;
  error.questions = "";

  try {
    questionSet.value = await listDecisionQuestions();
  } catch (e) {
    error.questions = e.message || "Decision questions failed to load. Please retry.";
  } finally {
    loading.questions = false;
  }
}

async function loadLatest() {
  loading.latest = true;
  error.latest = "";

  try {
    latest.value = await getLatestDecisionResult();
  } catch (e) {
    error.latest = e.message || "Latest assessment result failed to load.";
    latest.value = null;
  } finally {
    loading.latest = false;
  }
}

async function loadAll() {
  await Promise.all([loadQuestions(), loadLatest()]);
}

function buildSubmitPayload() {
  const ordered = questionSet.value.questions
    .slice()
    .sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0))
    .map((q) => ({ questionId: q.id, optionId: answers.value.get(q.id) }));

  return { answers: ordered };
}

async function submit() {
  if (!isComplete.value || loading.submit) {
    return;
  }

  loading.submit = true;
  error.submit = "";

  try {
    result.value = await submitDecisionAnswers(buildSubmitPayload());
  } catch (e) {
    error.submit = e.message || "Submission failed. Please try again.";
  } finally {
    loading.submit = false;
  }
}

function goTo(path) {
  if (!path) {
    return;
  }
  router.push(path);
}

onMounted(loadAll);
</script>

<template>
  <section class="page-stack">
    <article class="section-card assessment-hero">
      <div class="assessment-hero__copy">
        <span class="section-eyebrow">Decision Desk</span>
        <h1 class="hero-title" style="margin-top: 18px;">Assessment</h1>
        <hr class="editorial-rule" />
        <p class="hero-copy">
          Answer each prompt once. The desk will convert your choices into a single recommended track,
          plus a ranked score snapshot that you can carry into the timeline view.
        </p>
      </div>

      <div class="assessment-hero__panel">
        <div class="panel-card assessment-hero__stat">
          <span class="assessment-hero__label">Questions</span>
          <strong>{{ totalQuestions }}</strong>
        </div>
        <div class="panel-card assessment-hero__stat">
          <span class="assessment-hero__label">Answered</span>
          <strong>{{ answeredCount }}/{{ totalQuestions }}</strong>
        </div>
        <button type="button" class="ghost-btn" :disabled="loading.submit" @click="resetForm">
          Reset
        </button>
      </div>
    </article>

    <article v-if="showLatestBlock" class="section-card latest-block">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">Latest Snapshot</span>
          <h2 class="page-title" style="margin-top: 16px;">
            Recommended Track: {{ latest.recommendedTrack }}
          </h2>
          <p v-if="latest.summaryText" class="page-subtitle" style="margin-top: 16px;">
            {{ latest.summaryText }}
          </p>
        </div>
      </div>
      <div class="action-row">
        <RouterLink to="/timeline" class="app-link">Open Timeline</RouterLink>
        <button type="button" class="ghost-btn" @click="resetForm">
          Start New Submission
        </button>
      </div>
    </article>

    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">Question Set</span>
          <h2 class="page-title" style="margin-top: 16px;">Select one option per question.</h2>
          <p class="page-subtitle" style="margin-top: 16px;">
            Submit stays locked until every active question has a recorded answer.
          </p>
        </div>
      </div>

      <div v-if="loading.questions" class="empty-state">Loading assessment questions...</div>
      <div v-else-if="error.questions" class="field-grid">
        <p class="field-error" role="alert">{{ error.questions }}</p>
        <button type="button" class="ghost-btn" @click="loadQuestions">
          Retry
        </button>
      </div>
      <div v-else-if="!questionSet.questions.length" class="empty-state">
        No questions are configured yet. Please try again later.
      </div>
      <form v-else class="question-stack" @submit.prevent="submit">
        <fieldset
          v-for="question in questionSet.questions"
          :key="question.id"
          class="question-card"
        >
          <legend class="question-card__legend">
            <span class="question-card__code">{{ question.code }}</span>
            <span class="question-card__prompt">{{ question.prompt }}</span>
          </legend>
          <p v-if="question.description" class="muted-copy">{{ question.description }}</p>

          <div class="option-grid">
            <label
              v-for="option in question.options"
              :key="option.id"
              class="option-card"
              :class="{ selected: String(optionIdFor(question.id)) === String(option.id) }"
            >
              <input
                class="sr-only"
                type="radio"
                :name="`q-${question.id}`"
                :value="option.id"
                :checked="String(optionIdFor(question.id)) === String(option.id)"
                @change="setAnswer(question.id, $event.target.value)"
              />
              <div class="option-card__top">
                <strong class="option-card__label">{{ option.label }}</strong>
                <span class="option-card__code">{{ option.code }}</span>
              </div>
              <p v-if="option.description" class="option-card__desc">
                {{ option.description }}
              </p>
            </label>
          </div>
        </fieldset>

        <div class="inline-form-actions">
          <button
            type="submit"
            class="app-btn"
            data-test="assessment-submit"
            :disabled="!isComplete || loading.submit"
          >
            {{ loading.submit ? "Submitting..." : "Submit Assessment" }}
          </button>
          <span class="meta-copy">
            {{ isComplete ? "Ready to submit." : "Select answers for every question to unlock submission." }}
          </span>
        </div>

        <p v-if="error.submit" class="field-error" role="alert">{{ error.submit }}</p>
      </form>
    </article>

    <article v-if="result?.hasResult" class="section-card result-card" data-test="assessment-result">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">Result Desk</span>
          <h2 class="page-title" style="margin-top: 16px;">
            Recommended Track: {{ result.recommendedTrack }}
          </h2>
          <p v-if="result.summaryText" class="page-subtitle" style="margin-top: 16px;">
            {{ result.summaryText }}
          </p>
        </div>
      </div>

      <div class="result-grid">
        <div class="panel-card result-panel">
          <p class="result-panel__label">Scores</p>
          <dl class="score-list" v-if="result.scores">
            <div class="score-row">
              <dt>CAREER</dt>
              <dd>{{ result.scores.career }}</dd>
            </div>
            <div class="score-row">
              <dt>EXAM</dt>
              <dd>{{ result.scores.exam }}</dd>
            </div>
            <div class="score-row">
              <dt>ABROAD</dt>
              <dd>{{ result.scores.abroad }}</dd>
            </div>
          </dl>
          <p v-else class="muted-copy">Score snapshot unavailable.</p>
        </div>

        <div class="panel-card result-panel">
          <p class="result-panel__label">Ranking</p>
          <ol class="ranking-list" v-if="result.ranking?.length">
            <li v-for="item in result.ranking" :key="item.track">
              <span class="ranking-track">{{ item.track }}</span>
              <span class="ranking-score">{{ item.score }}</span>
            </li>
          </ol>
          <p v-else class="muted-copy">Ranking unavailable.</p>
        </div>
      </div>

      <div class="action-row" style="margin-top: 28px;">
        <button
          v-for="action in result.nextActions || []"
          :key="action.code || action.path"
          type="button"
          class="app-link"
          @click="goTo(action.path)"
        >
          {{ action.label || "Next" }}
        </button>
        <RouterLink to="/timeline" class="ghost-btn">Open Timeline</RouterLink>
      </div>
    </article>

    <article v-if="error.latest" class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">Latest Snapshot</span>
          <h2 class="page-title" style="margin-top: 16px;">Latest result unavailable</h2>
          <p class="page-subtitle" style="margin-top: 16px;">{{ error.latest }}</p>
        </div>
      </div>
      <button type="button" class="ghost-btn" :disabled="loading.latest" @click="loadLatest">
        Retry latest lookup
      </button>
    </article>
  </section>
</template>

<style scoped>
.assessment-hero {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: var(--cp-gap-6);
  background:
    linear-gradient(180deg, rgba(255, 252, 247, 0.94), rgba(245, 237, 223, 0.98)),
    radial-gradient(circle at top left, rgba(24, 38, 63, 0.12), transparent 36%),
    radial-gradient(circle at bottom right, rgba(197, 79, 45, 0.14), transparent 40%);
}

.assessment-hero__copy,
.assessment-hero__panel {
  display: grid;
  gap: var(--cp-gap-4);
}

.assessment-hero__panel {
  align-content: end;
}

.assessment-hero__stat {
  min-height: 132px;
  display: grid;
  gap: 8px;
  align-content: end;
}

.assessment-hero__label {
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.assessment-hero__stat strong {
  font-size: clamp(26px, 4vw, 34px);
  font-family: var(--cp-font-display);
}

.question-stack {
  display: grid;
  gap: var(--cp-gap-6);
}

.question-card {
  margin: 0;
  padding: 0;
  border: 0;
  display: grid;
  gap: var(--cp-gap-4);
}

.question-card__legend {
  display: grid;
  gap: 8px;
  font-weight: 700;
  font-family: var(--cp-font-display);
  font-size: var(--cp-text-xl);
  line-height: 1.14;
  margin: 0;
}

.question-card__code {
  font-family: var(--cp-font-mono);
  font-size: var(--cp-text-sm);
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--cp-ink-soft);
}

.option-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cp-gap-4);
}

.option-card {
  display: grid;
  gap: 10px;
  padding: 18px;
  border-radius: var(--cp-radius-md);
  border: 1px solid rgba(24, 38, 63, 0.14);
  background: rgba(255, 255, 255, 0.72);
  cursor: pointer;
  transition:
    transform var(--cp-transition),
    border-color var(--cp-transition),
    background-color var(--cp-transition),
    box-shadow var(--cp-transition);
}

.option-card:hover {
  transform: translateY(-1px);
  border-color: rgba(197, 79, 45, 0.32);
}

.option-card.selected {
  border-color: rgba(197, 79, 45, 0.6);
  box-shadow: 0 18px 36px rgba(197, 79, 45, 0.12);
  background: rgba(255, 250, 245, 0.92);
}

.option-card__top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.option-card__label {
  color: var(--cp-ink);
}

.option-card__code {
  font-family: var(--cp-font-mono);
  font-size: var(--cp-text-sm);
  color: var(--cp-ink-soft);
}

.option-card__desc {
  margin: 0;
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.result-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cp-gap-4);
}

.result-panel {
  min-height: 164px;
  display: grid;
  gap: var(--cp-gap-3);
  align-content: start;
}

.result-panel__label {
  margin: 0;
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.score-list {
  margin: 0;
  display: grid;
  gap: 12px;
}

.score-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--cp-line);
}

.score-row dt {
  color: var(--cp-ink-soft);
  font-family: var(--cp-font-mono);
}

.score-row dd {
  margin: 0;
  font-family: var(--cp-font-display);
  font-size: var(--cp-text-xl);
}

.ranking-list {
  margin: 0;
  padding-left: 18px;
  display: grid;
  gap: 10px;
  color: var(--cp-ink);
}

.ranking-list li {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.ranking-track {
  font-family: var(--cp-font-mono);
  letter-spacing: 0.04em;
}

.ranking-score {
  font-family: var(--cp-font-display);
}

@media (max-width: 1023px) {
  .assessment-hero,
  .option-grid,
  .result-grid {
    grid-template-columns: 1fr;
  }
}
</style>

