import * as cheerio from 'cheerio';
import * as fuzzball from 'fuzzball';
import * as punycode from 'punycode';
import { ProxyAgent, fetch } from 'undici';
import axios from 'axios';
// @ts-ignore
import { wrapper } from 'axios-cookiejar-support';
import { CookieJar } from 'tough-cookie';
let pLimitModule: any;
let pRetryModule: any;

async function getPLimit() {
    if (!pLimitModule) {
        pLimitModule = await import('p-limit');
    }
    return pLimitModule.default;
}

async function getPRetry() {
    if (!pRetryModule) {
        pRetryModule = await import('p-retry');
    }
    return pRetryModule.default;
}

const jar = new CookieJar();
// @ts-ignore
const client = wrapper(axios.create({ jar, withCredentials: true }));

// ========== Прокси (создаётся один раз) ==========
let proxyAgent: ProxyAgent | undefined;
if (process.env.PROXY_HOST) {
    proxyAgent = new ProxyAgent({
        uri: `http://${process.env.PROXY_HOST}:${process.env.PROXY_PORT}`,
    });
    process.once('exit', () => proxyAgent?.close());
}

// ========== Вспомогательные функции ==========
function parseDuration(durationStr: string): number {
    const parts = durationStr.trim().split(':');
    if (parts.length === 2) {
        const minutes = parseInt(parts[0], 10);
        const seconds = parseInt(parts[1], 10);
        if (!isNaN(minutes) && !isNaN(seconds)) {
            return minutes * 60 + seconds;
        }
    }
    return 0;
}

function toSlug(input: string): string {
    return input
        .toLowerCase()
        .replace(/[^\p{L}\p{N}\s-]/gu, '')
        .replace(/\s+/g, '-')
        .replace(/-+/g, '-')
        .replace(/^-|-$/g, '');
}

const cyrillicToLatin: Record<string, string> = {
    'а': 'a', 'б': 'b', 'в': 'v', 'г': 'g', 'д': 'd', 'е': 'e', 'ё': 'yo',
    'ж': 'zh', 'з': 'z', 'и': 'i', 'й': 'y', 'к': 'k', 'л': 'l', 'м': 'm',
    'н': 'n', 'о': 'o', 'п': 'p', 'р': 'r', 'с': 's', 'т': 't', 'у': 'u',
    'ф': 'f', 'х': 'kh', 'ц': 'ts', 'ч': 'ch', 'ш': 'sh', 'щ': 'shch',
    'ъ': '', 'ы': 'y', 'ь': '', 'э': 'e', 'ю': 'yu', 'я': 'ya'
};

function toLatin(text: string): string {
    return text.split('').map(ch => cyrillicToLatin[ch] || ch).join('');
}

function normalizeSpaces(text: string): string {
    return text.replace(/\s+/g, ' ').trim();
}

function parseArtistString(artistStr: string): string[] {
    const normalized = normalizeSpaces(artistStr);
    const separators = /[,&]|\band\b|\bfeat\.?\b|\bft\.?\b|\bx\b|\bvs\.?\b/i;
    return normalized
        .split(separators)
        .map(s => s.trim())
        .filter(s => s.length > 0);
}

function authorMatchesArtists(author: string, artists: Array<{ name: string }> | string): boolean {
    const cleaned = author.replace(/[^\p{L}\p{N}\s]/gu, ' ').replace(/\s+/g, ' ').trim();
    const authorParts = cleaned.split(/\s+/).filter(p => p.length > 0);
    if (authorParts.length === 0) return false;

    const lowerAuthorParts = authorParts.map(p => p.toLowerCase());
    const lowerLatinAuthorParts = lowerAuthorParts.map(p => toLatin(p));

    let artistNames: string[] = [];
    if (typeof artists === 'string') {
        artistNames = parseArtistString(artists);
    } else if (Array.isArray(artists)) {
        artistNames = artists.map(a => a.name);
    }

    return artistNames.some(artistName => {
        const artistNameLower = artistName.toLowerCase();
        const artistNameLatinLower = toLatin(artistNameLower);

        return lowerAuthorParts.some((part, index) => {
            if (part.length < 2) return false;
            const latinPart = lowerLatinAuthorParts[index];
            return fuzzball.partial_ratio(artistNameLower, part) > 70
                || fuzzball.partial_ratio(artistNameLatinLower, latinPart) > 70;
        });
    });
}

// ========== Функция fetchTracksFromUrl с retry ==========
async function fetchTracksFromUrl(url: string): Promise<Array<{
    artist: string;
    name: string;
    url: string;
    durationSec: number
}> | null> {
    const headers = new Headers({
        'accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7',
        'accept-language': 'en-US,en;q=0.9,ru;q=0.8',
        'cache-control': 'max-age=0',
        'priority': 'u=0, i',
        'referer': 'https://xn--shramov-akstaff-izopropil92-hoprik---7s2ab04b5hb4bb8b.skysound7.com/',
        'sec-ch-ua': '"Google Chrome";v="143", "Chromium";v="143", "Not A(Brand";v="24"',
        'sec-ch-ua-mobile': '?0',
        'sec-ch-ua-platform': '"Linux"',
        'sec-fetch-dest': 'document',
        'sec-fetch-mode': 'navigate',
        'sec-fetch-site': 'same-site',
        'sec-fetch-user': '?1',
        'upgrade-insecure-requests': '1',
        'user-agent': 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36',
    });

    const fetchWithRetry = async (): Promise<Response> => {
        const pRetry = await getPRetry();
        return pRetry(
            async () => {
                const res = await fetch(url, { headers, dispatcher: proxyAgent });
                if (!res.ok) {
                    throw new Error(`HTTP error ${res.status}`);
                }
                return res;
            },
            {
                retries: 2,
                factor: 2,
                minTimeout: 1000,
                onFailedAttempt: (error: any) => {
                    console.log(`Fetch failed for ${url}, attempt ${error.attemptNumber}. ${error.message}`);
                },
            }
        );
    };

    try {
        const response = await fetchWithRetry();
        const html = await response.text();
        const $ = cheerio.load(html);
        const tracks: Array<{ artist: string; name: string; url: string; durationSec: number }> = [];

        $('.__adv_list_track').each((_, element) => {
            const artistEl = $(element).find('.playlist-name-artist');
            const nameEl = $(element).find('.playlist-name-title');
            const linkEl = $(element).find('a.playlist-play.__adv_stream');
            const durationEl = $(element).find('.playlist-duration.__adv_duration');

            const artist = artistEl.text().trim();
            const trackName = nameEl.text().trim();
            const downloadUrl = linkEl.attr('data-url');
            const durationStr = durationEl.text().trim();

            if (artist && trackName && downloadUrl) {
                const durationSec = parseDuration(durationStr);
                tracks.push({ artist, name: trackName, url: downloadUrl, durationSec });
            }
        });

        return tracks;
    } catch (error) {
        console.error(`Fetch failed for ${url} after retries:`, error);
        return null;
    }
}

// ========== YouTube / Chosic ==========
async function getTrackFromYoutube(
    { name, author }: { name: string; author: string }
): Promise<{ service: string; url: string } | null> {
    const baseUrl = 'https://www.chosic.com';
    const headers = {
        'accept': '*/*',
        'accept-language': 'en-US,en;q=0.9,ru;q=0.8',
        'origin': baseUrl,
        'referer': `${baseUrl}/playlist-generator/`,
        'user-agent': 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36',
        'x-requested-with': 'XMLHttpRequest'
    };

    try {
        const cookies = await jar.getCookies(baseUrl);
        if (cookies.length === 0) {
            await client.post(`${baseUrl}/api/tools/handshake/`, {}, { headers });
        }

        const response = await client.get(`${baseUrl}/api/tools/get-song-video`, {
            params: { song: name, artist: author },
            headers: headers
        });

        if (response.data) {
            const videoId = String(response.data).replace(/"/g, '');
            return { service: "youtube", url: videoId };
        }
        return null;
    } catch (error: any) {
        console.error("Ошибка Chosic API:", error.response?.status, error.response?.data);
        if (error.response?.status === 401) {
            await jar.removeAllCookies();
        }
        return null;
    }
}

function removeParentheses(text: string): string {
    return text.replace(/[\[\(][^\]\)]*[\]\)]/g, '').replace(/\s+/g, ' ').trim();
}

function normalizeText(text: string): string {
    return text.replace(/[^\p{L}\p{N}\s]/gu, ' ').replace(/\s+/g, ' ').trim().toLowerCase();
}

let limiter: any;
async function getLimiter() {
    if (!limiter) {
        const pLimit = await getPLimit();
        limiter = pLimit(5);
    }
    return limiter;
}


// ========== Основная функция поиска (без кеша) ==========
export async function findTrackSong({
                                        name,
                                        author,
                                        expectedDurationSec
                                    }: {
    name: string;
    author: string;
    expectedDurationSec?: number
}): Promise<{ service: string; url: string; } | null> {
    const originalName = name;
    const originalAuthor = author;

    const cleanedName = removeParentheses(originalName);
    const cleanedAuthor = removeParentheses(originalAuthor);

    const nameVariants = [originalName];
    if (cleanedName && cleanedName !== originalName) {
        nameVariants.push(cleanedName);
    }

    const authorVariants = [originalAuthor];
    if (cleanedAuthor && cleanedAuthor !== originalAuthor) {
        authorVariants.push(cleanedAuthor);
    }

    const urlSet = new Set<string>();

    for (const nameVariant of nameVariants) {
        const slugName = toSlug(nameVariant);
        urlSet.add(punycode.toASCII(slugName));

        for (const authorVariant of authorVariants) {
            const slugAuthor = toSlug(authorVariant);
            const fullSubdomain = `${slugAuthor}-${slugName}`;
            urlSet.add(punycode.toASCII(fullSubdomain));
            urlSet.add(slugAuthor)
        }
    }

    const urls = Array.from(urlSet).map(ascii => `https://${ascii}.skysound7.com`);
    const nameVariantNorm = nameVariants.map(v => normalizeText(v));

    const MAX_DURATION_SEC = 15 * 60;
    const MAX_DURATION_DIFF = 15;

    let bestTrack: { artist: string; name: string; url: string; durationSec: number } | null = null;
    let bestDiff = Infinity;

    // Параллельные запросы с ограничением (максимум 5 одновременно)
    const limit = await getLimiter()
    const fetchPromises = urls.map(url =>
        limit(async () => {
            const tracks = await fetchTracksFromUrl(url);
            return { url, tracks };
        })
    );

    const results = await Promise.allSettled(fetchPromises);

    for (const result of results) {
        if (result.status === 'rejected') {
            console.error('Unexpected error in fetch promise:', result.reason);
            continue;
        }
        const { url, rawTracks } = result.value;
        const tracks = rawTracks as {artist: string, name: string, url: string, durationSec: number}[]
        if (!tracks || tracks.length === 0) {
            console.log(`No tracks or fetch failed for ${url}`);
            continue;
        }

        // Отладка (можно убрать в production)
        tracks.forEach(t => {
            const normName = normalizeText(t.name);
            const normArtist = normalizeText(t.artist);
            console.log(`Track: "${t.name}" (norm: "${normName}") – "${t.artist}" (norm: "${normArtist}")`);
        });

        const matchedOnPage = tracks.filter(track => {
            const normTrackName = normalizeText(track.name);
            let bestNameRatio = 0;
            for (const nv of nameVariantNorm) {
                const ratio = fuzzball.token_sort_ratio(normTrackName, nv);
                if (ratio > bestNameRatio) bestNameRatio = ratio;
            }

            let artistMatch = false;
            for (const av of authorVariants) {
                if (authorMatchesArtists(av, track.artist)) {
                    artistMatch = true;
                    break;
                }
            }

            console.log(`Comparing: "${normTrackName}" bestNameRatio=${bestNameRatio}, artistMatch=${artistMatch}`);
            return bestNameRatio > 85 && artistMatch;
        });

        if (matchedOnPage.length === 0) {
            console.log('No matches on this page, trying next URL...');
            continue;
        }

        if (expectedDurationSec) {
            const candidates = matchedOnPage.filter(t =>
                t.durationSec <= MAX_DURATION_SEC &&
                Math.abs(t.durationSec - expectedDurationSec) <= MAX_DURATION_DIFF
            );
            if (candidates.length > 0) {
                const bestOnPage = candidates.reduce((a, b) =>
                    Math.abs(a.durationSec - expectedDurationSec) < Math.abs(b.durationSec - expectedDurationSec) ? a : b
                );
                const diff = Math.abs(bestOnPage.durationSec - expectedDurationSec);
                if (diff < bestDiff) {
                    bestDiff = diff;
                    bestTrack = bestOnPage;
                }
            }
        } else {
            const filtered = matchedOnPage.filter(t => t.durationSec <= MAX_DURATION_SEC);
            if (filtered.length > 0) {
                const bestOnPage = filtered.reduce((a, b) => a.durationSec > b.durationSec ? a : b);
                if (!bestTrack || bestOnPage.durationSec > bestTrack.durationSec) {
                    bestTrack = bestOnPage;
                }
            }
        }
    }

    if (bestTrack) {
        console.log(`Selected track: ${bestTrack.name} – ${bestTrack.artist} (${bestTrack.durationSec}s)`);
        return { service: "skysound", url: bestTrack.url };
    }

    console.log('No tracks matched the criteria, trying YouTube...');
    for (const authorVariant of authorVariants) {
        const slugAuthor = toSlug(authorVariant);
        const res = await getTrackFromYoutube({ name, author: slugAuthor });
        console.log(res);
        if (res) {
            return res;
        }
    }

    return null;
}