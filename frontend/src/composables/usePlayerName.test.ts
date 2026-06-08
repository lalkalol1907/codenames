import { afterEach, describe, expect, it } from 'vitest';
import { loadPlayerName, savePlayerName } from './usePlayerName';

describe('usePlayerName', () => {
  afterEach(() => {
    localStorage.removeItem('codenames_player_name');
  });

  it('returns empty when nothing saved', () => {
    expect(loadPlayerName()).toBe('');
  });

  it('saves and loads nickname', () => {
    savePlayerName('Alice');
    expect(loadPlayerName()).toBe('Alice');
  });

  it('trims and limits length', () => {
    savePlayerName(`  ${'x'.repeat(80)}  `);
    expect(loadPlayerName()).toHaveLength(64);
  });
});
