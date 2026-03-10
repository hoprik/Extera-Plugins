import express, { Express, Request, Response, NextFunction } from 'express';
import path from 'path';
import cookieParser from 'cookie-parser';
import logger from 'morgan';
import cors from 'cors'

import tracksRouter from './routes/tracks';
const app: Express = express();

app.use(cors())
app.use(logger('dev'));
app.use(express.json());
app.use(express.urlencoded({ extended: false }));
app.use(cookieParser());
app.use(express.static(path.join(__dirname, 'public')));
app.use('/api', tracksRouter);

app.get('/', (req: Request, res: Response)=>{
    res.send("<!DOCTYPE html>\n" +
        "<html lang=\"ru\">\n" +
        "<head>\n" +
        "    <meta charset=\"UTF-8\">\n" +
        "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
        "    <title>Поиск трека через API</title>\n" +
        "    <style>\n" +
        "        body {\n" +
        "            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\n" +
        "            background-color: #1e1e1e;\n" +
        "            color: #fff;\n" +
        "            display: flex;\n" +
        "            flex-direction: column;\n" +
        "            align-items: center;\n" +
        "            padding: 40px 20px;\n" +
        "            margin: 0;\n" +
        "        }\n" +
        "\n" +
        "        /* Огромная плашка BETA */\n" +
        "        .beta-banner {\n" +
        "            background: linear-gradient(135deg, #ffaa00, #ff6600);\n" +
        "            color: #000;\n" +
        "            font-size: 6rem;\n" +
        "            font-weight: 900;\n" +
        "            text-align: center;\n" +
        "            text-transform: uppercase;\n" +
        "            letter-spacing: 10px;\n" +
        "            width: 100%;\n" +
        "            max-width: 700px;\n" +
        "            padding: 20px 0;\n" +
        "            margin-bottom: 20px;\n" +
        "            border-radius: 20px;\n" +
        "            box-shadow: 0 0 30px rgba(255, 102, 0, 0.8);\n" +
        "            border: 3px solid #fff;\n" +
        "            text-shadow: 2px 2px 0 rgba(255,255,255,0.3);\n" +
        "            transform: rotate(-1deg);\n" +
        "            transition: transform 0.2s;\n" +
        "        }\n" +
        "        .beta-banner:hover {\n" +
        "            transform: rotate(0deg) scale(1.02);\n" +
        "        }\n" +
        "\n" +
        "        /* Дисклеймер */\n" +
        "        .disclaimer {\n" +
        "            background-color: #333;\n" +
        "            border-left: 8px solid #ffaa00;\n" +
        "            color: #ddd;\n" +
        "            padding: 20px 25px;\n" +
        "            border-radius: 12px;\n" +
        "            width: 100%;\n" +
        "            max-width: 600px;\n" +
        "            margin-bottom: 30px;\n" +
        "            font-size: 1.1rem;\n" +
        "            box-shadow: 0 4px 12px rgba(0,0,0,0.5);\n" +
        "            line-height: 1.5;\n" +
        "        }\n" +
        "        .disclaimer a {\n" +
        "            color: #ffaa00;\n" +
        "            font-weight: bold;\n" +
        "            text-decoration: none;\n" +
        "            border-bottom: 1px dashed #ffaa00;\n" +
        "        }\n" +
        "        .disclaimer a:hover {\n" +
        "            color: #fff;\n" +
        "            border-bottom: 1px solid #fff;\n" +
        "        }\n" +
        "        .disclaimer strong {\n" +
        "            color: #fff;\n" +
        "        }\n" +
        "\n" +
        "        .container {\n" +
        "            background-color: #2d2d2d;\n" +
        "            padding: 30px;\n" +
        "            border-radius: 12px;\n" +
        "            box-shadow: 0 8px 16px rgba(0,0,0,0.3);\n" +
        "            width: 100%;\n" +
        "            max-width: 500px;\n" +
        "            margin-bottom: 20px;\n" +
        "        }\n" +
        "        h2 {\n" +
        "            margin-top: 0;\n" +
        "            color: #4caf50;\n" +
        "            text-align: center;\n" +
        "        }\n" +
        "        .form-group {\n" +
        "            margin-bottom: 15px;\n" +
        "        }\n" +
        "        label {\n" +
        "            display: block;\n" +
        "            margin-bottom: 5px;\n" +
        "            font-size: 14px;\n" +
        "            color: #bbb;\n" +
        "        }\n" +
        "        input {\n" +
        "            width: 100%;\n" +
        "            padding: 10px;\n" +
        "            border: 1px solid #444;\n" +
        "            border-radius: 6px;\n" +
        "            background-color: #1a1a1a;\n" +
        "            color: #fff;\n" +
        "            box-sizing: border-box;\n" +
        "            font-size: 16px;\n" +
        "        }\n" +
        "        button {\n" +
        "            width: 100%;\n" +
        "            padding: 12px;\n" +
        "            background-color: #4caf50;\n" +
        "            color: white;\n" +
        "            border: none;\n" +
        "            border-radius: 6px;\n" +
        "            cursor: pointer;\n" +
        "            font-size: 16px;\n" +
        "            font-weight: bold;\n" +
        "            transition: background-color 0.3s;\n" +
        "        }\n" +
        "        button:hover {\n" +
        "            background-color: #45a049;\n" +
        "        }\n" +
        "        button:disabled {\n" +
        "            background-color: #666;\n" +
        "            cursor: not-allowed;\n" +
        "        }\n" +
        "        .result-card {\n" +
        "            display: none;\n" +
        "            background-color: #2d2d2d;\n" +
        "            padding: 20px;\n" +
        "            border-radius: 12px;\n" +
        "            box-shadow: 0 8px 16px rgba(0,0,0,0.3);\n" +
        "            width: 100%;\n" +
        "            max-width: 500px;\n" +
        "            text-align: center;\n" +
        "        }\n" +
        "        .cover-image {\n" +
        "            width: 200px;\n" +
        "            height: 200px;\n" +
        "            border-radius: 8px;\n" +
        "            object-fit: cover;\n" +
        "            margin-bottom: 15px;\n" +
        "            box-shadow: 0 4px 12px rgba(0,0,0,0.5);\n" +
        "        }\n" +
        "        .track-title {\n" +
        "            font-size: 24px;\n" +
        "            font-weight: bold;\n" +
        "            margin: 10px 0 5px;\n" +
        "        }\n" +
        "        .track-author {\n" +
        "            font-size: 18px;\n" +
        "            color: #bbb;\n" +
        "            margin: 0 0 15px;\n" +
        "        }\n" +
        "        .track-info {\n" +
        "            font-size: 14px;\n" +
        "            color: #999;\n" +
        "            margin-bottom: 20px;\n" +
        "            text-align: left;\n" +
        "            background: #1a1a1a;\n" +
        "            padding: 15px;\n" +
        "            border-radius: 8px;\n" +
        "        }\n" +
        "        .track-info p {\n" +
        "            margin: 5px 0;\n" +
        "        }\n" +
        "        audio {\n" +
        "            width: 100%;\n" +
        "            margin-top: 15px;\n" +
        "        }\n" +
        "        .error {\n" +
        "            color: #ff5252;\n" +
        "            text-align: center;\n" +
        "            margin-top: 15px;\n" +
        "            display: none;\n" +
        "        }\n" +
        "        .raw-json {\n" +
        "            margin-top: 20px;\n" +
        "            text-align: left;\n" +
        "            background: #1a1a1a;\n" +
        "            padding: 15px;\n" +
        "            border-radius: 8px;\n" +
        "            font-family: monospace;\n" +
        "            font-size: 12px;\n" +
        "            white-space: pre-wrap;\n" +
        "            overflow-x: auto;\n" +
        "        }\n" +
        "        .footer {\n" +
        "            text-align: center;\n" +
        "            margin-top: 30px;\n" +
        "            padding: 15px 10px;\n" +
        "            color: #777;\n" +
        "            font-size: 14px;\n" +
        "            border-top: 1px solid #444;\n" +
        "            width: 100%;\n" +
        "            max-width: 500px;\n" +
        "        }\n" +
        "        .footer a {\n" +
        "            color: #4caf50;\n" +
        "            text-decoration: none;\n" +
        "            font-weight: 500;\n" +
        "        }\n" +
        "        .footer a:hover {\n" +
        "            text-decoration: underline;\n" +
        "        }\n" +
        "\n" +
        "        /* Адаптация для мобильных */\n" +
        "        @media (max-width: 600px) {\n" +
        "            .beta-banner {\n" +
        "                font-size: 3rem;\n" +
        "                letter-spacing: 5px;\n" +
        "            }\n" +
        "            .disclaimer {\n" +
        "                font-size: 1rem;\n" +
        "                padding: 15px;\n" +
        "            }\n" +
        "        }\n" +
        "    </style>\n" +
        "</head>\n" +
        "<body>\n" +
        "\n" +
        "    <!-- Огромная плашка BETA -->\n" +
        "    <div class=\"beta-banner\">BETA</div>\n" +
        "\n" +
        "    <!-- Дисклеймер -->\n" +
        "    <div class=\"disclaimer\">\n" +
        "        <strong>⚠️ ВНИМАНИЕ:</strong> О багах поисковика писать по данным местам \n" +
        "        <a href=\"https://t.me/hopriksplugins/5\" target=\"_blank\" rel=\"noopener noreferrer\">https://t.me/hopriksplugins/5</a>. \n" +
        "        Данная страница используется как просто интерфейс, взаимодействия, и была за вайбкожина за 10 минут. И не будет нигде использоватся, \n" +
        "        прозьба не писать о багах фронтэнда.\n" +
        "    </div>\n" +
        "\n" +
        "    <div class=\"container\">\n" +
        "        <h2>Поиск Трека - (35% шанс что найденый вами трэк будет без цензуры)</h2>\n" +
        "        <div class=\"form-group\">\n" +
        "            <label for=\"trackName\">Название песни</label>\n" +
        "            <input type=\"text\" id=\"trackName\" value=\"Мама, Я Люблю\">\n" +
        "        </div>\n" +
        "        <div class=\"form-group\">\n" +
        "            <label for=\"trackAuthor\">Исполнитель</label>\n" +
        "            <input type=\"text\" id=\"trackAuthor\" value=\"Anacondaz\">\n" +
        "        </div>\n" +
        "        <button id=\"fetchBtn\" onclick=\"getTrack()\">Получить информацию</button>\n" +
        "        <div id=\"errorMsg\" class=\"error\">Произошла ошибка при получении данных.</div>\n" +
        "    </div>\n" +
        "\n" +
        "    <div class=\"result-card\" id=\"resultCard\">\n" +
        "        <img id=\"coverImg\" class=\"cover-image\" src=\"\" alt=\"Обложка альбома\">\n" +
        "        <div class=\"track-title\" id=\"titleEl\">Название</div>\n" +
        "        <div class=\"track-author\" id=\"authorEl\">Исполнитель</div>\n" +
        "        \n" +
        "        <div class=\"track-info\">\n" +
        "            <p><strong>Альбом:</strong> <span id=\"albumEl\"></span></p>\n" +
        "            <p><strong>Лейбл:</strong> <span id=\"labelEl\"></span></p>\n" +
        "            <p><strong>Дата релиза:</strong> <span id=\"dateEl\"></span></p>\n" +
        "            <p><strong>Длительность:</strong> <span id=\"lengthEl\"></span></p>\n" +
        "        </div>\n" +
        "\n" +
        "        <audio id=\"audioPlayer\" controls>\n" +
        "            <source id=\"audioSource\" src=\"\" type=\"audio/mpeg\">\n" +
        "            Ваш браузер не поддерживает элемент audio.\n" +
        "        </audio>\n" +
        "\n" +
        "        <details>\n" +
        "            <summary style=\"cursor: pointer; margin-top: 15px; color: #888;\">Показать сырой JSON ответ</summary>\n" +
        "            <div id=\"rawJson\" class=\"raw-json\"></div>\n" +
        "        </details>\n" +
        "    </div>\n" +
        "\n" +
        "    <div class=\"footer\">\n" +
        "        Автор: <strong>@hoprik</strong> | \n" +
        "        <a href=\"https://t.me/hopriksplugins\" target=\"_blank\" rel=\"noopener noreferrer\">Telegram канал</a>\n" +
        "    </div>\n" +
        "\n" +
        "    <script>\n" +
        "        function formatDuration(ms) {\n" +
        "            const minutes = Math.floor(ms / 60000);\n" +
        "            const seconds = ((ms % 60000) / 1000).toFixed(0);\n" +
        "            return minutes + \":\" + (seconds < 10 ? '0' : '') + seconds;\n" +
        "        }\n" +
        "\n" +
        "        async function getTrack() {\n" +
        "            const name = document.getElementById('trackName').value;\n" +
        "            const author = document.getElementById('trackAuthor').value;\n" +
        "            const btn = document.getElementById('fetchBtn');\n" +
        "            const resultCard = document.getElementById('resultCard');\n" +
        "            const errorMsg = document.getElementById('errorMsg');\n" +
        "\n" +
        "            btn.disabled = true;\n" +
        "            btn.innerText = \"Загрузка...\";\n" +
        "            errorMsg.style.display = 'none';\n" +
        "            resultCard.style.display = 'none';\n" +
        "\n" +
        "            try {\n" +
        "                const response = await fetch('https://plugins.hoprik.ru/api/getTrack', {\n" +
        "                    method: 'POST',\n" +
        "                    headers: { 'Content-Type': 'application/json' },\n" +
        "                    body: JSON.stringify({ name, author })\n" +
        "                });\n" +
        "\n" +
        "                if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);\n" +
        "\n" +
        "                const data = await response.json();\n" +
        "\n" +
        "                if (data.success) {\n" +
        "                    document.getElementById('coverImg').src = data.coverUrl;\n" +
        "                    document.getElementById('titleEl').innerText = data.name;\n" +
        "                    document.getElementById('authorEl').innerText = data.author;\n" +
        "                    document.getElementById('lengthEl').innerText = formatDuration(data.length);\n" +
        "                    \n" +
        "                    if (data.release) {\n" +
        "                        document.getElementById('albumEl').innerText = data.release.name;\n" +
        "                        document.getElementById('labelEl').innerText = data.release.label;\n" +
        "                        const releaseDate = new Date(data.release.releaseDate);\n" +
        "                        document.getElementById('dateEl').innerText = releaseDate.toLocaleDateString('ru-RU');\n" +
        "                    }\n" +
        "\n" +
        "                    const audioPlayer = document.getElementById('audioPlayer');\n" +
        "                    document.getElementById('audioSource').src = data.songUrl;\n" +
        "                    audioPlayer.load();\n" +
        "\n" +
        "                    document.getElementById('rawJson').innerText = JSON.stringify(data, null, 2);\n" +
        "                    resultCard.style.display = 'block';\n" +
        "                } else {\n" +
        "                    throw new Error(\"API вернуло success: false\");\n" +
        "                }\n" +
        "            } catch (error) {\n" +
        "                console.error('Ошибка:', error);\n" +
        "                errorMsg.innerText = \"Ошибка: Не удалось загрузить данные. Проверьте консоль.\";\n" +
        "                errorMsg.style.display = 'block';\n" +
        "            } finally {\n" +
        "                btn.disabled = false;\n" +
        "                btn.innerText = \"Получить информацию\";\n" +
        "            }\n" +
        "        }\n" +
        "    </script>\n" +
        "</body>\n" +
        "</html>")
})
export default app;