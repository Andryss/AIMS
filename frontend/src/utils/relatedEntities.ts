const INCIDENT_REF_PATTERN = /^INCIDENT:(\d+)$/;

export function parseIncidentId(ref: string): number | null {
  const match = INCIDENT_REF_PATTERN.exec(ref.trim());
  if (!match) {
    return null;
  }
  return Number(match[1]);
}

export function extractIncidentIds(relatedEntities: string[]): number[] {
  const ids: number[] = [];
  relatedEntities.forEach((ref) => {
    const id = parseIncidentId(ref);
    if (id != null && !ids.includes(id)) {
      ids.push(id);
    }
  });
  return ids;
}
