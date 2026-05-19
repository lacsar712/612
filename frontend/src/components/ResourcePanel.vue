<template>
  <section class="panel">
    <h2>{{ resource.title }}</h2>
    <div class="main-grid">
      <div>
        <div class="table-tools">
          <button v-if="canExport" class="btn secondary" @click="downloadExport">导出员工信息</button>
        </div>
        <div class="notice error" v-if="listError">{{ listError }}</div>
        <table class="table">
          <thead>
            <tr>
              <th>ID</th>
              <th v-for="field in displayFields" :key="field.key">{{ field.label }}</th>
              <th v-if="showOperationsColumn">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in items" :key="item.id">
              <td>{{ item.id }}</td>
              <td v-for="field in displayFields" :key="field.key">
                <span>{{ formatValue(item, field) }}</span>
              </td>
              <td v-if="showOperationsColumn" class="row-actions">
                <button v-if="!effectiveReadOnly" class="btn secondary" @click="startEdit(item)">编辑</button>
                <button v-if="!effectiveReadOnly" class="btn" @click="remove(item)">删除</button>
                <button
                  v-if="resource.insight"
                  class="btn secondary"
                  @click="showInsight(item)"
                >
                  {{ resource.insight.label }}
                </button>
                <button
                  v-for="action in getVisibleActions(item)"
                  :key="action.key"
                  class="btn secondary"
                  @click="runAction(item, action)"
                >
                  {{ action.label }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>

        <div class="notice" v-if="!items.length && !listError">暂无数据</div>

        <div class="panel nested" v-if="insightTitle">
          <h3>{{ insightTitle }}</h3>
          <div class="notice error" v-if="insightError">{{ insightError }}</div>
          <table class="table" v-if="insightRows.length">
            <thead>
              <tr>
                <th v-for="column in resource.insight.columns" :key="column.key">{{ column.label }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, index) in insightRows" :key="index">
                <td v-for="column in resource.insight.columns" :key="column.key">
                  {{ formatRawValue(row[column.key]) }}
                </td>
              </tr>
            </tbody>
          </table>
          <div class="notice" v-if="!insightRows.length && !insightError">暂无明细记录</div>
        </div>
      </div>

      <div>
        <div class="form-grid" v-if="!effectiveReadOnly">
          <label v-for="field in resource.fields" :key="field.key">
            {{ field.label }}

            <select
              v-if="field.relation && field.multiple"
              v-model="form[field.key]"
              multiple
              :size="Math.min(6, Math.max(3, getRelationOptions(field).length || 3))"
            >
              <option
                v-for="option in getRelationOptions(field)"
                :key="getOptionValue(field, option)"
                :value="String(getOptionValue(field, option))"
              >
                {{ getOptionLabel(field, option) }}
              </option>
            </select>

            <select
              v-else-if="field.relation"
              v-model="form[field.key]"
            >
              <option value="">请选择</option>
              <option
                v-for="option in getRelationOptions(field)"
                :key="getOptionValue(field, option)"
                :value="String(getOptionValue(field, option))"
              >
                {{ getOptionLabel(field, option) }}
              </option>
            </select>

            <textarea
              v-else-if="field.type === 'textarea'"
              v-model="form[field.key]"
            ></textarea>

            <select v-else-if="field.type === 'select'" v-model="form[field.key]">
              <option value="">请选择</option>
              <option v-for="option in field.options || []" :key="option" :value="option">{{ option }}</option>
            </select>

            <select v-else-if="field.type === 'boolean'" v-model="form[field.key]">
              <option :value="true">启用</option>
              <option :value="false">停用</option>
            </select>

            <input
              v-else
              :type="field.inputType || 'text'"
              :min="field.min"
              :max="field.max"
              :placeholder="field.placeholder || ''"
              v-model="form[field.key]"
            />

            <small class="field-error" v-if="formErrors[field.key]">{{ formErrors[field.key] }}</small>
          </label>

          <div class="form-actions">
            <button class="btn primary" @click="submit">{{ editId ? '更新' : '新增' }}</button>
            <button class="btn secondary" @click="resetForm">清空</button>
          </div>

          <div class="notice success" v-if="submitSuccess">{{ submitSuccess }}</div>
          <div class="notice error" v-if="submitError">{{ submitError }}</div>
          <div class="notice" v-if="resource.note">{{ resource.note }}</div>
        </div>
        <div v-else class="notice">该模块为只读展示。</div>
      </div>
    </div>

    <div v-if="confirmDialog.visible" class="dialog-backdrop">
      <div class="dialog-card">
        <h3>{{ confirmDialog.title }}</h3>
        <p>{{ confirmDialog.message }}</p>
        <div class="dialog-actions">
          <button class="btn secondary" @click="closeConfirmDialog">取消</button>
          <button class="btn primary" @click="confirmDialog.onConfirm">{{ confirmDialog.confirmText }}</button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';

const props = defineProps({
  resource: {
    type: Object,
    required: true
  },
  apiBase: {
    type: String,
    required: true
  },
  token: {
    type: String,
    default: ''
  },
  user: {
    type: Object,
    default: null
  }
});

const items = ref([]);
const editId = ref(null);
const form = reactive({});
const formErrors = reactive({});
const relationOptions = reactive({});

const listError = ref('');
const submitError = ref('');
const submitSuccess = ref('');
const insightRows = ref([]);
const insightTitle = ref('');
const insightError = ref('');
const confirmDialog = reactive({
  visible: false,
  title: '',
  message: '',
  confirmText: '确认',
  onConfirm: null
});

const currentRoleCode = computed(() => props.user?.role?.code || 'STAFF');
const effectiveReadOnly = computed(() => Boolean(props.resource.effectiveReadOnly ?? props.resource.readOnly));
const canExport = computed(() => {
  if (!props.resource.exportEndpoint) {
    return false;
  }
  return !props.resource.exportRoles || props.resource.exportRoles.includes(currentRoleCode.value);
});

const displayFields = computed(() => {
  const fields = props.resource.displayFields || props.resource.fields || [];
  return fields.filter((field) => !field.hideInTable);
});

const relationFields = computed(() =>
  (props.resource.fields || []).filter((field) => field.relation && field.optionsEndpoint)
);

const canUseAction = (action, item = null) => {
  if (action.roles?.length && !action.roles.includes(currentRoleCode.value)) {
    return false;
  }
  if (item && typeof action.when === 'function' && !action.when(item, currentRoleCode.value)) {
    return false;
  }
  return true;
};

const getVisibleActions = (item) => (props.resource.actions || []).filter((action) => canUseAction(action, item));

const showOperationsColumn = computed(() => {
  if (props.resource.insight || !effectiveReadOnly.value) {
    return true;
  }
  return (props.resource.actions || []).some((action) => canUseAction(action));
});

const authHeaders = () => (props.token ? { Authorization: `Bearer ${props.token}` } : {});

const resetMessages = () => {
  submitError.value = '';
  submitSuccess.value = '';
};

const openConfirmDialog = ({ title, message, confirmText = '确认', onConfirm }) => {
  confirmDialog.visible = true;
  confirmDialog.title = title;
  confirmDialog.message = message;
  confirmDialog.confirmText = confirmText;
  confirmDialog.onConfirm = async () => {
    closeConfirmDialog();
    await onConfirm();
  };
};

const closeConfirmDialog = () => {
  confirmDialog.visible = false;
  confirmDialog.title = '';
  confirmDialog.message = '';
  confirmDialog.confirmText = '确认';
  confirmDialog.onConfirm = null;
};

const resetForm = () => {
  editId.value = null;
  resetMessages();
  Object.keys(formErrors).forEach((key) => {
    formErrors[key] = '';
  });
  (props.resource.fields || []).forEach((field) => {
    if (field.multiple) {
      form[field.key] = Array.isArray(field.defaultValue) ? [...field.defaultValue] : [];
      return;
    }
    if (field.type === 'boolean') {
      form[field.key] = field.defaultValue ?? true;
      return;
    }
    form[field.key] = field.defaultValue ?? '';
  });
};

const parseErrorMessage = async (response) => {
  try {
    const data = await response.json();
    if (data?.message) {
      return data.message;
    }
    if (data?.errors) {
      const firstError = Object.values(data.errors)[0];
      if (firstError) {
        return firstError;
      }
    }
  } catch (error) {
    return `请求失败（${response.status}）`;
  }
  return `请求失败（${response.status}）`;
};

const fetchItems = async () => {
  listError.value = '';
  const response = await fetch(`${props.apiBase}${props.resource.endpoint}`, {
    headers: authHeaders()
  });
  if (!response.ok) {
    listError.value = await parseErrorMessage(response);
    items.value = [];
    return;
  }
  items.value = await response.json();
};

const fetchRelationOptions = async () => {
  if (effectiveReadOnly.value || !relationFields.value.length) {
    return;
  }
  const tasks = relationFields.value.map(async (field) => {
    const response = await fetch(`${props.apiBase}${field.optionsEndpoint}`, {
      headers: authHeaders()
    });
    relationOptions[field.key] = response.ok ? await response.json() : [];
  });
  await Promise.all(tasks);
};

const normalizeDateTimeValue = (value) => {
  if (!value || typeof value !== 'string') {
    return value;
  }
  if (value.length === 16 && value.includes('T')) {
    return `${value}:00`;
  }
  return value;
};

const buildPayload = () => {
  const payload = {};
  (props.resource.fields || []).forEach((field) => {
    const value = form[field.key];
    if (field.relation) {
      if (field.sendAsIds) {
        if (field.multiple) {
          payload[field.key] = (Array.isArray(value) ? value : [])
            .filter((v) => v !== '' && v !== null && v !== undefined)
            .map((v) => Number(v))
            .filter((v) => Number.isFinite(v));
        } else {
          payload[field.key] =
            value === '' || value === null || value === undefined ? null : Number(value);
        }
        return;
      }
      if (field.multiple) {
        const relationValues = Array.isArray(value) ? value : [];
        payload[field.relation] = relationValues
          .filter((v) => v !== '' && v !== null && v !== undefined)
          .map((v) => ({ id: Number(v) }))
          .filter((item) => Number.isFinite(item.id));
      } else {
        payload[field.relation] =
          value === '' || value === null || value === undefined ? null : { id: Number(value) };
      }
      return;
    }

    if (field.inputType === 'number') {
      payload[field.key] = value === '' ? null : Number(value);
      return;
    }

    if (field.inputType === 'datetime-local') {
      payload[field.key] = value === '' ? null : normalizeDateTimeValue(value);
      return;
    }

    payload[field.key] = value === '' ? null : value;
  });
  return payload;
};

const isEmptyValue = (field, value) => {
  if (field.multiple) {
    return !Array.isArray(value) || value.length === 0;
  }
  if (field.type === 'boolean') {
    return value === null || value === undefined || value === '';
  }
  return value === null || value === undefined || String(value).trim() === '';
};

const validateField = (field) => {
  const value = form[field.key];
  const required = field.required || (field.requiredOnCreate && !editId.value);
  if (required && isEmptyValue(field, value)) {
    return `${field.label}不能为空`;
  }
  if (isEmptyValue(field, value)) {
    return '';
  }
  if (field.validate === 'phone' && !/^1[3-9]\d{9}$/.test(String(value))) {
    return `${field.label}格式不正确`;
  }
  if (
    field.validate === 'email'
    && !/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(String(value))
  ) {
    return `${field.label}格式不正确`;
  }
  if (field.validate === 'password') {
    const password = String(value);
    if (password.length < 6) {
      return `${field.label}长度不能少于6位`;
    }
    if (!/[A-Za-z]/.test(password) || !/\d/.test(password)) {
      return `${field.label}需同时包含字母和数字`;
    }
  }
  if (field.inputType === 'number') {
    const num = Number(value);
    if (Number.isNaN(num)) {
      return `${field.label}必须是数字`;
    }
    if (field.min !== undefined && num < field.min) {
      return `${field.label}不能小于${field.min}`;
    }
    if (field.max !== undefined && num > field.max) {
      return `${field.label}不能大于${field.max}`;
    }
  }
  return '';
};

const validateForm = () => {
  let valid = true;
  (props.resource.fields || []).forEach((field) => {
    const message = validateField(field);
    formErrors[field.key] = message;
    if (message) {
      valid = false;
    }
  });
  return valid;
};

const submit = async () => {
  resetMessages();
  if (!validateForm()) {
    submitError.value = '请先修正表单校验错误。';
    return;
  }

  const payload = buildPayload();
  const url = editId.value
    ? `${props.apiBase}${props.resource.endpoint}/${editId.value}`
    : `${props.apiBase}${props.resource.endpoint}`;
  const method = editId.value ? 'PUT' : 'POST';

  const response = await fetch(url, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders()
    },
    body: JSON.stringify(payload)
  });

  if (!response.ok) {
    submitError.value = await parseErrorMessage(response);
    return;
  }

  submitSuccess.value = editId.value ? '更新成功。' : '新增成功。';
  resetForm();
  await fetchItems();
};

const performDelete = async (id) => {
  resetMessages();
  const response = await fetch(`${props.apiBase}${props.resource.endpoint}/${id}`, {
    method: 'DELETE',
    headers: authHeaders()
  });
  if (!response.ok) {
    submitError.value = await parseErrorMessage(response);
    return;
  }
  submitSuccess.value = '删除成功。';
  await fetchItems();
};

const remove = (item) => {
  openConfirmDialog({
    title: '确认删除',
    message: `确认删除记录 ${item.orderNo || item.code || item.name || item.id} 吗？`,
    confirmText: '删除',
    onConfirm: async () => {
      await performDelete(item.id);
    }
  });
};

const startEdit = (item) => {
  editId.value = item.id;
  resetMessages();
  (props.resource.fields || []).forEach((field) => {
    if (field.relation) {
      if (field.multiple) {
        const relationList = Array.isArray(item[field.relation]) ? item[field.relation] : [];
        form[field.key] = relationList
          .map((entry) => entry?.id)
          .filter((id) => id !== null && id !== undefined)
          .map((id) => String(id));
      } else {
        form[field.key] = item[field.relation]?.id ? String(item[field.relation].id) : '';
      }
      return;
    }

    if (field.type === 'boolean') {
      form[field.key] = item[field.key] ?? false;
      return;
    }

    if (field.inputType === 'datetime-local' && typeof item[field.key] === 'string') {
      form[field.key] = item[field.key].slice(0, 16);
      return;
    }

    form[field.key] = item[field.key] ?? '';
  });
};

const formatRelatedValue = (related) => {
  if (!related) {
    return '-';
  }
  return related.name || related.code || related.username || related.empNo || related.id;
};

const formatValue = (item, field) => {
  if (field.relation) {
    const related = item[field.relation];
    if (Array.isArray(related)) {
      if (!related.length) {
        return '-';
      }
      return related.map((entry) => formatRelatedValue(entry)).join('、');
    }
    return formatRelatedValue(related);
  }

  const value = item[field.key];
  if (value === null || value === undefined || value === '') {
    return '-';
  }
  if (field.type === 'boolean') {
    return value ? '启用' : '停用';
  }
  if (typeof value === 'string' && value.includes('T')) {
    return value.replace('T', ' ');
  }
  return value;
};

const formatRawValue = (value) => {
  if (value === null || value === undefined || value === '') {
    return '-';
  }
  if (typeof value === 'string' && value.includes('T')) {
    return value.replace('T', ' ');
  }
  return value;
};

const getRelationOptions = (field) => {
  const options = relationOptions[field.key] || [];
  if (props.resource.key === 'warehouses' && field.key === 'managerIds') {
    return options.filter((option) => {
      const managedWarehouseId = option?.managedWarehouse?.id;
      return managedWarehouseId === null
          || managedWarehouseId === undefined
          || (editId.value !== null && Number(managedWarehouseId) === Number(editId.value));
    });
  }
  return options;
};

const getOptionValue = (field, option) => option[field.optionValue || 'id'];

const getOptionLabel = (field, option) => {
  const labelKey = field.optionLabel || 'name';
  const label = option[labelKey] || option.name || option.code || option.username || option.empNo || option.id;
  return `${label}（ID:${option.id}）`;
};

const showInsight = async (item) => {
  insightError.value = '';
  insightRows.value = [];
  const endpoint = props.resource.insight.endpointTemplate.replace('{id}', item.id);
  insightTitle.value = `${props.resource.insight.title}：${item.name || item.code || item.id}`;
  const response = await fetch(`${props.apiBase}${endpoint}`, {
    headers: authHeaders()
  });
  if (!response.ok) {
    insightError.value = await parseErrorMessage(response);
    return;
  }
  const rows = await response.json();
  insightRows.value = Array.isArray(rows) ? rows : [];
};

const executeAction = async (item, action) => {
  resetMessages();
  const endpoint = action.endpointTemplate.replace('{id}', item.id);
  const body = typeof action.body === 'function' ? action.body(item) : action.body;
  const response = await fetch(`${props.apiBase}${endpoint}`, {
    method: action.method || 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders()
    },
    body: body === undefined ? undefined : JSON.stringify(body)
  });
  if (!response.ok) {
    submitError.value = await parseErrorMessage(response);
    return;
  }
  submitSuccess.value = action.successMessage || `${action.label}成功。`;
  await fetchItems();
};

const runAction = (item, action) => {
  const title = action.confirmTitle || action.label;
  const message = typeof action.confirmMessage === 'function'
    ? action.confirmMessage(item)
    : (action.confirmMessage || `确认执行“${action.label}”吗？`);
  openConfirmDialog({
    title,
    message,
    confirmText: action.confirmText || action.label,
    onConfirm: async () => {
      await executeAction(item, action);
    }
  });
};

const parseFileName = (response) => {
  const disposition = response.headers.get('content-disposition') || '';
  const match = disposition.match(/filename="(.+?)"/);
  if (match?.[1]) {
    return match[1];
  }
  return props.resource.exportFileName || 'export.csv';
};

const downloadExport = async () => {
  resetMessages();
  const response = await fetch(`${props.apiBase}${props.resource.exportEndpoint}`, {
    headers: authHeaders()
  });
  if (!response.ok) {
    submitError.value = await parseErrorMessage(response);
    return;
  }
  const blob = await response.blob();
  const fileName = parseFileName(response);
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
  submitSuccess.value = '导出成功。';
};

const resetInsight = () => {
  insightRows.value = [];
  insightTitle.value = '';
  insightError.value = '';
};

onMounted(async () => {
  resetForm();
  await Promise.all([fetchItems(), fetchRelationOptions()]);
});

watch(
  () => props.resource,
  async () => {
    resetForm();
    resetInsight();
    await Promise.all([fetchItems(), fetchRelationOptions()]);
  }
);
</script>
