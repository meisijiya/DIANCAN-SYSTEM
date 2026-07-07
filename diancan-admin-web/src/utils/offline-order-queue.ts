export interface OfflineAdminOrderRecord {
  id?: number;
  clientOrderNo: string;
  payload: Api.Business.AdminOrderCreate;
  createdAt: number;
  retryCount: number;
  lastError?: string;
}

const DB_NAME = 'henfon_service_pos';
const DB_VERSION = 1;
const STORE_NAME = 'offline_admin_orders';
const INDEX_CLIENT_ORDER_NO = 'clientOrderNo';

let dbPromise: Promise<IDBDatabase> | null = null;

function getDB() {
  if (dbPromise) return dbPromise;

  dbPromise = new Promise((resolve, reject) => {
    const request = window.indexedDB.open(DB_NAME, DB_VERSION);

    request.onupgradeneeded = () => {
      const db = request.result;
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        const store = db.createObjectStore(STORE_NAME, { keyPath: 'id', autoIncrement: true });
        store.createIndex(INDEX_CLIENT_ORDER_NO, INDEX_CLIENT_ORDER_NO, { unique: true });
      }
    };

    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });

  return dbPromise;
}

function runRequest<T = unknown>(request: IDBRequest<T>): Promise<T> {
  return new Promise((resolve, reject) => {
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

export async function enqueueOfflineAdminOrder(
  payload: Api.Business.AdminOrderCreate,
  clientOrderNo: string
): Promise<void> {
  const db = await getDB();
  const tx = db.transaction(STORE_NAME, 'readwrite');
  const store = tx.objectStore(STORE_NAME);
  const index = store.index(INDEX_CLIENT_ORDER_NO);

  const existing = await runRequest(index.get(clientOrderNo));
  if (existing) return;

  const record: OfflineAdminOrderRecord = {
    clientOrderNo,
    payload,
    createdAt: Date.now(),
    retryCount: 0
  };
  await runRequest(store.add(record));
}

export async function listOfflineAdminOrders(): Promise<OfflineAdminOrderRecord[]> {
  const db = await getDB();
  const tx = db.transaction(STORE_NAME, 'readonly');
  const store = tx.objectStore(STORE_NAME);
  const list = (await runRequest(store.getAll())) as OfflineAdminOrderRecord[];
  return list.sort((a, b) => a.createdAt - b.createdAt);
}

export async function removeOfflineAdminOrder(id: number): Promise<void> {
  const db = await getDB();
  const tx = db.transaction(STORE_NAME, 'readwrite');
  const store = tx.objectStore(STORE_NAME);
  await runRequest(store.delete(id));
}

export async function markOfflineAdminOrderRetry(id: number, errorMsg: string): Promise<void> {
  const db = await getDB();
  const tx = db.transaction(STORE_NAME, 'readwrite');
  const store = tx.objectStore(STORE_NAME);
  const record = (await runRequest(store.get(id))) as OfflineAdminOrderRecord | undefined;
  if (!record) return;
  record.retryCount += 1;
  record.lastError = errorMsg;
  await runRequest(store.put(record));
}

export async function countOfflineAdminOrders(): Promise<number> {
  const db = await getDB();
  const tx = db.transaction(STORE_NAME, 'readonly');
  const store = tx.objectStore(STORE_NAME);
  return runRequest(store.count());
}

