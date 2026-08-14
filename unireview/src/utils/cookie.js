const COOKIE_NAME = 'reviewer_token';
const BACKUP_KEY = 'reviewer_token_backup';
const MAX_AGE = 31536000; // 1 year in seconds

export function getReviewerToken() {
  // Try cookie first
  let token = getCookie(COOKIE_NAME);

  // Fallback to localStorage backup
  if (!token) {
    token = localStorage.getItem(BACKUP_KEY);
    if (token) {
      // Restore cookie from backup
      setCookie(COOKIE_NAME, token, MAX_AGE);
    }
  }

  return token;
}

export function setReviewerToken(token) {
  setCookie(COOKIE_NAME, token, MAX_AGE);
  localStorage.setItem(BACKUP_KEY, token);
}

export function clearReviewerToken() {
  setCookie(COOKIE_NAME, '', -1);
  localStorage.removeItem(BACKUP_KEY);
}

function getCookie(name) {
  const match = document.cookie.match(new RegExp('(^| )' + name + '=([^;]+)'));
  return match ? decodeURIComponent(match[2]) : null;
}

function setCookie(name, value, maxAge) {
  const secure = window.location.protocol === 'https:' ? '; Secure' : '';
  document.cookie = `${name}=${encodeURIComponent(value)}; Max-Age=${maxAge}; Path=/; SameSite=Lax${secure}`;
}
