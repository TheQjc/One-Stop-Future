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
    const created = await createCommunityPost({
      tag: form.tag,
      title: form.title.trim(),
      content: form.content.trim(),
    });
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
      <h1 class="hero-title" style="margin-top: 18px;">先把结论写清，再补充细节和上下文。</h1>
      <hr class="editorial-rule" />
      <p class="hero-copy">
        社区首期先只做纯文本帖子，重点是把经验、提醒和判断过程完整沉淀下来。
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
            placeholder="例如：秋招时间线复盘、考研选校取舍、留学材料踩坑总结"
          />
        </label>

        <label class="field-label">
          正文
          <textarea
            v-model.trim="form.content"
            class="field-textarea"
            name="content"
            maxlength="10000"
            placeholder="正文里建议写清楚背景、过程、结果和建议。"
          />
        </label>

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
