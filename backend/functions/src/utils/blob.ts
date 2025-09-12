import {Storage} from "@google-cloud/storage";

const storage = new Storage();
const BUCKET_NAME = "roomie-9f45c.firebasestorage.app";
const BLOB_FILE = "groupStatsBlob.bin";

export const FIELDS_PER_GROUP = 24;

export const FIELD_INDEX = {
  size: 0,

  // --- Age ---
  avgAge: 1,
  minAge: 2,
  maxAge: 3,
  ageStdDev: 4,

  // --- Budget ---
  avgBudget: 5,
  minBudget: 6,
  maxBudget: 7,
  budgetStdDev: 8,

  // --- Commute ---
  avgCommute: 9,
  minCommute: 10,
  maxCommute: 11,
  commuteStdDev: 12,

  // --- Bedtime ---
  avgBedtime: 13,
  minBedtime: 14,
  maxBedtime: 15,
  bedtimeStdDev: 16,

  // --- Alcohol ---
  avgAlcohol: 17,
  minAlcohol: 18,
  maxAlcohol: 19,
  alcoholStdDev: 20,

  // --- Group size prefs ---
  groupMin: 21,
  groupMax: 22,

  profilePictureRatio: 23,
};


/**
 * Loads the blob from Cloud Storage and parses it into a map.
 * @return {void} Map of group ID to Float64Array of stats
 */
export async function loadBlob(): Promise<Map<string, Float64Array>> {
  const map = new Map<string, Float64Array>();
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

    const stats = new Float64Array(FIELDS_PER_GROUP);
    for (let i = 0; i < FIELDS_PER_GROUP; i++) {
      stats[i] = data.readDoubleBE(offset);
      offset += 8; // 8 bytes per double
    }

    map.set(id, stats);
  }

  return map;
}

/**
 * Saves the map to Cloud Storage as a binary blob.
 * @param {Map<string, Float64Array>} map Map of group ID to Float64Array of stats
 */
export async function saveBlob(map: Map<string, Float64Array>) {
  const buffers: Buffer[] = [];

  for (const [id, stats] of map.entries()) {
    const idBuf = Buffer.from(id, "utf-8");

    // 2 bytes for id length + idBuf + 8 bytes per stat
    const buf = Buffer.alloc(2 + idBuf.length + stats.length * 8);

    let offset = 0;

    buf.writeUInt16BE(idBuf.length, offset);
    offset += 2;

    idBuf.copy(buf, offset);
    offset += idBuf.length;

    for (let i = 0; i < stats.length; i++) {
      buf.writeDoubleBE(stats[i], offset);
      offset += 8;
    }

    buffers.push(buf);
  }

  const finalBuffer = Buffer.concat(buffers);
  await storage.bucket(BUCKET_NAME).file(BLOB_FILE).save(finalBuffer);
}
