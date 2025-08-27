import {onCall} from "firebase-functions/v2/https";
import {loadBlob, saveBlob} from "../utils/blob";
import {releaseLock, tryAcquireLock} from "../utils/lock";

export const deleteGroupFromBlob = onCall(async (req) => {
  try {
    const {groupId} = req.data;
    if (!groupId) {
      throw new Error("Missing groupId");
    }

    await tryAcquireLock();

    try {
      const map = await loadBlob();

      if (map.has(groupId)) {
        map.delete(groupId);
        await saveBlob(map);
        return {
          success: true, message: `Group ${groupId} deleted successfully`,
        };
      } else {
        return {success: false, message: `Group ${groupId} not found`};
      }
    } finally {
      await releaseLock();
    }
  } catch (err: any) {
    console.error(err);
    throw new Error(err.message || "Internal server error");
  }
});
