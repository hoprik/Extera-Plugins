import {createHash} from 'crypto';
import {createWriteStream, promises as fs, existsSync} from 'fs';
import path from 'path';
import {pipeline} from 'stream/promises';
import ffmpeg from 'fluent-ffmpeg';
import {ProxyAgent, fetch} from 'undici'
import {Readable} from 'stream';

async function downloadFile(url: string, destPath: string): Promise<void> {
    const proxy = {
        'host': process.env.PROXY_HOST,
        'port': process.env.PROXY_PORT,
    };
    const proxyAgent = new ProxyAgent({
        uri: `http://${proxy.host}:${proxy.port}`,
    });

    const response = await fetch(url);
    if (!response.ok) throw new Error(`Failed to download ${url}: ${response.statusText}`);
    if (!response.body) throw new Error('No response body');
    const fileStream = createWriteStream(destPath);
    await pipeline(Readable.fromWeb(response.body as any), fileStream);
    proxyAgent.close()
}

async function createFolders() {
    if (!existsSync("downloaded")) {
        await fs.mkdir("downloaded")
    }
    if (!existsSync("songs")) {
        await fs.mkdir("songs")
    }
}

export async function getSongByUrl(songUrl: string, name: string, author: string) {
    await createFolders()

    const hash = createHash("md5").update(name + author).digest('hex');

    const downloadPath = path.join("downloaded", `${hash}.mp3`)
    const outputPath = path.join("songs", `${hash}.mp3`);
    try {
        await fs.access(outputPath);
        return hash;
    } catch {
    }
    await downloadFile(songUrl, downloadPath)
    await new Promise<void>((resolve, reject) => {
        const command = ffmpeg(downloadPath)
            .audioBitrate(128) // сжатие до 128 kbps
            .audioCodec('libmp3lame')
            .outputOptions('-id3v2_version', '3')
            .outputOptions('-metadata', `title=${name}`)
            .outputOptions('-metadata', `artist=${author}`)
            .on('end', () => resolve())
            .on('error', (err) => reject(err));

        command.save(outputPath);
    });

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