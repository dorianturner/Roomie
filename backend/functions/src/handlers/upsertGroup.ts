import {onCall} from "firebase-functions/v2/https";
import {GroupProfile} from "../types/group";
import {loadBlob, saveBlob, FIELDS_PER_GROUP, FIELD_INDEX} from "../utils/blob";
import {releaseLock, tryAcquireLock} from "../utils/lock";

export const upsertGroupProfileWithLock = onCall(async (req) => {
  try {
    const group = req.data as GroupProfile;

    if (!group?.id || !group.stats) {
      throw new Error("Invalid payload");
    }

    await tryAcquireLock();

    try {
      const map = await loadBlob();

      // use Float64Array for direct double storage
      const statsArray = new Float64Array(FIELDS_PER_GROUP);

      const toNum = (val: number | null | undefined) => val ?? NaN;

      statsArray[FIELD_INDEX.size] = toNum(group.stats.size);

      statsArray[FIELD_INDEX.avgAge] = toNum(group.stats.avgAge);
      statsArray[FIELD_INDEX.minAge] = toNum(group.stats.minAge);
      statsArray[FIELD_INDEX.maxAge] = toNum(group.stats.maxAge);
      statsArray[FIELD_INDEX.ageStdDev] = toNum(group.stats.ageStdDev);

      statsArray[FIELD_INDEX.avgBudget] = toNum(group.stats.avgBudget);
      statsArray[FIELD_INDEX.minBudget] = toNum(group.stats.minBudget);
      statsArray[FIELD_INDEX.maxBudget] = toNum(group.stats.maxBudget);
      statsArray[FIELD_INDEX.budgetStdDev] = toNum(group.stats.budgetStdDev);

      statsArray[FIELD_INDEX.avgCommute] = toNum(group.stats.avgCommute);
      statsArray[FIELD_INDEX.minCommute] = toNum(group.stats.minCommute);
      statsArray[FIELD_INDEX.maxCommute] = toNum(group.stats.maxCommute);
      statsArray[FIELD_INDEX.commuteStdDev] = toNum(group.stats.commuteStdDev);

      statsArray[FIELD_INDEX.avgBedtime] = toNum(group.stats.avgBedtime);
      statsArray[FIELD_INDEX.minBedtime] = toNum(group.stats.minBedtime);
      statsArray[FIELD_INDEX.maxBedtime] = toNum(group.stats.maxBedtime);
      statsArray[FIELD_INDEX.bedtimeStdDev] = toNum(group.stats.bedtimeStdDev);

      statsArray[FIELD_INDEX.avgAlcohol] = toNum(group.stats.avgAlcohol);
      statsArray[FIELD_INDEX.minAlcohol] = toNum(group.stats.minAlcohol);
      statsArray[FIELD_INDEX.maxAlcohol] = toNum(group.stats.maxAlcohol);
      statsArray[FIELD_INDEX.alcoholStdDev] = toNum(group.stats.alcoholStdDev);

      statsArray[FIELD_INDEX.groupMin] = toNum(group.stats.groupMin);
      statsArray[FIELD_INDEX.groupMax] = toNum(group.stats.groupMax);

      statsArray[FIELD_INDEX.profilePictureRatio] = toNum(group.stats.profilePictureRatio);
      statsArray[FIELD_INDEX.status] = toNum(group.stats.status);
      map.set(group.id, statsArray);

      await saveBlob(map);
    } finally {
      await releaseLock();
    }

    return {success: true, message: "Group stats updated successfully"};
  } catch (err: any) {
    console.error(err);
    throw new Error(err.message || "Internal server error");
  }
});
