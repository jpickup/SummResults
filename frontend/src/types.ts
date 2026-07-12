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
 * This is the type returned by GET /api/results?eventId={id}.
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
 * A named event as returned by GET /api/events.
 * Only the public fields (id and name) are exposed by the backend.
 */
export interface NamedEvent {
  id: string;
  name: string;
}

/**
 * A team of one or two competitors, as returned by GET /api/teams.
 * Fields for member2 are absent for solo entries.
 */
export interface Team {
  id: string;
  teamName: string;
  member1: string;
  member1Age: number;
  member1Gender: 'M' | 'F';
  member2?: string;
  member2Age?: number;
  member2Gender?: 'M' | 'F';
}

/**
 * Request body for POST /api/teams and PUT /api/teams/{id}.
 * member2 fields are required when member2 is provided.
 */
export interface TeamRequest {
  teamName: string;
  member1: string;
  member1Age: number;
  member1Gender: 'M' | 'F';
  member2?: string;
  member2Age?: number;
  member2Gender?: 'M' | 'F';
}

/**
 * Aggregated result for a team as returned by GET /api/results.
 * day1NetScore and day2NetScore are each the best (max) net score
 * among the team's members for that day.
 * handicapPct is 0 for teams that do not qualify.
 * handicapScore = round(totalScore * (1 + handicapPct / 100)).
 */
export interface TeamResult {
  teamName: string;
  members: string[];
  day1NetScore: number;
  day2NetScore: number;
  totalScore: number;
  handicapPct: number;
  handicapScore: number;
}

/**
 * Runtime configuration injected via window.__APP_CONFIG__ in index.html.
 * Allows the same built SPA artefact to be pointed at different backend instances.
 */
export interface AppConfig {
  apiBaseUrl: string;
}
