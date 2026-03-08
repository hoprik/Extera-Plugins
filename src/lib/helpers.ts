import { Response } from 'express';
import fuzzball from "fuzzball";

export function createError(res: Response, textError: string, payload: any | null = null) {
    res.status(400).send({
        "success": false,
        "error": textError,
        "errorPayload": payload
    });
}

export function createSuccess(res: Response, payload: any) {
    res.status(200).send({
        "success": true,
        ...payload
    });
}

const cyrillicToLatin: Record<string, string> = {
    'а':'a','б':'b','в':'v','г':'g','д':'d','е':'e','ё':'yo','ж':'zh','з':'z','и':'i','й':'y','к':'k','л':'l','м':'m','н':'n','о':'o','п':'p','р':'r','с':'s','т':'t','у':'u','ф':'f','х':'kh','ц':'ts','ч':'ch','ш':'sh','щ':'shch','ъ':'','ы':'y','ь':'','э':'e','ю':'yu','я':'ya'
};

function toLatin(text: string) {
    return text.split('').map(ch => cyrillicToLatin[ch] || ch).join('');
}

export function authorMatchesArtists(author: string, artists: { name: string }[]) {
    const cleaned = author.replace(/[^\p{L}\p{N}\s]/gu, ' ');
    const parts = cleaned.split(/\s+/).filter(p => p.length > 0);
    if (parts.length === 0) return false;

    const lowerParts = parts.map(p => p.toLowerCase());
    const lowerLatinParts = lowerParts.map(p => toLatin(p));

    return artists.some(artist => {
        const artistNameLower = artist.name.toLowerCase();
        const artistNameLatinLower = toLatin(artistNameLower);

        return lowerParts.some((part, index) => {
            if (part.length < 2) return false;
            const latinPart = lowerLatinParts[index];
            // Используем partial_ratio для поиска подстроки
            const match = fuzzball.partial_ratio(artistNameLower, part) > 70
                || fuzzball.partial_ratio(artistNameLatinLower, latinPart) > 70;
            // Для отладки можно оставить, но потом убрать
            // console.log(artistNameLower, part, artistNameLatinLower, latinPart, match);
            return match;
        });
    });
}