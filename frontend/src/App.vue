<template>
  <div class="app-shell">
    <div v-if="!token" class="login-shell">
      <div class="login-card">
        <div class="login-brand">
          <div class="logo-mark">LZ</div>
          <div>
            <h1>联志玩具礼品仓库管理系统</h1>
            <p>苍梧 · 统一仓储业务平台</p>
          </div>
        </div>
        <div class="login-form">
          <label>
            用户名
            <input v-model="loginForm.username" placeholder="请输入用户名" />
          </label>
          <label>
            密码
            <input v-model="loginForm.password" type="password" placeholder="请输入密码" />
          </label>
          <button class="btn primary" @click="submitLogin">登录</button>
          <div class="login-note" v-if="loginError">{{ loginError }}</div>
        </div>
      </div>
    </div>

    <div v-else class="workspace">
      <aside class="sidebar">
        <div class="brand-block">
          <div class="logo-mark">LZ</div>
          <div>
            <h2>联志仓库管理</h2>
            <p>{{ user?.employee?.name || '系统管理员' }}</p>
            <p>{{ currentRoleLabel }}</p>
          </div>
        </div>
        <nav class="side-nav">
          <button
            v-for="resource in availableResources"
            :key="resource.key"
            :class="{ active: resource.key === activeKey }"
            @click="activeKey = resource.key"
          >
            {{ resource.title }}
          </button>
        </nav>
        <div class="side-footer">
          <span class="tag">API {{ apiBase }}</span>
          <button class="btn secondary" @click="logout">退出登录</button>
        </div>
      </aside>

      <main class="content">
        <header class="content-header">
          <div>
            <h1>{{ activeResource?.title }}</h1>
            <p>仓储业务数据管理与追溯 · {{ currentRoleLabel }}</p>
          </div>
          <div class="header-meta">
            <span class="chip">在线</span>
            <span class="chip">业务日期 {{ today }}</span>
          </div>
        </header>

        <ResourcePanel
          v-if="activeResource"
          :resource="activeResource"
          :api-base="apiBase"
          :token="token"
          :user="user"
        />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue';
import ResourcePanel from './components/ResourcePanel.vue';

const apiBase = window.__API_BASE__ || '/api-7f3b2';
const token = ref(localStorage.getItem('wms_token') || '');
const user = ref(JSON.parse(localStorage.getItem('wms_user') || 'null'));
const loginForm = reactive({ username: '', password: '' });
const loginError = ref('');
const today = new Date().toLocaleDateString('zh-CN').replace(/\//g, '-');
const roleLabelMap = {
  ADMIN: '系统管理员',
  KEEPER: '仓库保管员',
  STAFF: '业务员工'
};

const currentRoleCode = computed(() => user.value?.role?.code || 'STAFF');
const currentRoleLabel = computed(() => roleLabelMap[currentRoleCode.value] || '业务员工');
const hasRole = (roles, roleCode = currentRoleCode.value) => !roles?.length || roles.includes(roleCode);

const resources = [
  {
    key: 'warehouses',
    title: '仓库管理',
    endpoint: '/warehouses',
    roles: ['ADMIN', 'KEEPER', 'STAFF'],
    writeRoles: ['ADMIN'],
    note: '一个仓库可配置多位负责员工；同一员工只能负责一个仓库（可多选）。',
    insight: {
      label: '出库商品',
      title: '仓库出库商品统计',
      endpointTemplate: '/warehouses/{id}/outbound-products',
      columns: [
        { key: 'productCode', label: '商品编号' },
        { key: 'productName', label: '商品名称' },
        { key: 'totalQuantity', label: '累计出库数量' },
        { key: 'orderCount', label: '出库单数' }
      ]
    },
    displayFields: [
      { key: 'code', label: '仓库编号' },
      { key: 'name', label: '仓库名称' },
      { key: 'address', label: '地址' },
      { key: 'type', label: '仓库类型' },
      { key: 'enabled', label: '启用状态', type: 'boolean' },
      { key: 'managerIds', label: '协作负责人', relation: 'managers' }
    ],
    fields: [
      { key: 'code', label: '仓库编号', required: true },
      { key: 'name', label: '仓库名称', required: true },
      { key: 'address', label: '地址', required: true },
      { key: 'type', label: '仓库类型', required: true, type: 'select', options: ['成品仓', '原料仓', '辅料仓', '中转仓'] },
      { key: 'enabled', label: '启用状态', type: 'boolean', required: true, defaultValue: true },
      {
        key: 'managerIds',
        label: '协作负责人',
        relation: 'managers',
        multiple: true,
        sendAsIds: true,
        required: true,
        optionsEndpoint: '/employees',
        optionLabel: 'name'
      }
    ]
  },
  {
    key: 'suppliers',
    title: '供应商管理',
    endpoint: '/suppliers',
    roles: ['ADMIN', 'KEEPER', 'STAFF'],
    writeRoles: ['ADMIN'],
    insight: {
      label: '供货记录',
      title: '供应商历史供货记录',
      endpointTemplate: '/suppliers/{id}/histories',
      columns: [
        { key: 'orderNo', label: '入库单号' },
        { key: 'productName', label: '商品' },
        { key: 'warehouseName', label: '仓库' },
        { key: 'quantity', label: '数量' },
        { key: 'status', label: '状态' },
        { key: 'inboundTime', label: '入库时间' }
      ]
    },
    fields: [
      { key: 'code', label: '供应商编号', required: true },
      { key: 'name', label: '供应商名称', required: true },
      { key: 'contactName', label: '联系人', required: true },
      { key: 'phone', label: '联系电话', required: true, validate: 'phone' },
      { key: 'email', label: '电子邮箱', required: true, validate: 'email' },
      { key: 'supplyType', label: '供货类型', required: true, type: 'select', options: ['原料', '包材', '辅料', '成品'] },
      { key: 'cooperationType', label: '合作类型', required: true, type: 'select', options: ['长期合作', '框架协议', '临时采购'] },
      { key: 'status', label: '合作状态', required: true, type: 'select', options: ['ACTIVE', 'INACTIVE', 'PAUSED'] }
    ]
  },
  {
    key: 'products',
    title: '商品管理',
    endpoint: '/products',
    roles: ['ADMIN', 'KEEPER', 'STAFF'],
    writeRoles: ['ADMIN'],
    insight: {
      label: '出库仓库',
      title: '商品跨仓库出库统计',
      endpointTemplate: '/products/{id}/outbound-warehouses',
      columns: [
        { key: 'warehouseCode', label: '仓库编号' },
        { key: 'warehouseName', label: '仓库名称' },
        { key: 'totalQuantity', label: '累计出库数量' },
        { key: 'orderCount', label: '出库单数' }
      ]
    },
    fields: [
      { key: 'code', label: '商品编号', required: true },
      { key: 'name', label: '商品名称', required: true },
      { key: 'sku', label: 'SKU条码', required: true },
      { key: 'spec', label: '规格型号', required: true },
      { key: 'unit', label: '单位', required: true, type: 'select', options: ['件', '箱', '袋', '个'] },
      { key: 'description', label: '商品描述', type: 'textarea' }
    ]
  },
  {
    key: 'employees',
    title: '员工管理',
    endpoint: '/employees',
    roles: ['ADMIN', 'KEEPER', 'STAFF'],
    writeRoles: ['ADMIN'],
    exportRoles: ['ADMIN'],
    exportEndpoint: '/employees/export',
    exportFileName: 'employees.csv',
    fields: [
      { key: 'empNo', label: '工号', required: true },
      { key: 'name', label: '姓名', required: true },
      { key: 'gender', label: '性别', required: true, type: 'select', options: ['男', '女'] },
      { key: 'birthDate', label: '出生日期', inputType: 'date' },
      { key: 'phone', label: '联系电话', required: true, validate: 'phone' },
      { key: 'email', label: '电子邮箱', required: true, validate: 'email' },
      { key: 'roleType', label: '角色类型', required: true, type: 'select', options: ['ADMIN', 'KEEPER', 'STAFF'] },
      {
        key: 'departmentId',
        label: '所属部门',
        relation: 'department',
        required: true,
        optionsEndpoint: '/departments',
        optionLabel: 'name'
      }
    ]
  },
  {
    key: 'inbound',
    title: '入库管理',
    endpoint: '/inbound-orders',
    roles: ['ADMIN', 'KEEPER', 'STAFF'],
    writeRoles: ['ADMIN', 'STAFF'],
    note: '入库单关联对象已改为下拉选择。',
    actions: [
      {
        key: 'approve',
        label: '审核通过',
        roles: ['ADMIN', 'KEEPER'],
        endpointTemplate: '/inbound-orders/{id}/audit',
        method: 'PUT',
        body: { status: 'APPROVED' },
        successMessage: '入库单已审核通过。',
        confirmTitle: '确认通过审核',
        confirmMessage: (item) => `确认将入库单 ${item.orderNo || item.id} 标记为审核通过吗？`,
        when: (item) => item.status === 'PENDING'
      },
      {
        key: 'reject',
        label: '驳回',
        roles: ['ADMIN', 'KEEPER'],
        endpointTemplate: '/inbound-orders/{id}/audit',
        method: 'PUT',
        body: { status: 'REJECTED' },
        successMessage: '入库单已驳回。',
        confirmTitle: '确认驳回审核',
        confirmMessage: (item) => `确认驳回入库单 ${item.orderNo || item.id} 吗？`,
        when: (item) => item.status === 'PENDING'
      }
    ],
    fields: [
      { key: 'orderNo', label: '入库单号', required: true },
      { key: 'quantity', label: '数量', required: true, inputType: 'number', min: 1 },
      { key: 'inboundType', label: '入库类型', required: true, type: 'select', options: ['采购入库', '退货入库', '调拨入库'] },
      { key: 'status', label: '入库状态', required: true, type: 'select', options: ['DRAFT', 'PENDING', 'APPROVED', 'REJECTED'] },
      { key: 'inboundTime', label: '入库时间', required: true, inputType: 'datetime-local' },
      { key: 'remark', label: '备注', type: 'textarea' },
      {
        key: 'warehouseId',
        label: '仓库',
        relation: 'warehouse',
        required: true,
        optionsEndpoint: '/warehouses',
        optionLabel: 'name'
      },
      {
        key: 'supplierId',
        label: '供应商',
        relation: 'supplier',
        required: true,
        optionsEndpoint: '/suppliers',
        optionLabel: 'name'
      },
      {
        key: 'productId',
        label: '商品',
        relation: 'product',
        required: true,
        optionsEndpoint: '/products',
        optionLabel: 'name'
      },
      {
        key: 'createdById',
        label: '创建人',
        relation: 'createdBy',
        required: true,
        optionsEndpoint: '/employees',
        optionLabel: 'name'
      }
    ]
  },
  {
    key: 'outbound',
    title: '出库管理',
    endpoint: '/outbound-orders',
    roles: ['ADMIN', 'KEEPER', 'STAFF'],
    writeRoles: ['ADMIN', 'STAFF'],
    note: '支持跨仓库多商品出库业务，关联对象已改为下拉选择。',
    actions: [
      {
        key: 'approve',
        label: '审核通过',
        roles: ['ADMIN', 'KEEPER'],
        endpointTemplate: '/outbound-orders/{id}/audit',
        method: 'PUT',
        body: { status: 'APPROVED' },
        successMessage: '出库单已审核通过。',
        confirmTitle: '确认通过审核',
        confirmMessage: (item) => `确认将出库单 ${item.orderNo || item.id} 标记为审核通过吗？`,
        when: (item) => item.status === 'PENDING'
      },
      {
        key: 'reject',
        label: '驳回',
        roles: ['ADMIN', 'KEEPER'],
        endpointTemplate: '/outbound-orders/{id}/audit',
        method: 'PUT',
        body: { status: 'REJECTED' },
        successMessage: '出库单已驳回。',
        confirmTitle: '确认驳回审核',
        confirmMessage: (item) => `确认驳回出库单 ${item.orderNo || item.id} 吗？`,
        when: (item) => item.status === 'PENDING'
      }
    ],
    fields: [
      { key: 'orderNo', label: '出库单号', required: true },
      { key: 'quantity', label: '数量', required: true, inputType: 'number', min: 1 },
      { key: 'outboundType', label: '出库类型', required: true, type: 'select', options: ['销售出库', '调拨出库', '报损出库'] },
      { key: 'status', label: '出库状态', required: true, type: 'select', options: ['DRAFT', 'PENDING', 'APPROVED', 'REJECTED'] },
      { key: 'outboundTime', label: '出库时间', required: true, inputType: 'datetime-local' },
      { key: 'remark', label: '备注', type: 'textarea' },
      {
        key: 'warehouseId',
        label: '仓库',
        relation: 'warehouse',
        required: true,
        optionsEndpoint: '/warehouses',
        optionLabel: 'name'
      },
      {
        key: 'productId',
        label: '商品',
        relation: 'product',
        required: true,
        optionsEndpoint: '/products',
        optionLabel: 'name'
      },
      {
        key: 'createdById',
        label: '创建人',
        relation: 'createdBy',
        required: true,
        optionsEndpoint: '/employees',
        optionLabel: 'name'
      }
    ]
  },
  {
    key: 'stockChecks',
    title: '盘点单管理',
    endpoint: '/stock-checks',
    roles: ['ADMIN', 'KEEPER'],
    writeRoles: ['ADMIN', 'KEEPER'],
    note: '盘点单生成后可用于核对账实差异。',
    fields: [
      { key: 'checkNo', label: '盘点编号', required: true },
      { key: 'checkDate', label: '盘点日期', required: true, inputType: 'date' },
      { key: 'bookQty', label: '账面数量', required: true, inputType: 'number', min: 0 },
      { key: 'actualQty', label: '实际数量', required: true, inputType: 'number', min: 0 },
      {
        key: 'warehouseId',
        label: '仓库',
        relation: 'warehouse',
        required: true,
        optionsEndpoint: '/warehouses',
        optionLabel: 'name'
      },
      {
        key: 'productId',
        label: '商品',
        relation: 'product',
        required: true,
        optionsEndpoint: '/products',
        optionLabel: 'name'
      },
      {
        key: 'createdById',
        label: '盘点人',
        relation: 'createdBy',
        required: true,
        optionsEndpoint: '/employees',
        optionLabel: 'name'
      }
    ]
  },
  {
    key: 'stockFlows',
    title: '库存流水',
    endpoint: '/stock-flows',
    roles: ['ADMIN', 'KEEPER', 'STAFF'],
    readOnly: true,
    displayFields: [
      { key: 'flowNo', label: '流水号' },
      { key: 'flowType', label: '类型' },
      { key: 'quantity', label: '数量' },
      { key: 'flowTime', label: '时间' },
      { key: 'refType', label: '来源类型' },
      { key: 'refNo', label: '来源单号' },
      { key: 'warehouseId', label: '仓库', relation: 'warehouse' },
      { key: 'productId', label: '商品', relation: 'product' },
      { key: 'operatorId', label: '操作人', relation: 'operator' }
    ],
    fields: []
  },
  {
    key: 'departments',
    title: '部门管理',
    endpoint: '/departments',
    roles: ['ADMIN'],
    writeRoles: ['ADMIN'],
    fields: [
      { key: 'code', label: '部门编号', required: true },
      { key: 'name', label: '部门名称', required: true },
      {
        key: 'parentId',
        label: '上级部门',
        relation: 'parent',
        optionsEndpoint: '/departments',
        optionLabel: 'name'
      }
    ]
  },
  {
    key: 'roles',
    title: '角色管理',
    endpoint: '/roles',
    roles: ['ADMIN'],
    writeRoles: ['ADMIN'],
    note: '在此模块直接分配角色权限（可多选）。',
    displayFields: [
      { key: 'code', label: '角色编码' },
      { key: 'name', label: '角色名称' },
      { key: 'description', label: '描述' },
      { key: 'permissionIds', label: '权限分配', relation: 'permissions' }
    ],
    fields: [
      { key: 'code', label: '角色编码', required: true },
      { key: 'name', label: '角色名称', required: true },
      { key: 'description', label: '描述', type: 'textarea' },
      {
        key: 'permissionIds',
        label: '权限分配',
        relation: 'permissions',
        multiple: true,
        optionsEndpoint: '/permissions',
        optionLabel: 'name'
      }
    ]
  },
  {
    key: 'permissions',
    title: '权限管理',
    endpoint: '/permissions',
    roles: ['ADMIN'],
    writeRoles: ['ADMIN'],
    fields: [
      { key: 'code', label: '权限编码', required: true },
      { key: 'name', label: '权限名称', required: true },
      { key: 'description', label: '描述', type: 'textarea' }
    ]
  },
  {
    key: 'users',
    title: '用户管理',
    endpoint: '/users',
    roles: ['ADMIN'],
    writeRoles: ['ADMIN'],
    note: '密码输入明文即可，系统会自动加密；编辑时密码留空表示不修改。',
    displayFields: [
      { key: 'username', label: '用户名' },
      { key: 'enabled', label: '启用状态', type: 'boolean' },
      { key: 'roleId', label: '角色', relation: 'role' },
      { key: 'employeeId', label: '员工', relation: 'employee' }
    ],
    fields: [
      { key: 'username', label: '用户名', required: true },
      {
        key: 'password',
        label: '登录密码',
        inputType: 'password',
        validate: 'password',
        placeholder: '至少6位，需包含字母和数字',
        requiredOnCreate: true,
        hideInTable: true
      },
      { key: 'enabled', label: '启用状态', type: 'boolean', required: true, defaultValue: true },
      {
        key: 'roleId',
        label: '角色',
        relation: 'role',
        required: true,
        optionsEndpoint: '/roles',
        optionLabel: 'name'
      },
      {
        key: 'employeeId',
        label: '员工',
        relation: 'employee',
        required: true,
        optionsEndpoint: '/employees',
        optionLabel: 'name'
      }
    ]
  }
];

const availableResources = computed(() => resources
  .filter((resource) => hasRole(resource.roles))
  .map((resource) => ({
    ...resource,
    effectiveReadOnly: Boolean(resource.readOnly) || !hasRole(resource.writeRoles)
  })));

const activeKey = ref(resources[0].key);
const activeResource = computed(() => availableResources.value.find((r) => r.key === activeKey.value));

watch(availableResources, (nextResources) => {
  if (!nextResources.length) {
    activeKey.value = '';
    return;
  }
  if (!nextResources.some((resource) => resource.key === activeKey.value)) {
    activeKey.value = nextResources[0].key;
  }
}, { immediate: true });

const submitLogin = async () => {
  loginError.value = '';
  const response = await fetch(`${apiBase}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(loginForm)
  });
  if (!response.ok) {
    loginError.value = '用户名或密码错误，请重试。';
    return;
  }
  const data = await response.json();
  token.value = data.token;
  user.value = data.user;
  localStorage.setItem('wms_token', token.value);
  localStorage.setItem('wms_user', JSON.stringify(user.value));
};

const logout = () => {
  token.value = '';
  user.value = null;
  localStorage.removeItem('wms_token');
  localStorage.removeItem('wms_user');
};
</script>
