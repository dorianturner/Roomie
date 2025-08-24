import * as admin from "firebase-admin";

const db = admin.firestore();
const LOCK_DOC = db.collection("locks").doc("groupBlob");

// Simple wait helper
async function wait(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

// Acquire lock with retry
export async function acquireLock(timeoutMs = 5000, intervalMs = 100): Promise<void> {
  const start = Date.now();

  while (Date.now() - start < timeoutMs) {
    try {
      await db.runTransaction(async (t) => {
        const doc = await t.get(LOCK_DOC);
        if (!doc.exists || doc.data()?.blobBeingWritten === false) {
          t.set(LOCK_DOC, { blobBeingWritten: true }, { merge: true });
        } else {
          throw new Error("Locked");
        }
      });
      return; // success
    } catch {
      await wait(intervalMs);
    }
  }
  throw new Error("Could not acquire lock in time");
}

// Release lock
export async function releaseLock() {
  await LOCK_DOC.set({ blobBeingWritten: false }, { merge: true });
}

export async function tryAcquireLock(
  maxAttempts = 5,
  backoffMs = 200
): Promise<void> {
  for (let i = 0; i < maxAttempts; i++) {
    try {
      await acquireLock();
      return; // success
    } catch (e) {
      if (i === maxAttempts - 1) throw e;
      await new Promise((res) => setTimeout(res, backoffMs * (i + 1))); // exponential backoff
    }
  }
}

