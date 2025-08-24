import { Storage } from "@google-cloud/storage";

const storage = new Storage();
const BUCKET_NAME = "roomie-9f45c.firebasestorage.app";
const BLOB_FILE = "groupStats.blob";
export const FIELDS_PER_GROUP = 4; // size, avgBudget, avgCommute, avgAge

// Load blob from Cloud Storage
export async function loadBlob(): Promise<Map<string, Uint16Array>> {
  const map = new Map<string, Uint16Array>();
  const file = storage.bucket(BUCKET_NAME).file(BLOB_FILE);

  const [exists] = await file.exists();
  if (!exists) return map;

  const [data] = await file.download();
  let offset = 0;

  while (offset < data.length) {
    const idLen = data.readUInt16BE(offset);
    offset += 2;
    const id = data.subarray(offset, offset + idLen).toString("utf-8");
    offset += idLen;

    const stats = new Uint16Array(FIELDS_PER_GROUP);
    for (let i = 0; i < FIELDS_PER_GROUP; i++) {
      stats[i] = data.readUInt16BE(offset);
      offset += 2;
    }

    map.set(id, stats);
  }

  return map;
}

// Save map to blob in Cloud Storage
export async function saveBlob(map: Map<string, Uint16Array>) {
  const buffers: Buffer[] = [];

  for (const [id, stats] of map.entries()) {
    const idBuf = Buffer.from(id, "utf-8");
    const buf = Buffer.alloc(2 + idBuf.length + stats.length * 2);
    let offset = 0;

    buf.writeUInt16BE(idBuf.length, offset);
    offset += 2;

    idBuf.copy(buf, offset);
    offset += idBuf.length;

    for (let i = 0; i < stats.length; i++) {
      buf.writeUInt16BE(stats[i], offset);
      offset += 2;
    }

    buffers.push(buf);
  }

  const finalBuffer = Buffer.concat(buffers);
  await storage.bucket(BUCKET_NAME).file(BLOB_FILE).save(finalBuffer);
}
