import { createApp } from "vue";
import App from "./App.vue";
import router from "./router/index.js";
import pinia from "./stores/pinia.js";
import "./styles/tokens.css";
import "./styles/base.css";

createApp(App).use(pinia).use(router).mount("#app");
