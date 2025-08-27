import * as functions from "firebase-functions";
import {loadBlob, FIELD_INDEX} from "../utils/blob"; // your blob helpers

// Constants
const DECAY_DURATION_MS = 5 * 24 * 60 * 60 * 1000; // 5 days

interface PreferenceWeights {
  age: number;
  budget: number;
  commute: number;
  bedtime: number;
  alcohol: number;
  groupSize: number;
  lastSeen: number;
  profilePicture: number;
}


interface MyRequest {
  groupId: string;
  lastSeenTimestamps: Record<string, number>;
  weights: PreferenceWeights;
  n?: number;
}

export const findMatchesForGroup = functions.https.onCall(
  async (request: functions.https.CallableRequest<MyRequest>) => {
    const {groupId, lastSeenTimestamps, weights, n = 10} = request.data;

    if (!groupId || !weights) {
      throw new functions
        .https
        .HttpsError("invalid-argument", "groupId and weights are required");
    }

    const blobMap = await loadBlob();

    for (const [id, stats] of blobMap.entries()) {
      functions.logger.debug("Group stats", {
        id,
        size: stats[FIELD_INDEX.size],
        avgAge: stats[FIELD_INDEX.avgAge],
        minAge: stats[FIELD_INDEX.minAge],
        maxAge: stats[FIELD_INDEX.maxAge],
        ageStdDev: stats[FIELD_INDEX.ageStdDev],
        avgBudget: stats[FIELD_INDEX.avgBudget],
        minBudget: stats[FIELD_INDEX.minBudget],
        maxBudget: stats[FIELD_INDEX.maxBudget],
        budgetStdDev: stats[FIELD_INDEX.budgetStdDev],
        avgCommute: stats[FIELD_INDEX.avgCommute],
        minCommute: stats[FIELD_INDEX.minCommute],
        maxCommute: stats[FIELD_INDEX.maxCommute],
        commuteStdDev: stats[FIELD_INDEX.commuteStdDev],
        avgBedtime: stats[FIELD_INDEX.avgBedtime],
        minBedtime: stats[FIELD_INDEX.minBedtime],
        maxBedtime: stats[FIELD_INDEX.maxBedtime],
        bedtimeStdDev: stats[FIELD_INDEX.bedtimeStdDev],
        avgAlcohol: stats[FIELD_INDEX.avgAlcohol],
        minAlcohol: stats[FIELD_INDEX.minAlcohol],
        maxAlcohol: stats[FIELD_INDEX.maxAlcohol],
        alcoholStdDev: stats[FIELD_INDEX.alcoholStdDev],
        groupMin: stats[FIELD_INDEX.groupMin],
        groupMax: stats[FIELD_INDEX.groupMax],
        profilePictureRatio: stats[FIELD_INDEX.profilePictureRatio],
      });
    }

    const currentStats = blobMap.get(groupId);
    if (!currentStats) {
      throw new functions
        .https
        .HttpsError("not-found", "Group stats not found");
    }

    const now = Date.now();

    // --- Filtering & Scoring ---
    const results: { id: string; score: number }[] = [];

    for (const [otherId, otherStats] of blobMap.entries()) {
      if (otherId === groupId) continue; // skip self

      // Hard filters
      if (
        weights.commute === 5 &&
        otherStats[FIELD_INDEX.maxCommute] > currentStats[FIELD_INDEX.maxCommute]
      ) {
        continue;
      }
      if (
        weights.budget === 5 &&
        otherStats[FIELD_INDEX.maxBudget] > currentStats[FIELD_INDEX.maxBudget]
      ) {
        continue;
      }
      if (
        weights.profilePicture === 5 &&
        otherStats[FIELD_INDEX.profilePictureRatio] < 1.0
      ) {
        continue;
      }
      if (weights.age === 5 &&
        otherStats[FIELD_INDEX.maxAge] > currentStats[FIELD_INDEX.maxAge]
      ) {
        continue;
      }
      if (weights.age === 5 &&
        otherStats[FIELD_INDEX.minAge] < currentStats[FIELD_INDEX.minAge]
      ) {
        continue;
      }
      if (weights.bedtime === 5 &&
        otherStats[FIELD_INDEX.maxBedtime] > currentStats[FIELD_INDEX.maxBedtime]
      ) {
        continue;
      }
      if (weights.alcohol === 5 &&
        otherStats[FIELD_INDEX.maxAlcohol] > currentStats[FIELD_INDEX.maxAlcohol]
      ) {
        continue;
      }
      if (weights.alcohol === 5 &&
        otherStats[FIELD_INDEX.minAlcohol] < currentStats[FIELD_INDEX.minAlcohol]
      ) {
        continue;
      }


      // Group size compatibility
      const currentSize = currentStats[FIELD_INDEX.size];
      const otherSize = otherStats[FIELD_INDEX.size];
      const combinedSize = currentSize + otherSize;

      const currentMax = currentStats[FIELD_INDEX.groupMax];
      const otherMax = otherStats[FIELD_INDEX.groupMax];
      const allowedMax = Math.min(currentMax, otherMax);

      if (combinedSize > allowedMax) {
        continue;
      }

      // --- Extract stats ---
      const myAvgAge = currentStats[FIELD_INDEX.avgAge];
      const myMinAge = currentStats[FIELD_INDEX.minAge];
      const myMaxAge = currentStats[FIELD_INDEX.maxAge];
      const otherAvgAge = otherStats[FIELD_INDEX.avgAge];
      const otherMinAge = otherStats[FIELD_INDEX.minAge];
      const otherMaxAge = otherStats[FIELD_INDEX.maxAge];

      const myAvgBudget = currentStats[FIELD_INDEX.avgBudget];
      const otherAvgBudget = otherStats[FIELD_INDEX.avgBudget];
      const myBudgetStdDev = currentStats[FIELD_INDEX.budgetStdDev];
      const otherBudgetStdDev = otherStats[FIELD_INDEX.budgetStdDev];

      const myAvgCommute = currentStats[FIELD_INDEX.avgCommute];
      const otherAvgCommute = otherStats[FIELD_INDEX.avgCommute];
      const myCommuteStdDev = currentStats[FIELD_INDEX.commuteStdDev];
      const otherCommuteStdDev = otherStats[FIELD_INDEX.commuteStdDev];

      const myAvgBedtime = currentStats[FIELD_INDEX.avgBedtime];
      const otherAvgBedtime = otherStats[FIELD_INDEX.avgBedtime];
      const myBedtimeStdDev = currentStats[FIELD_INDEX.bedtimeStdDev];
      const otherBedtimeStdDev = otherStats[FIELD_INDEX.bedtimeStdDev];

      const myAvgAlcohol = currentStats[FIELD_INDEX.avgAlcohol];
      const otherAvgAlcohol = otherStats[FIELD_INDEX.avgAlcohol];
      const myAlcoholStdDev = currentStats[FIELD_INDEX.alcoholStdDev];
      const otherAlcoholStdDev = otherStats[FIELD_INDEX.alcoholStdDev];

      const myGroupMin = currentStats[FIELD_INDEX.groupMin];
      const otherGroupMin = otherStats[FIELD_INDEX.groupMin];

      // --- Compute similarity score ---
      let score = 0.0;
      let totalWeight = 0.0;

      const overlap = Math.min(myMaxAge, otherMaxAge) - Math.max(myMinAge, otherMinAge);
      const ageScore = overlap > 0 ?
        1.0 :
        1.0 / (1 + Math.abs(myAvgAge - otherAvgAge));

      const diffBudget = Math.abs(myAvgBudget - otherAvgBudget);
      const budgetScore = 1.0 / (1 + diffBudget / (myBudgetStdDev + otherBudgetStdDev + 1));

      const diffCommute = Math.abs(myAvgCommute - otherAvgCommute);
      const commuteScore = 1.0 / (1 + diffCommute / (myCommuteStdDev + otherCommuteStdDev + 1));

      const diffBedtime = Math.abs(myAvgBedtime - otherAvgBedtime);
      const bedtimeScore = 1.0 / (1 + diffBedtime / (myBedtimeStdDev + otherBedtimeStdDev + 1));

      const diffAlcohol = Math.abs(myAvgAlcohol - otherAvgAlcohol);
      const alcoholScore = 1.0 / (1 + diffAlcohol / (myAlcoholStdDev + otherAlcoholStdDev + 1));

      const overlapSize = Math.min(currentMax, otherMax) - Math.max(myGroupMin, otherGroupMin);
      const sizeScore = overlapSize > 0 ? 1.0 : 0.5;

      /**
       * Applies a weighted score component to the total score.
       * @param {number} weight - weight from 1 (ignore) to 5 (must match)
       * @param {number} value - score component to apply
       * @return {void}
       */
      const apply = (weight: number, value: number): void => {
        if (weight <= 1) return;
        score += value * weight;
        totalWeight += weight;
      };


      // then:
      apply(weights.age, ageScore);
      apply(weights.budget, budgetScore);
      apply(weights.commute, commuteScore);
      apply(weights.bedtime, bedtimeScore);
      apply(weights.alcohol, alcoholScore);
      apply(weights.groupSize, sizeScore);

      let baseScore = totalWeight > 0 ? score / totalWeight : 0;

      // --- Last seen penalty ---
      const lastSeen = lastSeenTimestamps[otherId];
      if (lastSeen) {
        const elapsed = now - lastSeen;
        const decayFactor = 1.0 - (elapsed / DECAY_DURATION_MS);
        const penalty = weights.lastSeen * Math.max(0, Math.min(1, decayFactor));
        baseScore -= penalty;
      }

      results.push({id: otherId, score: baseScore});
    }

    // Sort & return top-N
    results.sort((a, b) => b.score - a.score);
    return results.slice(0, n);
  });
