import { getApiBaseUrl } from '../../auth/authConfig';

const escapeHtml = (value: string): string =>
  value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');

const normalizeAssetUrl = (url: string): string => {
  const trimmed = url.trim();
  if (!trimmed) return trimmed;
  if (/^https?:\/\//i.test(trimmed) || trimmed.startsWith('data:')) {
    return trimmed;
  }
  if (trimmed.startsWith('/')) {
    return `${getApiBaseUrl()}${trimmed}`;
  }
  return trimmed;
};

const parseImageAttributes = (rawAttributes?: string) => {
  const result: {
    readonly align?: 'left' | 'center' | 'right';
    readonly width?: string;
    readonly height?: string;
  } = {};
  if (!rawAttributes) return result;
  const tokens = rawAttributes
    .replace(/^\{|\}$/g, '')
    .split(/\s+/)
    .map(token => token.trim())
    .filter(Boolean);
  for (const token of tokens) {
    const [key, value] = token.split('=');
    if (!key || !value) continue;
    if (key === 'align' && (value === 'left' || value === 'center' || value === 'right')) {
      (result as { align?: 'left' | 'center' | 'right' }).align = value;
      continue;
    }
    if (key === 'width') {
      (result as { width?: string }).width = value;
      continue;
    }
    if (key === 'height') {
      (result as { height?: string }).height = value;
    }
  }
  return result;
};

const renderImageTag = (alt: string, url: string, rawAttributes?: string): string => {
  const attrs = parseImageAttributes(rawAttributes);
  const classes = ['idol-glow-image'];
  if (attrs.align) {
    classes.push(`idol-glow-image--${attrs.align}`);
  }
  const styleParts: string[] = [];
  if (attrs.width) {
    styleParts.push(`width:${attrs.width}`);
  }
  if (attrs.height) {
    styleParts.push(`height:${attrs.height}`);
    styleParts.push('object-fit:contain');
  }
  const style = styleParts.length > 0 ? ` style="${styleParts.join(';')}"` : '';
  return `<img src="${normalizeAssetUrl(url)}" alt="${alt}" class="${classes.join(' ')}"${style} />`;
};

const inlineHeadingStyleByLevel: Record<string, string> = {
  h1: 'font-size:1.75em;font-weight:800;line-height:1.2',
  h2: 'font-size:1.5em;font-weight:800;line-height:1.25',
  h3: 'font-size:1.25em;font-weight:800;line-height:1.3',
  h4: 'font-size:1.1em;font-weight:800;line-height:1.35',
};

const renderInlineHeading = (level: string, content: string): string => {
  const normalizedLevel = level.toLowerCase();
  const style = inlineHeadingStyleByLevel[normalizedLevel] ?? inlineHeadingStyleByLevel.h3;
  return `<span class="idol-glow-inline-heading idol-glow-inline-heading--${normalizedLevel}" style="${style}">${content}</span>`;
};

const parseInline = (value: string): string => {
  const escaped = escapeHtml(value);
  return escaped
    .replace(
      /!\[([^\]]*)\]\(([^)]+)\)(\{[^}]+\})?/g,
      (_, alt, url, rawAttributes) => renderImageTag(alt, url, rawAttributes)
    )
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.+?)\*/g, '<em>$1</em>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(
      /\[(.+?)\]\((.+?)\)\{target=(_blank|_self)\}/g,
      (_, text, url, target) =>
        `<a href="${normalizeAssetUrl(url)}" target="${target}" rel="noreferrer">${text}</a>`
    )
    .replace(/\[(.+?)\]\((.+?)\)/g, (_, text, url) => `<a href="${normalizeAssetUrl(url)}" target="_blank" rel="noreferrer">${text}</a>`)
    .replace(/\[\[(h[1-4]):(.+?)\]\]/gi, (_, level, content) => renderInlineHeading(level, content));
};

export function renderIdolGlowMarkdown(markdown: string): string {
  const lines = markdown.replace(/\r\n/g, '\n').split('\n');
  const html: string[] = [];
  let index = 0;
  let inList = false;

  const closeList = () => {
    if (!inList) return;
    html.push('</ul>');
    inList = false;
  };

  while (index < lines.length) {
    const trimmed = lines[index].trim();

    if (!trimmed) {
      closeList();
      index += 1;
      continue;
    }

    // Preserve trusted inline HTML blocks from legacy event content.
    if (/^<[^>]+>/.test(trimmed)) {
      closeList();
      html.push(trimmed);
      index += 1;
      continue;
    }

    const headerMatch = trimmed.match(/^(#{1,6})\s+(.+)$/);
    if (headerMatch) {
      closeList();
      const level = headerMatch[1].length;
      html.push(`<h${level}>${parseInline(headerMatch[2])}</h${level}>`);
      index += 1;
      continue;
    }

    const nextLine = lines[index + 1]?.trim() ?? '';
    if (trimmed.includes('|') && /^\|?[\s:-]+\|[\s|:-]*$/.test(nextLine)) {
      closeList();
      const headerCells = trimmed
        .replace(/^\||\|$/g, '')
        .split('|')
        .map((cell: string) => cell.trim());
      html.push('<table><thead><tr>');
      headerCells.forEach(cell => html.push(`<th>${parseInline(cell)}</th>`));
      html.push('</tr></thead><tbody>');
      index += 2;
      while (index < lines.length && lines[index].includes('|')) {
        const rowCells = lines[index]
          .trim()
          .replace(/^\||\|$/g, '')
          .split('|')
          .map((cell: string) => cell.trim());
        html.push('<tr>');
        rowCells.forEach(cell => html.push(`<td>${parseInline(cell)}</td>`));
        html.push('</tr>');
        index += 1;
      }
      html.push('</tbody></table>');
      continue;
    }

    const listMatch = trimmed.match(/^[-*]\s+(.+)$/);
    if (listMatch) {
      if (!inList) {
        html.push('<ul>');
        inList = true;
      }
      html.push(`<li>${parseInline(listMatch[1])}</li>`);
      index += 1;
      continue;
    }

    closeList();
    html.push(`<p>${parseInline(trimmed)}</p>`);
    index += 1;
  }

  closeList();
  return html.join('');
}
