export function getCookie(name: string): string | undefined {
  if (typeof document === 'undefined') return undefined;
  const row = document.cookie.split('; ').find(r => r.startsWith(`${name}=`));
  if (!row) return undefined;
  return decodeURIComponent(row.slice(name.length + 1));
}
