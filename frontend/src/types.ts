/**
 * Represents a single control visit with its scored point value.
 * Mirrors the backend ControlVisit record.
 */
export interface ControlVisit {
  controlId: string;
  points: number;
}

/**
 * Processed result for a single participant across both days.
 * Mirrors the backend ParticipantResult record.
 * This is the type returned by GET /api/results.
 */
export interface ParticipantResult {
  participantName: string;
  day1Controls: ControlVisit[];
  day1GrossScore: number;
  day1Penalty: number;
  day1NetScore: number;
  day2Controls: ControlVisit[];
  day2GrossScore: number;
  day2Penalty: number;
  day2Deduction: number;
  day2NetScore: number;
  totalScore: number;
}

/**
 * Runtime configuration injected via window.__APP_CONFIG__ in index.html.
 * Allows the same built SPA artefact to be pointed at different backend instances.
 */
export interface AppConfig {
  apiBaseUrl: string;
  day1EventId: string;
  day2EventId: string;
}
