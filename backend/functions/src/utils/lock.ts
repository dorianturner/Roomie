import * as admin from "firebase-admin";

const db = admin.firestore();
const LOCK_DOC = db.collection("locks").doc("groupBlob");

/**
 * Blocks execution for a specified duration.
 * @param {number} ms Milliseconds to wait
 * @return {void} Promise that resolves after the wait
 */
async function wait(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/**
 * Acquires a lock by setting a Firestore document field.
 * @param {number} timeoutMs Maximum time to try acquiring the lock
 * @param {number} intervalMs Time between attempts
 * @return {Promise<void>} Resolves when the lock is acquired
 */
export async function acquireLock(timeoutMs = 5000, intervalMs = 100): Promise<void> {
  const start = Date.now();

  while (Date.now() - start < timeoutMs) {
    try {
      await db.runTransaction(async (t) => {
        const doc = await t.get(LOCK_DOC);
        if (!doc.exists || doc.data()?.blobBeingWritten === false) {
          t.set(LOCK_DOC, {blobBeingWritten: true}, {merge: true});
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

/**
 * Releases the lock by updating the Firestore document field.
 * @return {void}
 */
export async function releaseLock() {
  await LOCK_DOC.set({blobBeingWritten: false}, {merge: true});
}

/**
 * Tries to acquire the lock with retries and exponential backoff.
 * @param {number} maxAttempts Maximum number of attempts
 * @param {number} backoffMs Base backoff time in milliseconds
 * @return {Promise<void>} Resolves when the lock is acquired
 */
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

