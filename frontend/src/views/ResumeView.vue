<template>
  <div id="resume-app" :class="['resume-root', { 'mode-preview': mode === 'preview' }]">
    <!-- ====== 顶部工具栏 ====== -->
    <header class="topbar">
      <div class="tb-left">
        <el-button text @click="goBack"><el-icon><ArrowLeft /></el-icon> 返回</el-button>
        <span class="tb-title">简历编辑器</span>
      </div>
      <div class="tb-center edit-only">
        <el-button size="small" @click="applyBold">加粗 <kbd>Ctrl+B</kbd></el-button>
        <el-button size="small" @click="importJson">导入 JSON</el-button>
        <el-button size="small" @click="exportJson">导出 JSON</el-button>
        <el-button size="small" @click="loadSample">示例模板</el-button>
        <el-button size="small" @click="resetAll">清空</el-button>
        <el-button size="small" type="warning" @click="aiGenerate" :loading="aiGenerating">
          <el-icon><MagicStick /></el-icon> AI 生成
        </el-button>
      </div>
      <div class="tb-right">
        <span class="save-status">本地编辑（数据在内存中）</span>
        <el-button size="small" type="primary" @click="toggleMode">{{ mode==='edit'?'预览模式':'返回编辑' }}</el-button>
        <el-button size="small" @click="printResume">打印 / 导出 PDF</el-button>
        <el-button size="small" @click="showThemePanel = !showThemePanel">主题</el-button>
      </div>
    </header>

    <!-- ====== 主题面板 ====== -->
    <div v-if="showThemePanel" class="theme-panel edit-only">
      <div class="th-head"><span>主题/版式设置</span><el-button text size="small" @click="showThemePanel=false">关闭</el-button></div>
      <div class="th-body">
        <label>字号
          <select v-model="state.theme.fontScale"><option value="sm">小</option><option value="md">中</option><option value="lg">大</option></select>
        </label>
        <label>行距
          <select v-model="state.theme.lineHeight"><option value="compact">紧凑</option><option value="normal">标准</option><option value="loose">宽松</option></select>
        </label>
        <label>强调色
          <select v-model="state.theme.accent"><option value="mono">黑灰</option><option value="deepblue">深蓝</option></select>
        </label>
        <label>模块间距
          <select v-model="state.theme.sectionGap"><option value="sm">小</option><option value="md">中</option><option value="lg">大</option></select>
        </label>
        <label>头像显示
          <select v-model="state.theme.showAvatar"><option :value="true">显示</option><option :value="false">隐藏</option></select>
        </label>
      </div>
    </div>

    <!-- ====== 主体双栏 ====== -->
    <div class="wrap">
      <!-- 左侧编辑器 -->
      <aside class="editor edit-only">
        <!-- 模块结构 -->
        <div class="ed-box">
          <h3>模块结构</h3>
          <button class="mod-btn" :class="{ active: selected.type==='header' }" @click="selectHeader">顶部信息</button>
          <div v-for="(sec, i) in state.sections" :key="sec.id"
            :class="['mod-item', { active: selected.type==='section' && selected.sid===sec.id, hide: sec.hidden }]">
            <button class="mod-btn" @click="selectSection(sec.id)">{{ sec.title }}</button>
            <div class="mod-ops">
              <button @click="moveSection(i, -1)" :disabled="i===0">↑</button>
              <button @click="moveSection(i, 1)" :disabled="i===state.sections.length-1">↓</button>
              <button @click="sec.hidden=!sec.hidden; toast(sec.hidden?'已隐藏':'已显示')">{{ sec.hidden?'显示':'隐藏' }}</button>
            </div>
          </div>
        </div>

        <!-- 顶部信息编辑 -->
        <div v-if="selected.type==='header'" class="ed-box">
          <h3>顶部信息编辑</h3>
          <div class="field-grid">
            <label v-for="f in headerFields" :key="f.key" :style="f.span ? 'grid-column:span 2' : ''">
              {{ f.label }}<input class="fld" v-model="state.header[f.key]" @input="onFieldChange" />
            </label>
          </div>
          <div style="margin-top:10px;display:flex;gap:8px;align-items:center">
            <div class="av-preview" :class="{ 'has-img': state.header.avatarDataUrl }" @click="triggerAvatar">
              <img v-if="state.header.avatarDataUrl" :src="state.header.avatarDataUrl" alt="头像" />
              <span v-else>证件照</span>
            </div>
            <button @click="triggerAvatar">上传头像</button>
            <button @click="state.header.avatarDataUrl=''; onFieldChange()">删除</button>
            <input ref="avatarInput" type="file" accept="image/*" hidden @change="onAvatarChange" />
          </div>
        </div>

        <!-- 教育经历/竞赛奖项 -->
        <div v-if="isSectionSelected('educationAwards')" class="ed-box">
          <h3>{{ curSection.title }}</h3>
          <label>模块标题<input class="fld" v-model="curSection.title" @input="onFieldChange" /></label>
          <div class="field-grid">
            <label>学校<input class="fld" v-model="curSection.education.school" @input="onFieldChange" /></label>
            <label>专业<input class="fld" v-model="curSection.education.major" @input="onFieldChange" /></label>
            <label>学历<input class="fld" v-model="curSection.education.degree" @input="onFieldChange" /></label>
            <label>起止时间<input class="fld" v-model="curSection.education.timeRange" @input="onFieldChange" /></label>
          </div>
          <label>奖项分栏
            <select v-model.number="curSection.layout.columns" @change="onFieldChange">
              <option :value="1">1 列</option><option :value="2">2 列</option>
            </select>
          </label>
          <div class="arr-list">
            <h4>奖项条目</h4>
            <div v-for="(a, ai) in curSection.awards" :key="ai" class="arr-item">
              <div class="arr-head">
                <span>奖项 {{ ai+1 }}</span>
                <div class="arr-ops">
                  <button @click="moveArr(curSection.awards, ai, -1)">↑</button>
                  <button @click="moveArr(curSection.awards, ai, 1)">↓</button>
                  <button @click="curSection.awards.splice(ai,1); onFieldChange()">删除</button>
                </div>
              </div>
              <textarea class="fld" rows="2" v-model="curSection.awards[ai]" @input="onFieldChange"></textarea>
            </div>
            <button class="add-btn" @click="curSection.awards.push(''); onFieldChange()">+ 新增奖项</button>
          </div>
        </div>

        <!-- 实习经历 -->
        <div v-if="isSectionSelected('experienceList')" class="ed-box">
          <h3>{{ curSection.title }}</h3>
          <label>模块标题<input class="fld" v-model="curSection.title" @input="onFieldChange" /></label>
          <div class="arr-list">
            <div v-for="(it, ii) in curSection.items" :key="ii" class="arr-item">
              <div class="arr-head"><span>实习 {{ ii+1 }}</span>
                <div class="arr-ops">
                  <button @click="moveArr(curSection.items, ii, -1)">↑</button>
                  <button @click="moveArr(curSection.items, ii, 1)">↓</button>
                  <button @click="curSection.items.splice(ii,1); onFieldChange()">删除</button>
                </div>
              </div>
              <div class="field-grid">
                <label>公司<input class="fld" v-model="it.company" @input="onFieldChange" /></label>
                <label>职位<input class="fld" v-model="it.role" @input="onFieldChange" /></label>
                <label style="grid-column:span 2">起止时间<input class="fld" v-model="it.timeRange" @input="onFieldChange" /></label>
              </div>
              <div v-for="(b, bi) in it.bullets" :key="bi" class="arr-item">
                <div class="arr-head"><span>要点 {{ bi+1 }}</span>
                  <div class="arr-ops">
                    <button @click="moveArr(it.bullets, bi, -1)">↑</button>
                    <button @click="moveArr(it.bullets, bi, 1)">↓</button>
                    <button @click="it.bullets.splice(bi,1); onFieldChange()">删除</button>
                  </div>
                </div>
                <textarea class="fld bullet" rows="2" v-model="it.bullets[bi]" @input="onFieldChange" @keydown="onBulletKey($event, it, bi)"></textarea>
              </div>
              <button class="add-btn" @click="it.bullets.push(''); onFieldChange()">+ 新增要点</button>
            </div>
            <button class="add-btn" @click="addExperienceItem()">+ 新增实习</button>
          </div>
        </div>

        <!-- 项目经历 -->
        <div v-if="isSectionSelected('projectList')" class="ed-box">
          <h3>{{ curSection.title }}</h3>
          <label>模块标题<input class="fld" v-model="curSection.title" @input="onFieldChange" /></label>
          <div class="arr-list">
            <div v-for="(it, ii) in curSection.items" :key="ii" class="arr-item">
              <div class="arr-head"><span>项目 {{ ii+1 }}</span>
                <div class="arr-ops">
                  <button @click="moveArr(curSection.items, ii, -1)">↑</button>
                  <button @click="moveArr(curSection.items, ii, 1)">↓</button>
                  <button @click="curSection.items.splice(ii,1); onFieldChange()">删除</button>
                </div>
              </div>
              <div class="field-grid">
                <label>项目名<input class="fld" v-model="it.name" @input="onFieldChange" /></label>
                <label>链接标签<input class="fld" v-model="it.linkLabel" @input="onFieldChange" /></label>
                <label style="grid-column:span 2">链接URL<input class="fld" v-model="it.linkUrl" @input="onFieldChange" /></label>
              </div>
              <label>项目介绍<textarea class="fld" rows="3" v-model="it.intro" @input="onFieldChange"></textarea></label>
              <div v-for="(b, bi) in it.bullets" :key="bi" class="arr-item">
                <div class="arr-head"><span>要点 {{ bi+1 }}</span>
                  <div class="arr-ops">
                    <button @click="moveArr(it.bullets, bi, -1)">↑</button>
                    <button @click="moveArr(it.bullets, bi, 1)">↓</button>
                    <button @click="it.bullets.splice(bi,1); onFieldChange()">删除</button>
                  </div>
                </div>
                <textarea class="fld bullet" rows="2" v-model="it.bullets[bi]" @input="onFieldChange" @keydown="onBulletKey($event, it, bi)"></textarea>
              </div>
              <button class="add-btn" @click="it.bullets.push(''); onFieldChange()">+ 新增要点</button>
            </div>
            <button class="add-btn" @click="addProjectItem()">+ 新增项目</button>
          </div>
        </div>

        <!-- 专业技能 -->
        <div v-if="isSectionSelected('skillLines')" class="ed-box">
          <h3>{{ curSection.title }}</h3>
          <label>模块标题<input class="fld" v-model="curSection.title" @input="onFieldChange" /></label>
          <div class="arr-list">
            <div v-for="(it, ii) in curSection.items" :key="ii" class="arr-item">
              <div class="arr-head"><span>技能 {{ ii+1 }}</span>
                <div class="arr-ops">
                  <button @click="moveArr(curSection.items, ii, -1)">↑</button>
                  <button @click="moveArr(curSection.items, ii, 1)">↓</button>
                  <button @click="curSection.items.splice(ii,1); onFieldChange()">删除</button>
                </div>
              </div>
              <div class="field-grid">
                <label>技能项<input class="fld" v-model="it.label" @input="onFieldChange" /></label>
                <label>描述<textarea class="fld" rows="2" v-model="it.value" @input="onFieldChange"></textarea></label>
              </div>
            </div>
            <button class="add-btn" @click="addSkillItem()">+ 新增技能行</button>
          </div>
        </div>

        <div v-if="selected.type==='section' && !curSection" class="ed-box"><h3>未选择模块</h3></div>
      </aside>

      <!-- 右侧预览 -->
      <main class="pre">
        <div class="page" :style="pageStyle">
          <!-- 头部 -->
          <div class="hd">
            <div>
              <div class="nrow">
                <div class="nm">{{ state.header.name || '姓名' }}</div>
                <div class="mi">
                  <span>性别：{{ state.header.gender || '--' }}</span><span class="sep">｜</span>
                  <span>年龄：{{ state.header.age || '--' }}</span>
                </div>
              </div>
              <div class="crow">
                <span>☎ {{ state.header.phone || '--' }}</span>
                <span>✉ {{ state.header.email || '--' }}</span>
              </div>
              <div class="crow">
                <span>▣ {{ state.header.status || '--' }}</span>
                <span>⌁ 意向职位：{{ state.header.targetTitle || '--' }}</span>
              </div>
            </div>
            <div v-if="state.theme.showAvatar" class="av">
              <img v-if="state.header.avatarDataUrl" :src="state.header.avatarDataUrl" alt="avatar" />
              <span v-else>证件照</span>
            </div>
          </div>

          <!-- 各模块 -->
          <section v-for="(sec, si) in visibleSections" :key="sec.id" class="sec">
            <div class="st"><h2>{{ sec.title }}</h2><div class="ln"></div></div>

            <!-- 教育经历 -->
            <template v-if="sec.type==='educationAwards'">
              <div class="eh">
                <div class="el"><strong>{{ sec.education.school }}</strong></div>
                <div>{{ sec.education.major }} / {{ sec.education.degree }}</div>
                <div class="dt">{{ sec.education.timeRange }}</div>
              </div>
              <ul class="aw" :style="'--cols:' + (sec.layout?.columns||2)">
                <li v-for="(a, ai) in sec.awards" :key="ai" v-html="md(a)"></li>
              </ul>
            </template>

            <!-- 实习经历 -->
            <template v-if="sec.type==='experienceList'">
              <article v-for="(it, ii) in sec.items" :key="ii" class="ent">
                <div class="ehd">
                  <div class="cmp">{{ it.company }}</div>
                  <div class="role">{{ it.role }}</div>
                  <div class="dt">{{ it.timeRange }}</div>
                </div>
                <div class="ttl">{{ it.highlightsTitle || '主要工作：' }}</div>
                <ul class="bul">
                  <li v-for="(b, bi) in it.bullets" :key="bi" v-html="md(b)"></li>
                </ul>
              </article>
            </template>

            <!-- 项目经历 -->
            <template v-if="sec.type==='projectList'">
              <article v-for="(it, ii) in sec.items" :key="ii" class="ent">
                <div class="ehd">
                  <div class="cmp">{{ it.name }}</div><div></div><div></div>
                </div>
                <div v-if="it.linkUrl">
                  <a v-if="isValidUrl(it.linkUrl)" class="plink" :href="it.linkUrl" target="_blank">{{ it.linkLabel||'项目链接' }}：{{ it.linkUrl }}</a>
                  <span v-else class="muted">{{ it.linkLabel||'项目链接' }}：{{ it.linkUrl }}</span>
                </div>
                <div v-html="md(it.intro)"></div>
                <div class="ttl">{{ it.highlightsTitle || '主要工作：' }}</div>
                <ul class="bul">
                  <li v-for="(b, bi) in it.bullets" :key="bi" v-html="md(b)"></li>
                </ul>
              </article>
            </template>

            <!-- 专业技能 -->
            <template v-if="sec.type==='skillLines'">
              <p v-for="(it, ii) in sec.items" :key="ii" class="skill">
                <strong>{{ it.label }}：</strong><span v-html="md(it.value)"></span>
              </p>
            </template>
          </section>
        </div>
      </main>
    </div>

    <!-- Toast 通知 -->
    <div class="toastw"><div v-for="(t,i) in toasts" :key="i" :class="['toast', { err: t.err }]">{{ t.msg }}</div></div>

    <!-- 隐藏的文件输入 -->
    <input ref="importInput" type="file" accept=".json,application/json" hidden @change="onImportFile" />

    <!-- AI 生成结果弹窗 -->
    <div v-if="aiResult" class="ai-result-overlay" @click.self="aiResult=''">
      <div class="ai-result-box">
        <h3>AI 生成内容</h3>
        <div class="markdown-body" v-html="renderMarkdown(aiResult)" style="max-height:50vh;overflow:auto;font-size:13px;line-height:1.7"></div>
        <el-button type="primary" size="small" style="margin-top:12px" @click="aiResult=''">关闭</el-button>
        <el-button size="small" style="margin-top:12px" @click="copyAiResult">复制内容</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, MagicStick } from '@element-plus/icons-vue'
import { generateResume } from '../api/resume'
import { marked } from 'marked'
marked.setOptions({ breaks: true, gfm: true })

const router = useRouter()

// ==================== 数据模型 ====================
function clone(o) { return JSON.parse(JSON.stringify(o)) }

const DEFAULT = {
  theme: { fontScale: 'md', lineHeight: 'normal', accent: 'mono', showAvatar: true, sectionGap: 'md' },
  header: { name: '张三', gender: '男', age: '21', phone: '13800000000', email: 'zhangsan@example.com', status: '在校生', targetTitle: '后端开发实习生', avatarDataUrl: '' },
  sections: [
    { id: 'edu', type: 'educationAwards', title: '教育经历/竞赛奖项', hidden: false, education: { school: '某重点大学', tags: ['211', '双一流'], major: '计算机科学与技术', degree: '本科', timeRange: '2023-09 ~ 2027-06' }, awards: ['2025 ACM-ICPC 国际大学生程序设计竞赛全国邀请赛 **银奖**', '第十届团体程序设计天梯赛 **全国二等奖**', '大学英语六级（CET-6）'], layout: { columns: 2 } },
    { id: 'intern', type: 'experienceList', title: '实习经历', hidden: false, items: [{ company: '甲方科技有限公司', role: '后端开发实习生', timeRange: '2025-10 ~ 2026-01', highlightsTitle: '主要工作：', bullets: ['针对设备控制历史的**高频查询**，通过**联合索引排序 + UNION ALL**消除了 filesort 瓶颈，实测相关接口响应速度提高 **40%+**。', '基于 **MQTT v5** 协议 + **SSE**，开发 SCADA 项目中的设备实时推送模块，支持千级设备并发数据采集与实时展示。'] }] },
    { id: 'proj', type: 'projectList', title: '项目经历', hidden: false, items: [{ name: 'AI智能面试助手', linkLabel: '项目在线地址', linkUrl: 'https://example.com', intro: '基于 **LangChain + SpringBoot + RAG + Redis + MySQL** 实现的 AI 智能面试助手，支持面试题问答、AI模拟面试等功能。', highlightsTitle: '主要工作：', bullets: ['基于 **LangChain** 实现 **RAG** 检索增强生成，显著提高 AI 回答质量与准确性。', '基于 **Docker** 实现容器化部署。'] }] },
    { id: 'skill', type: 'skillLines', title: '专业技能', hidden: false, items: [{ label: 'Java基础', value: '熟悉 Java，如常见集合类及底层数据结构、面向对象特性、反射等。' }, { label: 'Redis', value: '熟悉 Redis 常见数据结构及应用场景，了解 IO 模型、集群、持久化策略等。' }, { label: 'MySQL', value: '熟悉索引、事务、锁机制、MVCC 等，有结合业务进行 SQL 优化的实践经历。' }] }
  ]
}

const EMPTY = {
  theme: { fontScale: 'md', lineHeight: 'normal', accent: 'mono', showAvatar: true, sectionGap: 'md' },
  header: { name: '', gender: '', age: '', phone: '', email: '', status: '', targetTitle: '', avatarDataUrl: '' },
  sections: [
    { id: 'edu', type: 'educationAwards', title: '教育经历/竞赛奖项', hidden: false, education: { school: '', tags: [], major: '', degree: '', timeRange: '' }, awards: [], layout: { columns: 2 } },
    { id: 'intern', type: 'experienceList', title: '实习经历', hidden: false, items: [] },
    { id: 'proj', type: 'projectList', title: '项目经历', hidden: false, items: [] },
    { id: 'skill', type: 'skillLines', title: '专业技能', hidden: false, items: [] }
  ]
}

function normalize(raw) {
  if (!raw || typeof raw !== 'object') return clone(DEFAULT)
  return {
    theme: { ...DEFAULT.theme, ...(raw.theme || {}) },
    header: { ...DEFAULT.header, ...(raw.header || {}) },
    sections: Array.isArray(raw.sections) && raw.sections.length
      ? DEFAULT.sections.map((defSec, i) => {
          const src = raw.sections[i] || defSec
          return { ...defSec, ...src, education: src.education ? { ...defSec.education, ...src.education } : defSec.education }
        })
      : clone(DEFAULT.sections)
  }
}

// ==================== 状态 ====================
const mode = ref('edit')
const selected = reactive({ type: 'header', sid: '' })
const showThemePanel = ref(false)
const toasts = ref([])
const aiGenerating = ref(false)
const avatarInput = ref(null)
const importInput = ref(null)

const state = reactive(clone(EMPTY))

const headerFields = [
  { key: 'name', label: '姓名' }, { key: 'gender', label: '性别' },
  { key: 'age', label: '年龄' }, { key: 'phone', label: '电话' },
  { key: 'email', label: '邮箱' }, { key: 'status', label: '身份标签' },
  { key: 'targetTitle', label: '意向职位', span: true }
]

// ==================== 主题 ====================
const themeVars = computed(() => {
  const fs = { sm: '10.8px', md: '11.5px', lg: '12.2px' }
  const lh = { compact: '1.48', normal: '1.6', loose: '1.74' }
  const ac = { mono: '#222', deepblue: '#1b3f74' }
  const sg = { sm: '10px', md: '14px', lg: '18px' }
  return {
    '--fs': fs[state.theme.fontScale] || fs.md,
    '--lh': lh[state.theme.lineHeight] || lh.normal,
    '--rc': ac[state.theme.accent] || ac.mono,
    '--sg': sg[state.theme.sectionGap] || sg.md
  }
})

const pageStyle = computed(() => ({
  fontSize: themeVars.value['--fs'],
  lineHeight: themeVars.value['--lh'],
  '--rc': themeVars.value['--rc'],
  '--sg': themeVars.value['--sg']
}))

// ==================== 模块选择 ====================
const visibleSections = computed(() => state.sections.filter(s => !s.hidden))

const curSection = computed(() => {
  if (selected.type !== 'section') return null
  return state.sections.find(s => s.id === selected.sid)
})

function isSectionSelected(type) {
  if (selected.type !== 'section') return false
  return curSection.value?.type === type
}

function selectHeader() { selected.type = 'header'; selected.sid = '' }
function selectSection(id) { selected.type = 'section'; selected.sid = id }

// ==================== 模式切换 ====================
function toggleMode() {
  mode.value = mode.value === 'edit' ? 'preview' : 'edit'
  showThemePanel.value = false
  toast(mode.value === 'preview' ? '已进入预览模式' : '已返回编辑模式')
}

function printResume() {
  const prev = mode.value
  mode.value = 'preview'
  nextTick(() => {
    window.print()
    if (prev === 'edit') mode.value = 'edit'
  })
}

// ==================== Markdown 渲染 ====================
function md(txt) {
  if (!txt) return ''
  let v = esc(txt)
  v = v.replace(/\*\*([\s\S]+?)\*\*/g, '<strong>$1</strong>')
  v = v.replace(/(https?:\/\/[^\s<]+)/g, m => `<a href="${esc(m)}" target="_blank" rel="noopener">${esc(m)}</a>`)
  return v.replace(/\n/g, '<br>')
}

function esc(v) {
  return String(v).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
}

function isValidUrl(u) {
  try { const x = new URL(u, location.href); return ['http:', 'https:', 'mailto:'].includes(x.protocol) }
  catch { return false }
}

// ==================== 字段变更 ====================
function onFieldChange() { /* 实时预览自动更新（响应式） */ }

function moveArr(arr, from, to) {
  const dir = to > from ? 1 : -1
  const newIdx = from + dir
  if (newIdx < 0 || newIdx >= arr.length) return
  const [item] = arr.splice(from, 1)
  arr.splice(newIdx, 0, item)
  onFieldChange()
}

function moveSection(i, dir) {
  const to = i + dir
  if (to < 0 || to >= state.sections.length) return
  const [item] = state.sections.splice(i, 1)
  state.sections.splice(to, 0, item)
  onFieldChange()
}

function onBulletKey(e, parentArr, idx) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    parentArr.bullets.splice(idx + 1, 0, '')
  }
  if (e.key === 'Backspace' && !e.target.value.trim() && parentArr.bullets.length > 1) {
    e.preventDefault()
    parentArr.bullets.splice(idx, 1)
  }
}

// ==================== 加粗 ====================
function applyBold() {
  const el = document.activeElement
  if (!el || (el.tagName !== 'TEXTAREA' && el.tagName !== 'INPUT')) {
    toast('请先在可编辑文本中选择内容', true); return
  }
  const st = el.selectionStart, ed = el.selectionEnd
  if (st == null || ed == null) { toast('当前字段不支持加粗', true); return }
  const v = el.value
  const sel = st === ed ? '关键词' : v.slice(st, ed)
  const nv = v.slice(0, st) + '**' + sel + '**' + v.slice(ed)
  el.value = nv
  el.setSelectionRange(st + 2, st + 2 + sel.length)
  el.dispatchEvent(new Event('input', { bubbles: true }))
  onFieldChange()
}

// ==================== 头像 ====================
function triggerAvatar() { avatarInput.value?.click() }

function onAvatarChange(e) {
  const f = e.target.files?.[0]
  if (!f) return
  const reader = new FileReader()
  reader.onload = () => {
    state.header.avatarDataUrl = String(reader.result)
    onFieldChange()
    toast('头像已更新')
  }
  reader.readAsDataURL(f)
}

// ==================== JSON 导入导出 ====================
function exportJson() {
  const blob = new Blob([JSON.stringify(state, null, 2)], { type: 'application/json' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = 'resume.json'
  a.click()
  URL.revokeObjectURL(a.href)
  toast('已导出 resume.json')
}

function importJson() { importInput.value?.click() }

function onImportFile(e) {
  const f = e.target.files?.[0]
  if (!f) return
  const reader = new FileReader()
  reader.onload = () => {
    try {
      const data = JSON.parse(String(reader.result))
      if (!data.header || !Array.isArray(data.sections)) throw new Error('数据格式无效')
      Object.assign(state, clone(normalize(data)))
      selected.type = 'header'; selected.sid = ''
      toast('导入成功')
    } catch (err) { toast('导入失败：' + err.message, true) }
    importInput.value.value = ''
  }
  reader.readAsText(f)
}

// ==================== 示例与清空 ====================
function loadSample() {
  Object.assign(state, clone(DEFAULT))
  selected.type = 'header'; selected.sid = ''
  toast('已加载示例模板')
}

function resetAll() {
  if (!confirm('确认清空为空白模板？未导出的修改会丢失。')) return
  Object.assign(state, clone(EMPTY))
  selected.type = 'header'; selected.sid = ''
  toast('已清空')
}

// ==================== AI 生成 ====================
async function aiGenerate() {
  if (!state.header.name) { toast('请先填写姓名', true); return }

  aiGenerating.value = true
  try {
    const eduSec = state.sections.find(s => s.type === 'educationAwards')
    const expSec = state.sections.find(s => s.type === 'experienceList')
    const projSec = state.sections.find(s => s.type === 'projectList')
    const skillSec = state.sections.find(s => s.type === 'skillLines')

    const data = {
      name: state.header.name,
      phone: state.header.phone,
      email: state.header.email,
      targetPosition: state.header.targetTitle,
      education: eduSec ? formatEduForAI(eduSec) : '',
      experience: expSec ? formatExpForAI(expSec) : '',
      projects: projSec ? formatProjForAI(projSec) : '',
      skills: skillSec ? formatSkillForAI(skillSec) : ''
    }
    // 移除空字段
    Object.keys(data).forEach(k => { if (!data[k]) delete data[k] })

    const res = await generateResume(data)
    const content = res.data?.generatedContent
    if (content) {
      toast('AI 内容已生成，请将生成的内容填入对应模块')
      // 把生成的内容存入一个可查看的状态
      aiResult.value = content
    }
  } catch { toast('AI 生成失败', true) } finally { aiGenerating.value = false }
}

const aiResult = ref('')

function formatEduForAI(sec) {
  const parts = [`学校：${sec.education.school}`, `专业：${sec.education.major}`, `学历：${sec.education.degree}`, `时间：${sec.education.timeRange}`]
  if (sec.awards.length) parts.push('奖项：\n' + sec.awards.filter(Boolean).join('\n'))
  return parts.join('\n')
}
function formatExpForAI(sec) {
  return sec.items.map(it => `公司：${it.company}\n职位：${it.role}\n时间：${it.timeRange}\n工作内容：\n${it.bullets.filter(Boolean).join('\n')}`).join('\n\n')
}
function formatProjForAI(sec) {
  return sec.items.map(it => `项目：${it.name}\n介绍：${it.intro}\n工作：\n${it.bullets.filter(Boolean).join('\n')}`).join('\n\n')
}
function formatSkillForAI(sec) {
  return sec.items.map(it => `${it.label}：${it.value}`).join('\n')
}

function renderMarkdown(text) { return marked(text || '') }
function copyAiResult() {
  if (aiResult.value) {
    navigator.clipboard.writeText(aiResult.value)
    toast('已复制到剪贴板')
  }
}

// ==================== 新增条目 ====================
function addExperienceItem() {
  curSection.value?.items.push({ company: '', role: '', timeRange: '', highlightsTitle: '主要工作：', bullets: [''] })
  onFieldChange()
}
function addProjectItem() {
  curSection.value?.items.push({ name: '', linkLabel: '项目在线地址', linkUrl: '', intro: '', highlightsTitle: '主要工作：', bullets: [''] })
  onFieldChange()
}
function addSkillItem() {
  curSection.value?.items.push({ label: '', value: '' })
  onFieldChange()
}

// ==================== Toast ====================
function toast(msg, err = false) {
  toasts.value.push({ msg, err })
  setTimeout(() => toasts.value.shift(), 2200)
}

// ==================== 键盘快捷键 ====================
function onKeydown(e) {
  const meta = e.ctrlKey || e.metaKey
  if (!meta) return
  if (e.key.toLowerCase() === 'b') { e.preventDefault(); applyBold() }
  if (e.key.toLowerCase() === 's') { e.preventDefault(); exportJson() }
  if (e.key.toLowerCase() === 'p') { e.preventDefault(); printResume() }
}

// ==================== 返回 ====================
function goBack() { router.push('/chat') }

// ==================== 挂载 ====================
onMounted(() => {
  document.addEventListener('keydown', onKeydown)
})
</script>

<style>
/* ====== 全局变量 ====== */
:root {
  --rb-bg: #eef2f7; --rb-panel: #fff; --rb-line: #d7dee8;
  --rb-text: #1f2937; --rb-muted: #6b7280; --rb-accent: #1d4a85;
}

/* ====== 布局 ====== */
.resume-root { height: 100vh; display: flex; flex-direction: column; background: var(--rb-bg); color: var(--rb-text); font-family: "PingFang SC","Microsoft YaHei",sans-serif; }
.resume-root button, .resume-root input, .resume-root textarea, .resume-root select { font: inherit; }

/* 顶栏 */
.topbar {
  position: sticky; top: 0; z-index: 20; display: flex; justify-content: space-between;
  align-items: center; gap: 8px; padding: 10px 14px;
  background: rgba(255,255,255,.96); backdrop-filter: blur(8px); border-bottom: 1px solid var(--rb-line);
  flex-shrink: 0;
}
.tb-left { display: flex; align-items: center; gap: 8px; }
.tb-title { font-weight: 700; font-size: 15px; }
.tb-center, .tb-right { display: flex; gap: 6px; align-items: center; flex-wrap: wrap; }
.save-status { font-size: 11px; color: var(--rb-muted); white-space: nowrap; }
kbd { font-size: 10px; color: #888; background: #f1f3f5; padding: 1px 4px; border-radius: 3px; margin-left: 3px; }

/* 主题面板 */
.theme-panel {
  position: fixed; top: 72px; right: 16px; z-index: 25; width: 260px; padding: 11px;
  border: 1px solid var(--rb-line); border-radius: 12px; background: #fff;
  box-shadow: 0 8px 24px rgba(15,23,42,.16);
}
.th-head { display: flex; justify-content: space-between; align-items: center; font-size: 14px; font-weight: 700; margin-bottom: 8px; }
.th-body { display: flex; flex-direction: column; gap: 8px; }
.th-body label { display: flex; justify-content: space-between; align-items: center; font-size: 12px; color: #4b5563; }
.th-body select { padding: 4px 8px; border-radius: 6px; border: 1px solid #cfd8e6; font-size: 12px; }

/* 双栏 */
.wrap { display: grid; grid-template-columns: 400px 1fr; flex: 1; overflow: hidden; }

/* 编辑器 */
.editor { overflow: auto; border-right: 1px solid var(--rb-line); padding: 10px; background: #f8fafe; }
.ed-box { border: 1px solid var(--rb-line); background: var(--rb-panel); border-radius: 10px; padding: 10px; margin-bottom: 10px; }
.ed-box h3 { margin: 0 0 8px; font-size: 13px; }
.ed-box h4 { margin: 8px 0 4px; font-size: 12px; color: #4b5563; }

.mod-item { display: grid; grid-template-columns: 1fr auto; gap: 6px; align-items: center; border: 1px solid #e2e8f1; border-radius: 8px; padding: 6px; margin-bottom: 5px; background: #fff; }
.mod-item.active { border-color: #3f76bc; }
.mod-item.hide { opacity: 0.5; }
.mod-btn { border: 0; background: transparent; padding: 0; text-align: left; cursor: pointer; font-size: 12px; font-weight: 600; }
.mod-ops { display: flex; gap: 3px; }
.mod-ops button { padding: 1px 6px; font-size: 11px; border-radius: 5px; border: 1px solid #cfd8e6; background: #fff; cursor: pointer; }
.mod-ops button:hover { background: #f0f4ff; }

.fld { width: 100%; padding: 6px 8px; border-radius: 7px; border: 1px solid #cfd8e6; background: #fff; outline: none; font-size: 12px; }
.fld:focus { border-color: #3f76bc; }
textarea.fld { resize: vertical; min-height: 38px; }
.field-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; margin-bottom: 8px; }
.ed-box label { display: flex; flex-direction: column; gap: 3px; font-size: 11px; color: #4b5563; }

.arr-list { margin-top: 8px; }
.arr-item { border: 1px solid #e1e7f0; border-radius: 8px; padding: 6px; background: #fff; margin-bottom: 6px; }
.arr-head { display: flex; justify-content: space-between; align-items: center; font-size: 11px; color: #677085; font-weight: 600; margin-bottom: 4px; }
.arr-ops { display: flex; gap: 3px; }
.arr-ops button { padding: 1px 5px; font-size: 10px; border-radius: 4px; border: 1px solid #cfd8e6; background: #fff; cursor: pointer; }
.add-btn { width: 100%; padding: 5px 8px; border: 1px dashed #7c90af; background: #f6faff; color: #204f90; border-radius: 7px; cursor: pointer; font-size: 12px; margin-top: 4px; }
.add-btn:hover { background: #edf5ff; }

.av-preview { width: 24mm; height: 32mm; border: 1px solid #333; background: #fafafa; display: flex; align-items: center; justify-content: center; overflow: hidden; color: #6b7280; font-size: 11px; cursor: pointer; border-radius: 2px; }
.av-preview.has-img { border-color: #cfd8e6; }
.av-preview img { width: 100%; height: 100%; object-fit: cover; }

/* 预览区 */
.pre { overflow: auto; padding: 14px; }
.page {
  width: 210mm; max-width: 100%; min-height: 297mm; margin: 0 auto;
  background: #fff; border: 1px solid #d9dee7; box-shadow: 0 8px 24px rgba(0,0,0,.08);
  padding: 14mm 13mm 12mm; font-size: var(--fs, 11.5px); line-height: var(--lh, 1.6);
  overflow-wrap: anywhere; box-sizing: border-box;
}
.hd { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 12px; align-items: start; }
.nrow { display: flex; justify-content: flex-start; align-items: flex-end; gap: 16px; flex-wrap: wrap; }
.nm { font-size: 26px; font-weight: 700; line-height: 1.1; }
.mi { font-size: 12px; color: #4b5563; white-space: nowrap; display: inline-flex; align-items: center; gap: 7px; }
.sep { color: #9ca3af; }
.crow { display: flex; gap: 16px; flex-wrap: wrap; margin-top: 7px; }
.av { width: 23mm; height: 30mm; border: 1px solid #333; background: #fafafa; display: flex; align-items: center; justify-content: center; overflow: hidden; color: #6b7280; font-size: 12px; }
.av img { width: 100%; height: 100%; object-fit: cover; }
.sec { margin-top: var(--sg, 14px); break-inside: avoid; }
.st { display: flex; align-items: flex-end; gap: 10px; margin-bottom: 6px; }
.st h2 { margin: 0; font-size: 17px; font-weight: 800; color: var(--rc, #222); white-space: nowrap; }
.st .ln { flex: 1; border-bottom: 1px solid #2c2c2c; transform: translateY(-3px); }
.eh { display: grid; grid-template-columns: 1.5fr 1fr auto; gap: 10px; align-items: baseline; }
.el { display: flex; align-items: baseline; gap: 8px; flex-wrap: wrap; }
.dt { font-size: 11px; color: #6b7280; white-space: nowrap; text-align: right; }
.aw { margin: 4px 0 0; padding-left: 14px; display: grid; grid-template-columns: repeat(var(--cols,2), minmax(0,1fr)); gap: 2px 14px; }
.ent { margin-top: 6px; }
.ehd { display: grid; grid-template-columns: 1fr auto auto; gap: 10px; align-items: center; }
.cmp { font-weight: 700; }
.role { font-weight: 600; white-space: nowrap; }
.ttl { margin-top: 2px; font-weight: 700; }
.bul { margin: 3px 0 0 16px; padding: 0; }
.bul li { margin: 2px 0; }
.plink { font-size: 11px; color: #1f4f8f; text-decoration: none; }
.plink:hover { text-decoration: underline; }
.skill { margin: 4px 0; }
.muted { color: #9ca3af; font-size: 11px; }

/* Toast */
.toastw { position: fixed; right: 16px; top: 16px; z-index: 99; display: grid; gap: 8px; pointer-events: none; }
.toast { background: #111827; color: #fff; padding: 8px 12px; border-radius: 9px; font-size: 12px; animation: tp-in .25s; }
.toast.err { background: #9f1239; }
@keyframes tp-in { from { opacity: 0; transform: translateY(-5px); } to { opacity: 1; transform: none; } }

/* 模式：仅预览 */
.mode-preview .edit-only { display: none !important; }
.mode-preview .wrap { grid-template-columns: 1fr; }
.mode-preview .pre { padding: 8px 0 16px; }

/* AI 结果弹窗 */
.ai-result-overlay { position: fixed; inset: 0; background: rgba(0,0,0,.3); z-index: 30; display: flex; align-items: center; justify-content: center; }
.ai-result-box { background: #fff; border-radius: 12px; padding: 20px; max-width: 700px; max-height: 80vh; overflow: auto; width: 90%; }
.ai-result-box h3 { margin-top: 0; }
.ai-result-close { margin-top: 12px; }

/* 响应式 */
@media (max-width: 1000px) {
  .wrap { grid-template-columns: 1fr; height: auto; }
  .editor { border-right: 0; border-bottom: 1px solid var(--rb-line); max-height: 45vh; }
}

/* 打印 */
@media print {
  @page { size: A4; margin: 0; }
  html, body { background: #fff !important; -webkit-print-color-adjust: exact; print-color-adjust: exact; }
  .topbar, .editor, .theme-panel, .toastw, .edit-only { display: none !important; }
  .wrap { display: block !important; height: auto !important; }
  .pre { padding: 0 !important; overflow: visible !important; }
  .page { width: 210mm !important; max-width: 210mm !important; min-height: 297mm !important; margin: 0 !important; border: 0 !important; box-shadow: none !important; padding: 12mm 12mm 12mm !important; box-sizing: border-box !important; }
  a { color: #111 !important; text-decoration: none !important; }
}
</style>
