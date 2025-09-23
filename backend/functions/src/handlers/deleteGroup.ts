import {onCall} from "firebase-functions/v2/https";
import {loadBlob, saveBlob} from "../utils/blob";
import {releaseLock, tryAcquireLock} from "../utils/lock";

/**
 * Deletes a group from a blob based on the provided groupId.
 * @param {Object} req - The request object containing the data.
 * @returns {Object} An object with success status and message.
 * @throws {Error} If groupId is missing or if the group is not found.
 */
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
