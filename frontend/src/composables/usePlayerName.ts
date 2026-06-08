const STORAGE_KEY = 'codenames_player_name';

export function loadPlayerName(): string {
  if (typeof localStorage === 'undefined') return '';
  return localStorage.getItem(STORAGE_KEY)?.trim() ?? '';
}

export function savePlayerName(name: string): void {
  if (typeof localStorage === 'undefined') return;
  const trimmed = name.trim().slice(0, 64);
  if (trimmed) {
    localStorage.setItem(STORAGE_KEY, trimmed);
  } else {
    localStorage.removeItem(STORAGE_KEY);
  }
}
