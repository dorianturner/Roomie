export interface StudentProfile {
  id: string;
  name: string;
  photos: string[]
  age: number | null;
  profilePictureUrl: string | null;
  pets: string | null;
  bedtime: number | null; // 1-5 ordinal, 1 = <10pm, 5 = >1am
  alcohol: string | null;
  smokingStatus: string | null;
  groupMin: number | null;
  groupMax: number | null;
  maxCommute: number | null;
  maxBudget: number | null;
  university: string | null;
  bio: string | null;
  addicted: string | null;
  petPeeve: string | null;
  passionate: string | null;
  idealNight: string | null;
  listening: string | null;
  phoneNumber: string | null;
  seenUsersTimestamps: Map<string, number> | null;
}
