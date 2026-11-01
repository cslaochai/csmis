<template>
  <!--
    递归菜单节点：支持任意层级（当前为三级：顶级域 → 业务分组 → 页面）。
    规则：
      - menuType=1（目录）且有子节点 → el-sub-menu
      - menuType=1 但无子节点 → 当作页面处理（不渲染成空的折叠组）
      - menuType=2（菜单）→ el-menu-item，index 用 path 走 router 跳转
    折叠态（collapsed）下只渲染一层，避免 el-menu 折叠时子菜单错位。
  -->
  <el-sub-menu v-if="isGroup && !collapsed" :index="String(node.id)">
    <template #title>
      <el-icon v-if="node.icon"><component :is="node.icon" /></el-icon>
      <span>{{ node.menuName }}</span>
    </template>
    <MenuTreeNode
      v-for="child in node.children || []"
      :key="child.id"
      :node="child"
      :collapsed="collapsed" />
  </el-sub-menu>

  <el-menu-item v-else-if="!isGroup" :index="node.path || String(node.id)">
    <el-icon v-if="node.icon"><component :is="node.icon" /></el-icon>
    <template #title>
      <span>{{ node.menuName }}</span>
    </template>
  </el-menu-item>

  <!-- 折叠态：目录直接展开为子项（el-menu 折叠时子菜单不显示，这里降级为平铺） -->
  <template v-else>
    <el-menu-item
      v-for="child in node.children || []"
      :key="child.id"
      :index="child.path || String(child.id)">
      <el-icon v-if="child.icon"><component :is="child.icon" /></el-icon>
      <template #title>
        <span>{{ child.menuName }}</span>
      </template>
    </el-menu-item>
  </template>
</template>

<script setup>
import { computed } from 'vue'
// 递归组件自引用（Vue3 script setup 需显式导入自身文件名组件）
import MenuTreeNode from './MenuTreeNode.vue'

const props = defineProps({
  /** 菜单节点（后端 /auth/menus 返回的树节点） */
  node: { type: Object, required: true },
  /** 侧边栏是否折叠 */
  collapsed: { type: Boolean, default: false },
})

/** 是目录（有子节点可展开） */
const isGroup = computed(() => {
  const n = props.node
  if (n.menuType !== 1) return false
  return Array.isArray(n.children) && n.children.length > 0
})
</script>
