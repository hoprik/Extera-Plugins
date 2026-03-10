import {createHash} from 'crypto';
import {createWriteStream, promises as fs, existsSync} from 'fs';
import path from 'path';
import {pipeline} from 'stream/promises';
import ffmpeg from 'fluent-ffmpeg';
import youtubedl from 'youtube-dl-exec';
import {ProxyAgent, fetch} from 'undici'
import {Readable} from 'stream';

async function downloadFile(url: string, destPath: string): Promise<Boolean> {
    const response = await fetch(url);
    if (!response.ok) return false;
    if (!response.body)return false;
    const fileStream = createWriteStream(destPath);
    await pipeline(Readable.fromWeb(response.body as any), fileStream);
    return true
}

async function createFolders() {
    if (!existsSync("downloaded")) {
        await fs.mkdir("downloaded")
    }
    if (!existsSync("songs")) {
        await fs.mkdir("songs")
    }
}

export async function skySoundDownload(soundUrl: string, path: string, name: string, author: string, outputPath: string){
    const res = await downloadFile(soundUrl, path)
    if (!res){
        return false
    }
    await new Promise<void>((resolve, reject) => {
        const command = ffmpeg(path)
            .audioBitrate(128) // сжатие до 128 kbps
            .audioCodec('libmp3lame')
            .outputOptions('-id3v2_version', '3')
            .outputOptions('-metadata', `title=${name}`)
            .outputOptions('-metadata', `artist=${author}`)
            .on('end', () => resolve())
            .on('error', (err) => reject(err));

        command.save(outputPath);
    });
    return true
}

export async function youtubeDownload(
    videoId: string,
    outputPath: string,
    name: string,
    author: string
) {

    await youtubedl('https://www.youtube.com/watch?v=' + videoId, {
        extractAudio: true,
        audioFormat: 'mp3',
        audioQuality: 0,

        output: outputPath,

        addMetadata: true,

        postprocessorArgs: `-metadata title="${name}" -metadata artist="${author}"`,

        noCheckCertificates: true,
        noWarnings: true,
        preferFreeFormats: true,
        addHeader: [
            'referer:youtube.com',
            'user-agent:googlebot'
        ]
    });

    return true
}

export async function getTrackInFolder(name: string, author: string){
    const hash = createHash("md5").update(name + author).digest('hex');

    const downloadPath = path.join("downloaded", `${hash}.mp3`)
    const outputPath = path.join("songs", `${hash}.mp3`);
    try {
        await fs.access(outputPath);
        return hash;
    } catch {
        return null
    }
}

export async function getSongByUrl(songUrl: { service: string; url: string; } , name: string, author: string) {
    await createFolders()

    const hash = createHash("md5").update(name + author).digest('hex');

    const downloadPath = path.join("downloaded", `${hash}.mp3`)
    const outputPath = path.join("songs", `${hash}.mp3`);
    try {
        await fs.access(outputPath);
        return hash;
    } catch {
    }

    if (songUrl.service == "skysound"){
        const res = await skySoundDownload(songUrl.url, downloadPath, name, author, outputPath)
        if (!res){
            return null
        }
    }
    if (songUrl.service == "youtube"){
        const res = await youtubeDownload(songUrl.url, outputPath, name, author)
        if (!res){
            return null
        }
    }

    return hash
}

export async function downloadSong(hash: string) {
    const outputPath = path.join("songs", `${hash}.mp3`);
    try {
        await fs.access(outputPath);
        return outputPath
    } catch {
        return null;
    }
}