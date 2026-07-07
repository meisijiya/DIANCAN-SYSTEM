import { $t } from '@/locales';
import dayjs from 'dayjs';

const ISO_DATE_TIME_PATTERN =
  /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,3})?(?:Z|[+-]\d{2}:\d{2})?$/;

const DATE_FIELD_PATTERN = /(time|date)$/i;

/**
 * Transform record to option
 *
 * @example
 *   ```ts
 *   const record = {
 *     key1: 'label1',
 *     key2: 'label2'
 *   };
 *   const options = transformRecordToOption(record);
 *   // [
 *   //   { value: 'key1', label: 'label1' },
 *   //   { value: 'key2', label: 'label2' }
 *   // ]
 *   ```;
 *
 * @param record
 */
export function transformRecordToOption<T extends Record<string, string>>(record: T) {
  return Object.entries(record).map(([value, label]) => ({
    value,
    label
  })) as CommonType.Option<keyof T, T[keyof T]>[];
}

/**
 * Translate options
 *
 * @param options
 */
export function translateOptions(options: CommonType.Option<string, App.I18n.I18nKey>[]) {
  return options.map(option => ({
    ...option,
    label: $t(option.label)
  }));
}

/**
 * Toggle html class
 *
 * @param className
 */
export function toggleHtmlClass(className: string) {
  function add() {
    document.documentElement.classList.add(className);
  }

  function remove() {
    document.documentElement.classList.remove(className);
  }

  return {
    add,
    remove
  };
}

/**
 * 格式化后端返回的 ISO 时间字符串
 *
 * @param value 数据值
 * @returns 格式化后的展示值
 */
function formatIsoDateTimeString(value: string) {
  if (!ISO_DATE_TIME_PATTERN.test(value)) {
    return value;
  }

  const parsed = dayjs(value);

  if (!parsed.isValid()) {
    return value;
  }

  return parsed.format('YYYY-MM-DD HH:mm:ss');
}

/**
 * 递归格式化响应中的时间字段
 *
 * @param payload 接口响应数据
 * @returns 格式化后的数据
 */
export function formatServiceDateTimeFields<T>(payload: T): T {
  if (Array.isArray(payload)) {
    return payload.map(item => formatServiceDateTimeFields(item)) as T;
  }

  if (!payload || typeof payload !== 'object') {
    return payload;
  }

  const record = payload as Record<string, unknown>;

  Object.keys(record).forEach(key => {
    const value = record[key];

    if (typeof value === 'string' && DATE_FIELD_PATTERN.test(key)) {
      record[key] = formatIsoDateTimeString(value);
      return;
    }

    if (value && typeof value === 'object') {
      record[key] = formatServiceDateTimeFields(value);
    }
  });

  return payload;
}
