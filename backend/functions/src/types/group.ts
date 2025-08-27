import { StudentProfile } from './student.js';

export interface GroupStats {
  size: number;

  // --- Age ---
  avgAge: number | null;
  minAge: number | null;
  maxAge: number | null;
  ageStdDev: number | null;

  // --- Budget ---
  avgBudget: number | null;
  minBudget: number | null;
  maxBudget: number | null;
  budgetStdDev: number | null;

  // --- Commute ---
  avgCommute: number | null;
  minCommute: number | null;
  maxCommute: number | null;
  commuteStdDev: number | null;

  // --- Bedtime (ordinal: 1–5) ---
  avgBedtime: number | null;
  minBedtime: number | null;
  maxBedtime: number | null;
  bedtimeStdDev: number | null;

  // --- Alcohol (ordinal: 1–5) ---
  avgAlcohol: number | null;
  minAlcohol: number | null;
  maxAlcohol: number | null;
  alcoholStdDev: number | null;

  // --- Lifestyle --- not used in matching yet
  commonSmokingStatus: string | null;
  commonPets: string | null;

  // --- Group size preferences (constraints) ---
  groupMin: number | null; // max of member mins
  groupMax: number | null; // min of member maxes

  // --- Personality / soft factors --- not used in matching yet
  topPassions: string[];
  topPetPeeves: string[];

  // --- Meta ---
  universities: string[]; // not actually used in matching
  profilePictureRatio: number; // 0.0–1.0
}

export interface GroupProfile {
  id: string;
  name: string;
  members: StudentProfile[];
  stats: GroupStats;
}