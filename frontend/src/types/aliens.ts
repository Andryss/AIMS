import type { Page } from './common';

export interface Alien {
  photoFileIds?: number[];
  id: number;
  name: string;
  description: string;
  threatLevel: number;
}

export interface AlienSearchResponse {
  items: Alien[];
}

export interface SaveAlienRequest {
  photoFileIds?: number[];
  name: string;
  description: string;
  threatLevel: number;
}

export type AlienListResponse = Page<Alien>;
