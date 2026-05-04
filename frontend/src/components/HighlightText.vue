<script setup>
import { computed } from "vue";

const props = defineProps({
  html: {
    type: String,
    default: null,
  },
  text: {
    type: String,
    default: null,
  },
  tag: {
    type: String,
    default: "span",
  },
  className: {
    type: String,
    default: "",
  },
});

const displayText = computed(() => {
  if (props.html) {
    return null;
  }
  return props.text;
});
</script>

<template>
  <component
    :is="tag"
    v-if="html"
    :class="className"
    v-html="html"
  />
  <component
    :is="tag"
    v-else-if="displayText"
    :class="className"
  >{{ displayText }}</component>
</template>
