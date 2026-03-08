import {ResGetAlbum, ResStatsFmFind} from "../type/apiTypes";

export async function findTrack({name}:{name: string}){
    const myHeaders = new Headers();
    myHeaders.append("accept", "*/*");
    myHeaders.append("accept-language", "en-US,en;q=0.9,ru;q=0.8");
    myHeaders.append("origin", "https://stats.fm");
    myHeaders.append("priority", "u=1, i");
    myHeaders.append("sec-ch-ua", "\"Google Chrome\";v=\"143\", \"Chromium\";v=\"143\", \"Not A(Brand\";v=\"24\"");
    myHeaders.append("sec-ch-ua-mobile", "?0");
    myHeaders.append("sec-ch-ua-platform", "\"Linux\"");
    myHeaders.append("sec-fetch-dest", "empty");
    myHeaders.append("sec-fetch-mode", "cors");
    myHeaders.append("sec-fetch-site", "same-site");
    myHeaders.append("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36");

    const res = await fetch(`https://api.stats.fm/api/v1/search/elastic?query=${name}&type=album%2Cartist%2Ctrack%2Cuser&limit=50`, {
        method: "GET",
        headers: myHeaders,
        redirect: "follow"
    })

    return await res.json() as ResStatsFmFind
}

export async function getAlbum({albumId}:{albumId: number}){
    const myHeaders = new Headers();
    myHeaders.append("accept", "*/*");
    myHeaders.append("accept-language", "en-US,en;q=0.9,ru;q=0.8");
    myHeaders.append("priority", "u=1, i");
    myHeaders.append("referer", "https://stats.fm/search?query=%D1%8F+%D1%80%D1%83%D1%81%D1%81%D0%BA%D0%B8%D0%B9");
    myHeaders.append("sec-ch-ua", "\"Google Chrome\";v=\"143\", \"Chromium\";v=\"143\", \"Not A(Brand\";v=\"24\"");
    myHeaders.append("sec-ch-ua-mobile", "?0");
    myHeaders.append("sec-ch-ua-platform", "\"Linux\"");
    myHeaders.append("sec-fetch-dest", "empty");
    myHeaders.append("sec-fetch-mode", "cors");
    myHeaders.append("sec-fetch-site", "same-origin");
    myHeaders.append("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36");
    myHeaders.append("x-nextjs-data", "1");
    myHeaders.append("Cookie", "_ga=GA1.1.917318341.1769080297; cf-country=RU; cf_clearance=vluP5QpF2FA2a6K_Ij8E6Xbtw.dHcZsZR_GRu.dsx2s-1772920130-1.2.1.1-id_MzLoH.DzQMlBkFx_mwcDRMsQbUEuDFNC3deJXijnih1u2zJsfSmH_5T1JdMbCwx4yIf6FM3gKcOWhrIz1LzkGma2cDVyDCyKQFIRMPslMsTjmmQTB5HHUCsg4BLjiKjwJhgSp73L549P3mHswetje7brqWkU3CxWBuzzUh2llJSEWtijhiHkJCEFRgO829bCK1Py7FBTQckVy2epNsYlqtWDNi98uU.aT00X7fco; _ga_3PMBLRJDEB=GS2.1.s1772917892$o7$g1$t1772920561$j27$l0$h0");


    const res=  await fetch(`https://stats.fm/_next/data/statsfm-site/album/${albumId}.json?id=${albumId}`, {
        method: "GET",
        headers: myHeaders,
        redirect: "follow"
    })

    if (res.status !== 200){
        return null
    }

    return await res.json() as ResGetAlbum
}

