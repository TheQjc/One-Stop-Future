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
      <span class="section-eyebrow">发布帖子</span>
      <h1 class="hero-title" style="margin-top: 18px;">先把核心经历讲清楚，再按需要补充结构化信息。</h1>
      <hr class="editorial-rule" />
      <p class="hero-copy">
        你可以先用轻量方式发帖；如果希望读者更快抓住重点，也可以开启经历摘要，把目标、结果、时间线和行动建议整理出来。
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
              <strong>添加经历结构</strong>
              <span class="meta-copy">如果你希望帖子自动生成一张摘要卡片，可以打开这个选项。</span>
            </div>
          </label>

          <div v-if="form.experiencePost" class="field-grid experience-panel__fields">
            <label class="field-label">
              目标
              <input
                v-model.trim="form.experienceTargetLabel"
                class="field-control"
                name="experience-target-label"
                type="text"
                maxlength="120"
                placeholder="例如：后端实习冲刺"
              />
            </label>

            <label class="field-label">
              结果
              <input
                v-model.trim="form.experienceOutcomeLabel"
                class="field-control"
                name="experience-outcome-label"
                type="text"
                maxlength="120"
                placeholder="例如：拿到 2 个面试邀请"
              />
            </label>

            <label class="field-label">
              时间线
              <textarea
                v-model.trim="form.experienceTimelineSummary"
                class="field-textarea"
                name="experience-timeline-summary"
                maxlength="255"
                placeholder="例如：第 1 周改简历，第 2 周补项目，第 3 周集中投递"
              />
            </label>

            <label class="field-label">
              行动提醒
              <textarea
                v-model.trim="form.experienceActionSummary"
                class="field-textarea"
                name="experience-action-summary"
                maxlength="500"
                placeholder="例如：先打磨一个代表项目，再批量投递更匹配的岗位。"
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
