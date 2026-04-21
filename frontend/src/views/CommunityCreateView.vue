<script setup>
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { createCommunityPost } from "../api/community.js";

const router = useRouter();

const submitting = ref(false);
const errorMessage = ref("");
const form = reactive({
  tag: "CAREER",
  title: "",
  content: "",
  experiencePost: false,
  experienceTargetLabel: "",
  experienceOutcomeLabel: "",
  experienceTimelineSummary: "",
  experienceActionSummary: "",
});

const tagOptions = [
  { value: "CAREER", label: "就业" },
  { value: "EXAM", label: "考研" },
  { value: "ABROAD", label: "留学" },
  { value: "CHAT", label: "闲聊" },
];

async function handleSubmit() {
  errorMessage.value = "";

  if (!form.title.trim()) {
    errorMessage.value = "请输入标题。";
    return;
  }

  if (!form.content.trim()) {
    errorMessage.value = "请输入正文。";
    return;
  }

  submitting.value = true;

  try {
    const payload = {
      tag: form.tag,
      title: form.title.trim(),
      content: form.content.trim(),
      ...(form.experiencePost
        ? {
            experiencePost: true,
            experienceTargetLabel: form.experienceTargetLabel.trim(),
            experienceOutcomeLabel: form.experienceOutcomeLabel.trim(),
            experienceTimelineSummary: form.experienceTimelineSummary.trim(),
            experienceActionSummary: form.experienceActionSummary.trim(),
          }
        : {}),
    };
    const created = await createCommunityPost(payload);
    router.push(`/community/${created.id}`);
  } catch (error) {
    errorMessage.value = error.message || "帖子发布失败，请稍后重试。";
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <section class="page-stack">
    <article class="section-card community-create-hero">
      <span class="section-eyebrow">Write a Post</span>
      <h1 class="hero-title" style="margin-top: 18px;">Write the main story first, then add structure where it helps.</h1>
      <hr class="editorial-rule" />
      <p class="hero-copy">
        Community posts can stay lightweight, or you can switch on the optional experience layer to summarize
        target, outcome, timeline, and actionable notes for readers.
      </p>
    </article>

    <article class="section-card">
      <form class="field-grid" @submit.prevent="handleSubmit">
        <label class="field-label">
          方向标签
          <select
            v-model="form.tag"
            class="field-select"
            name="tag"
          >
            <option
              v-for="option in tagOptions"
              :key="option.value"
              :value="option.value"
            >
              {{ option.label }}
            </option>
          </select>
        </label>

        <label class="field-label">
          标题
          <input
            v-model.trim="form.title"
            class="field-control"
            name="title"
            type="text"
            maxlength="120"
            placeholder="例如：秋招时间线复盘、考研选校取舍、留学材料避坑清单"
          />
        </label>

        <label class="field-label">
          正文
          <textarea
            v-model.trim="form.content"
            class="field-textarea"
            name="content"
            maxlength="10000"
            placeholder="正文里建议写清楚背景、过程、结果和你最想提醒别人的部分。"
          />
        </label>

        <article class="panel-card experience-panel">
          <label class="experience-panel__toggle">
            <input
              v-model="form.experiencePost"
              name="experience-post"
              type="checkbox"
            />
            <div class="experience-panel__toggle-copy">
              <strong>Add experience structure</strong>
              <span class="meta-copy">Turn this on when you want the post to surface a quick summary card.</span>
            </div>
          </label>

          <div v-if="form.experiencePost" class="field-grid experience-panel__fields">
            <label class="field-label">
              Target
              <input
                v-model.trim="form.experienceTargetLabel"
                class="field-control"
                name="experience-target-label"
                type="text"
                maxlength="120"
                placeholder="Backend internship sprint"
              />
            </label>

            <label class="field-label">
              Outcome
              <input
                v-model.trim="form.experienceOutcomeLabel"
                class="field-control"
                name="experience-outcome-label"
                type="text"
                maxlength="120"
                placeholder="Received 2 interview invitations"
              />
            </label>

            <label class="field-label">
              Timeline
              <textarea
                v-model.trim="form.experienceTimelineSummary"
                class="field-textarea"
                name="experience-timeline-summary"
                maxlength="255"
                placeholder="Week 1 resume refresh, week 2 projects, week 3 applications"
              />
            </label>

            <label class="field-label">
              Action notes
              <textarea
                v-model.trim="form.experienceActionSummary"
                class="field-textarea"
                name="experience-action-summary"
                maxlength="500"
                placeholder="Refine one showcase project, then batch tailored applications."
              />
            </label>
          </div>
        </article>

        <p v-if="errorMessage" class="field-error" role="alert">{{ errorMessage }}</p>

        <div class="inline-form-actions">
          <button type="submit" class="app-btn" :disabled="submitting">
            {{ submitting ? "发布中..." : "立即发布" }}
          </button>
          <button type="button" class="ghost-btn" @click="router.push('/community')">
            返回社区
          </button>
        </div>
      </form>
    </article>
  </section>
</template>

<style scoped>
.experience-panel {
  display: grid;
  gap: 18px;
}

.experience-panel__toggle {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  cursor: pointer;
}

.experience-panel__toggle input {
  margin-top: 4px;
}

.experience-panel__toggle-copy {
  display: grid;
  gap: 6px;
}

.experience-panel__fields {
  margin-top: 4px;
}
</style>
