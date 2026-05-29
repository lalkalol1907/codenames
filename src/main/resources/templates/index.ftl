<#import "_layout.ftl" as layout>
<@layout.layout title="${msg('home.title')}">
<div class="hero">
    <h1 class="page-title">${msg("home.title")}</h1>
    <p class="hero-tagline">Word game · Teams · Deduction</p>
</div>

<#if error??>
    <p class="alert-error" role="alert">${error}</p>
</#if>

<div class="grid-2">
    <section class="panel">
        <h2 class="section-title">
            <span class="section-icon" aria-hidden="true">✦</span>${msg("home.create_room")}
        </h2>
        <form method="post" action="/rooms" class="form-stack">
            <label class="field">
                <span class="field-label">${msg("home.name")}</span>
                <input type="text" name="name" required maxlength="64" autocomplete="nickname">
            </label>
            <label class="field">
                <span class="field-label">${msg("home.game_language")}</span>
                <select name="language">
                    <#list gameLanguages as lang>
                        <option value="${lang.value}">${lang.label}</option>
                    </#list>
                </select>
            </label>
            <button type="submit" class="primary">${msg("home.create")}</button>
        </form>
    </section>

    <section class="panel">
        <h2 class="section-title">
            <span class="section-icon section-icon--join" aria-hidden="true">→</span>${msg("home.join_room")}
        </h2>
        <form method="post" action="/rooms/join" class="form-stack">
            <label class="field">
                <span class="field-label">${msg("home.room_code")}</span>
                <input type="text" name="code" required maxlength="4" pattern="[A-Za-z0-9]{4}"
                       class="mono" placeholder="ABCD" autocomplete="off">
            </label>
            <label class="field">
                <span class="field-label">${msg("home.name")}</span>
                <input type="text" name="name" required maxlength="64" autocomplete="nickname">
            </label>
            <button type="submit" class="primary">${msg("home.join")}</button>
        </form>
    </section>
</div>
</@layout.layout>
