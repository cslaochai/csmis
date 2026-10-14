<template>
  <svg
    class="brand-mark"
    :width="size"
    :height="size"
    viewBox="0 0 48 48"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    role="img"
    :aria-label="label"
  >
    <defs>
      <linearGradient :id="gradId" x1="8" y1="4" x2="40" y2="45" gradientUnits="userSpaceOnUse">
        <stop offset="0" :stop-color="colors.from" :stop-opacity="colors.stopOpacity" />
        <stop offset="1" :stop-color="colors.to" :stop-opacity="colors.stopOpacity" />
      </linearGradient>
    </defs>

    <!-- 盾形：保障与合规 -->
    <path
      d="M24 4 L40 10.3 V25.6 C40 34.7 33.7 41.2 24 44.5 C14.3 41.2 8 34.7 8 25.6 V10.3 Z"
      :fill="`url(#${gradId})`"
      :stroke="colors.stroke"
      :stroke-opacity="colors.strokeOpacity"
      :stroke-width="colors.strokeWidth"
    />
    <!-- 十字：医疗 -->
    <path
      d="M21.5 12.5 h5 v4 h4 v5 h-4 v4 h-5 v-4 h-4 v-5 h4 Z"
      :fill="colors.glyph"
    />
    <!-- 双线：结算清单 -->
    <rect x="15.5" y="29" width="17" height="3.2" rx="1.6" :fill="colors.glyphSoft" :fill-opacity="colors.glyphSoftOpacity" />
    <rect x="15.5" y="34.4" width="11" height="3.2" rx="1.6" :fill="colors.glyphSoft" :fill-opacity="colors.glyphSoftOpacity" />
  </svg>
</template>

<script setup>
// 品牌徽标：盾形 + 医疗十字 + 结算清单双线。纯内联 SVG，随尺寸矢量缩放。
import { computed } from 'vue'

const props = defineProps({
  size: { type: [Number, String], default: 44 },
  // brand = 浅色背景用（渐变实底盾），light = 深色/渐变背景用（玻璃底盾）
  tone: { type: String, default: 'brand' },
  label: { type: String, default: '医保结算经办平台' },
})

const TONES = {
  brand: {
    from: '#1269B5',
    to: '#0E9488',
    stopOpacity: 1,
    stroke: '#0B3B66',
    strokeOpacity: 0.16,
    strokeWidth: 1,
    glyph: '#FFFFFF',
    glyphSoft: '#FFFFFF',
    glyphSoftOpacity: 0.82,
  },
  light: {
    from: '#FFFFFF',
    to: '#FFFFFF',
    stopOpacity: 0.16,
    stroke: '#FFFFFF',
    strokeOpacity: 0.42,
    strokeWidth: 1.5,
    glyph: '#FFFFFF',
    glyphSoft: '#FFFFFF',
    glyphSoftOpacity: 0.72,
  },
}

const colors = computed(() => TONES[props.tone] || TONES.brand)
const gradId = computed(() => `brand-mark-grad-${props.tone}-${String(props.size).replace(/\D/g, '')}`)
</script>

<style scoped>
.brand-mark {
  display: block;
  flex: none;
}
</style>
