import { StudentProfile } from './student.js';

export interface GroupStats {
  size: number;
  avgBudget: number;
  avgCommute: number;
  avgAge: number;
}

export interface GroupProfile {
  id: string;
  name: string;
  members: StudentProfile[];
  stats: GroupStats;
}