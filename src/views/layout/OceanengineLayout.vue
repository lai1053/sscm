<template>
  <el-container>
    <el-header class="nav-container">
      <navbar
        nav-index="/oceanengine"
        @nav-items-click="navClick"/>
    </el-header>
    <el-container>
      <sidebar
        :items="oceanengineRouters"
        class="sidebar-container" />

      <el-main id="manager-main-container">
        <app-main/>
      </el-main>
    </el-container>
  </el-container>
</template>

<script>
import { mapGetters } from 'vuex'
import { Navbar, Sidebar, AppMain } from './components'

export default {
  name: 'OceanengineLayout',
  components: {
    Navbar,
    Sidebar,
    AppMain
  },
  computed: {
    ...mapGetters(['oceanengineRouters'])
  },
  methods: {
    navClick() {}
  }
}
</script>

<style lang="scss" scoped>
.aside-container {
  position: relative;
  background-color: #2d3037;
  box-sizing: border-box;
}

.nav-container {
  padding: 0;
  box-shadow: 0px 1px 2px #dbdbdb;
  z-index: 100;
  min-width: 1200px;
}

#manager-main-container {
  max-height: 100%;
  padding: 0 0 15px;
}

.el-container {
  overflow: hidden;
  height: 100%;
}

</style>
