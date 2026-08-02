<template>
  <div class="app-header">
    <img src="../assets/summ-logo.jpg" alt="SUMM logo" class="app-logo" />
    <slot />
  </div>
</template>

<script setup lang="ts">
// Slot content allows each page to place its title/actions alongside the logo.
</script>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem 1rem;
  border-bottom: 1px solid #e0e0e0;
  background: #fff;
  margin-bottom: 1rem;
}

.app-logo {
  height: 48px;
  width: auto;
  flex-shrink: 0;
  border-radius: 4px;
}
</style>
