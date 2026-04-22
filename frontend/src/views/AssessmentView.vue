<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { RouterLink, useRouter } from "vue-router";
import {
  getLatestDecisionResult,
  listDecisionQuestions,
  submitDecisionAnswers,
} from "../api/decision.js";

const router = useRouter();

const trackLabelMap = {
  CAREER: "就业",
  EXAM: "考研",
  ABROAD: "留学",
};

const actionLabelMap = {
  TIMELINE: "查看时间线",
  OPEN_TIMELINE: "查看时间线",
  START_ASSESSMENT: "开始测评",
  COMPARE_SCHOOLS: "对比院校",
};

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
    error.questions = e.message || "测评题目加载失败，请重试。";
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
    error.latest = e.message || "最近测评结果加载失败。";
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
    error.submit = e.message || "提交失败，请稍后重试。";
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

function displayTrack(track) {
  return trackLabelMap[track] || track || "未确定";
}

function displayActionLabel(action) {
  if (!action) {
    return "查看下一步";
  }

  return actionLabelMap[action.code]
    || (action.path === "/timeline" ? "查看时间线" : "")
    || (action.path === "/assessment" ? "开始测评" : "")
    || (action.path === "/schools/compare" ? "对比院校" : "")
    || action.label
    || "查看下一步";
}

onMounted(loadAll);
</script>

<template>
  <section class="page-stack">
    <article class="section-card assessment-hero">
      <div class="assessment-hero__copy">
        <span class="section-eyebrow">决策支持</span>
        <h1 class="hero-title" style="margin-top: 18px;">方向测评</h1>
        <hr class="editorial-rule" />
        <p class="hero-copy">
          逐题完成选择后，系统会给出当前更适合你的成长方向，并生成一份可带入时间线页继续查看的分数快照。
        </p>
      </div>

      <div class="assessment-hero__panel">
        <div class="panel-card assessment-hero__stat">
          <span class="assessment-hero__label">题目数</span>
          <strong>{{ totalQuestions }}</strong>
        </div>
        <div class="panel-card assessment-hero__stat">
          <span class="assessment-hero__label">已完成</span>
          <strong>{{ answeredCount }}/{{ totalQuestions }}</strong>
        </div>
        <button type="button" class="ghost-btn" :disabled="loading.submit" @click="resetForm">
          重新填写
        </button>
      </div>
    </article>

    <article v-if="showLatestBlock" class="section-card latest-block">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">最近结果</span>
          <h2 class="page-title" style="margin-top: 16px;">
            推荐方向：{{ displayTrack(latest.recommendedTrack) }}
          </h2>
          <p v-if="latest.summaryText" class="page-subtitle" style="margin-top: 16px;">
            {{ latest.summaryText }}
          </p>
        </div>
      </div>
      <div class="action-row">
        <RouterLink to="/timeline" class="app-link">查看时间线</RouterLink>
        <button type="button" class="ghost-btn" @click="resetForm">
          重新开始
        </button>
      </div>
    </article>

    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">测评题目</span>
          <h2 class="page-title" style="margin-top: 16px;">每题选择一个最符合你的选项。</h2>
          <p class="page-subtitle" style="margin-top: 16px;">
            需要完成全部题目作答后，才可以提交测评。
          </p>
        </div>
      </div>

      <div v-if="loading.questions" class="empty-state">正在加载测评题目...</div>
      <div v-else-if="error.questions" class="field-grid">
        <p class="field-error" role="alert">{{ error.questions }}</p>
        <button type="button" class="ghost-btn" @click="loadQuestions">
          重试
        </button>
      </div>
      <div v-else-if="!questionSet.questions.length" class="empty-state">
        暂未配置测评题目，请稍后再试。
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
            {{ loading.submit ? "提交中..." : "提交测评" }}
          </button>
          <span class="meta-copy">
            {{ isComplete ? "可以提交了。" : "完成全部题目后即可提交。" }}
          </span>
        </div>

        <p v-if="error.submit" class="field-error" role="alert">{{ error.submit }}</p>
      </form>
    </article>

    <article v-if="result?.hasResult" class="section-card result-card" data-test="assessment-result">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">测评结果</span>
          <h2 class="page-title" style="margin-top: 16px;">
            推荐方向：{{ displayTrack(result.recommendedTrack) }}
          </h2>
          <p v-if="result.summaryText" class="page-subtitle" style="margin-top: 16px;">
            {{ result.summaryText }}
          </p>
        </div>
      </div>

      <div class="result-grid">
        <div class="panel-card result-panel">
          <p class="result-panel__label">评分概览</p>
          <dl class="score-list" v-if="result.scores">
            <div class="score-row">
              <dt>就业</dt>
              <dd>{{ result.scores.career }}</dd>
            </div>
            <div class="score-row">
              <dt>考研</dt>
              <dd>{{ result.scores.exam }}</dd>
            </div>
            <div class="score-row">
              <dt>留学</dt>
              <dd>{{ result.scores.abroad }}</dd>
            </div>
          </dl>
          <p v-else class="muted-copy">暂未生成分数快照。</p>
        </div>

        <div class="panel-card result-panel">
          <p class="result-panel__label">方向排序</p>
          <ol class="ranking-list" v-if="result.ranking?.length">
            <li v-for="item in result.ranking" :key="item.track">
              <span class="ranking-track">{{ displayTrack(item.track) }}</span>
              <span class="ranking-score">{{ item.score }}</span>
            </li>
          </ol>
          <p v-else class="muted-copy">暂未生成排序结果。</p>
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
          {{ displayActionLabel(action) }}
        </button>
        <RouterLink to="/timeline" class="ghost-btn">查看时间线</RouterLink>
      </div>
    </article>

    <article v-if="error.latest" class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">最近结果</span>
          <h2 class="page-title" style="margin-top: 16px;">最近结果暂不可用</h2>
          <p class="page-subtitle" style="margin-top: 16px;">{{ error.latest }}</p>
        </div>
      </div>
      <button type="button" class="ghost-btn" :disabled="loading.latest" @click="loadLatest">
        重新获取最近结果
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
