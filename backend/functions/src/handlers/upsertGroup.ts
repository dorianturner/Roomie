import { onCall } from "firebase-functions/v2/https";
import { GroupProfile } from "../types/group";
import { loadBlob, saveBlob, FIELDS_PER_GROUP } from "../utils/blob";
import { releaseLock, tryAcquireLock } from "../utils/lock";

export const upsertGroupProfileWithLock = onCall(async (req) => {
  try {
    const group = req.data as GroupProfile;

    if (!group?.id || !group.stats) {
      throw new Error("Invalid payload");
    }

    await tryAcquireLock();

    try {
      const map = await loadBlob();

      const statsArray = new Uint16Array(FIELDS_PER_GROUP);
      statsArray[0] = group.stats.size;
      statsArray[1] = group.stats.avgBudget;
      statsArray[2] = group.stats.avgCommute;
      statsArray[3] = group.stats.avgAge;

      map.set(group.id, statsArray);

      await saveBlob(map);
    } finally {
      await releaseLock();
    }

    return { success: true, message: "Group stats updated successfully" };
  } catch (err: any) {
    console.error(err);
    throw new Error(err.message || "Internal server error");
  }
});
