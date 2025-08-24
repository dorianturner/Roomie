export interface StudentProfile {
  id: string;
  name: string;
  bio: string;
  profileType: string; // "student"
  studentUniversity: string;
  studentBasicPreferences: string[];
  studentDesiredGroupSize: [number, number];
  studentMaxCommute: number;
  studentMaxBudget: number;
  studentAge: number;
  seenGroupsTimestamps: Record<string, number>;
}