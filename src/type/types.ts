export interface Track{
    name: string
    author: string
    coverUrl?: string
    length: number
    release?: Release
    features?: Features
    spotifyId: string
    songUrl?: string | null
}

export interface Release{
    name: string
    coverUrl: string
    type: "single" | "album"
    tracks: number
    releaseDate: number
    label: string
}

export interface Features{
    acousticness: number,
    createdAt: number,
    danceability: number,
    duration_ms: number,
    energy: number,
    instrumentalness: number,
    key: number,
    liveness: number,
    loudness: number,
    mode: number,
    speechiness: number,
    tempo: number,
    time_signature: number,
    valence: number
}