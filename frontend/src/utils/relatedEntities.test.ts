import { extractIncidentIds, parseIncidentId } from './relatedEntities';

describe('relatedEntities', () => {
  describe('parseIncidentId', () => {
    it('parses INCIDENT ref', () => {
      expect(parseIncidentId('INCIDENT:42')).toBe(42);
      expect(parseIncidentId('  INCIDENT:7  ')).toBe(7);
    });

    it('returns null for invalid ref', () => {
      expect(parseIncidentId('TASK:1')).toBeNull();
      expect(parseIncidentId('')).toBeNull();
    });
  });

  describe('extractIncidentIds', () => {
    it('extracts unique incident ids', () => {
      expect(
        extractIncidentIds(['INCIDENT:1', 'INCIDENT:2', 'INCIDENT:1', 'OTHER']),
      ).toEqual([1, 2]);
    });
  });
});
