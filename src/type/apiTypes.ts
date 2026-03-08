export interface reqGetInfoTack {
    name: string;
    author: string;
}

export interface StatsFmArtist {
    id: number;
    name: string;
}

export interface StatsFmAlbum {
    id: number;
    name: string;
    image: string;
}

export interface StatsFmTrack {
    albums: StatsFmAlbum[];
    artists: StatsFmArtist[];
    durationMs: number;
    explicit: boolean;
    externalIds: {
        spotify?: string[];
        appleMusic?: string[];
    };
    id: number;
    name: string;
    spotifyPopularity: number;
    spotifyPreview: string | null;
    appleMusicPreview: string | null;
}

export interface ResStatsFmFind {
    items: {
        artists: StatsFmArtist[];
        tracks: StatsFmTrack[];
        albums: StatsFmAlbum[];
    };
}

/**
 * Внешние идентификаторы (для полной версии альбома/трека)
 */
export interface ExternalIds {
    upc: string | null;
    ean: string | null;
    isrc: string | null;
    spotify?: string[];
    appleMusic?: string[];
}

/**
 * Полная информация об исполнителе (включая изображение)
 */
export interface ArtistFull extends StatsFmArtist {
    image?: string | null;
}

/**
 * Полная информация об альбоме (расширяет StatsFmAlbum)
 */
export interface AlbumFull extends StatsFmAlbum {
    label: string;
    spotifyPopularity: number;
    totalTracks: number;
    releaseDate: number; // timestamp в миллисекундах
    genres: string[];
    artists: ArtistFull[];
    externalIds: ExternalIds;
    type: string; // например "single"
}

/**
 * Полная информация о треке (с детальными альбомами и исполнителями)
 */
export interface TrackFull {
    albums: AlbumFull[];
    artists: ArtistFull[];
    durationMs: number;
    explicit: boolean;
    externalIds: {
        spotify?: string[];
        appleMusic?: string[];
    };
    id: number;
    name: string;
    spotifyPopularity: number;
    spotifyPreview: string | null;
    appleMusicPreview: string | null;
}

/**
 * Свойства страницы для ответа с альбомом
 */
export interface PagePropsAlbum {
    album: AlbumFull;
    tracks: TrackFull[];
    user: null;
}

/**
 * Ответ API для получения альбома (resGetAlbum)
 */
export interface ResGetAlbum {
    pageProps: PagePropsAlbum;
    __N_SSP: boolean;
}