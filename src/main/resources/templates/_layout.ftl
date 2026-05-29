<#macro layout title wide=false>
<!DOCTYPE html>
<html lang="${locale}">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${title} — ${msg("app.title")}</title>
    <link rel="stylesheet" href="/static/css/game.css">
</head>
<body>
<div class="bg-glow" aria-hidden="true"></div>
<header class="site-header">
    <a href="/" class="logo">${msg("app.title")}</a>
    <form method="post" action="/locale" class="locale-form">
        <input type="hidden" name="redirect" value="${redirect}">
        <label class="locale-label">
            <span class="locale-label-text">${msg("locale.label")}</span>
            <select name="locale" onchange="this.form.submit()">
                <option value="ru"<#if locale == "ru"> selected</#if>>${msg("lang.ru")}</option>
                <option value="en"<#if locale == "en"> selected</#if>>${msg("lang.en")}</option>
            </select>
        </label>
    </form>
</header>
<main class="container<#if wide> container--wide</#if>">
    <#nested>
</main>
</body>
</html>
</#macro>
