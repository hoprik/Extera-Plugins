import { Router, Request, Response, NextFunction } from 'express';
import {authorMatchesArtists, createError, createSuccess} from "../lib/helpers";
import {reqGetInfoTack} from "../type/apiTypes";
import * as fuzzball from "fuzzball";
import {findTrack, getAlbum} from "../lib/statsfm";
import {Track} from "../type/types";
import {findTrackSong} from "../lib/getSongUrl";
import {downloadSong, getSongByUrl, getTrackInFolder} from "../lib/downloader";

const router: Router = Router();

router.post('/getTrack', async (req: Request, res: Response, next: NextFunction) => {
    const {name, author} = await req.body as reqGetInfoTack
    const tracks = await findTrack({name})

    const track = tracks.items.tracks.filter(value => {
        return fuzzball.ratio(value.name.toLowerCase(), name.toLowerCase()) > 90 && authorMatchesArtists(author, value.artists)
    })[0]

    if (!track){
        return createError(res, "Песня не найдена", {name, author})
    }

    const album = await getAlbum({albumId: track.albums[0].id})
    if (!album){
        const trackInfo: Track = {
            name: track.name,
            author: track.artists.map(a => a.name).join(", "),
            length: track.durationMs,
            spotifyId: !!track.externalIds.spotify ? track.externalIds.spotify[0] : "",
        }
        return createSuccess(res, trackInfo)
    }

    let songUrl: string | null = null
    const hashFolder = await getTrackInFolder(track.name, track.artists.map(a => a.name).join(", "))
    if (!hashFolder){
        const findSongLink = await findTrackSong({name: track.name, author: track.artists.map(a => a.name).join(", "), expectedDurationSec: track.durationMs / 1000})
        if (findSongLink){
            const hash = await getSongByUrl(findSongLink, track.name, track.artists.map(a => a.name).join(", "))
            songUrl = req.protocol + '://' + req.get('host') + "/api/download/" + hash
        }
    }else{
        songUrl = req.protocol + '://' + req.get('host') + "/api/download/" + hashFolder
    }



    const data: Track = {
        name: track.name,
        author: track.artists.map(a => a.name).join(", "),
        length: track.durationMs,
        spotifyId: !!track.externalIds.spotify ? track.externalIds.spotify[0] : "",
        coverUrl: track.albums[0].image,
        release: {
            name: album.pageProps.album.name,
            label: album.pageProps.album.label,
            type: album.pageProps.album.type as "album" | "single",
            tracks: album.pageProps.album.totalTracks,
            coverUrl: track.albums[0].image,
            releaseDate: album.pageProps.album.releaseDate
        },
        songUrl: songUrl
    }

    return createSuccess(res, data)
});

router.get("/download/:songId", async (req: Request, res: Response)=>{
    const songId = req.params.songId as string
    const downloadFile = await downloadSong(songId)
    if (!downloadFile){
        return createError(res, "Файл не найден")
    }
    res.download(downloadFile, songId+".mp3", (err)=>{
        if (err) {
            createError(res, "Не удалось скачать", err)
        }
    })
})

export default router;