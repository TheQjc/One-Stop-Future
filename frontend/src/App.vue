<script setup>
import NavBar from "./components/NavBar.vue";
import PageFooter from "./components/PageFooter.vue";
</script>

<template>
  <div class="min-h-screen relative bg-slate-50 text-slate-900 overflow-x-hidden">
    <!-- Background Effects -->
    <div 
      class="pointer-events-none fixed inset-0 bg-[linear-gradient(rgba(24,38,63,0.03)_1px,transparent_1px),linear-gradient(90deg,rgba(24,38,63,0.03)_1px,transparent_1px)] bg-[size:64px_64px] [mask-image:linear-gradient(180deg,rgba(0,0,0,0.4),transparent_88%)]" 
      aria-hidden="true" 
    />
    <div 
      class="pointer-events-none fixed bottom-[12%] -left-[8%] w-[22rem] h-[22rem] rounded-full blur-[18px] opacity-70 bg-[radial-gradient(circle,rgba(139,92,246,0.15),transparent_72%)]" 
      aria-hidden="true" 
    />
    <div 
      class="pointer-events-none fixed top-[8%] -right-[10%] w-[28rem] h-[28rem] rounded-full blur-[18px] opacity-70 bg-[radial-gradient(circle,rgba(244,63,94,0.12),transparent_70%)]" 
      aria-hidden="true" 
    />
    
    <!-- Modern Navbar Wrapper -->
    <div class="sticky top-0 z-50 bg-white/80 backdrop-blur-md border-b border-gray-200 shadow-sm flex items-center w-full">
      <NavBar class="w-full flex-1" />
    </div>

    <!-- Main Content -->
    <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 relative z-10 min-h-[calc(100vh-16rem)]">
      <router-view />
    </main>

    <PageFooter class="relative z-10" />
  </div>
</template>


