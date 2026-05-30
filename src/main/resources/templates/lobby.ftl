<#import "_layout.ftl" as layout>
<#import "_app-data.ftl" as appData>
<@layout.layout title="${msg('lobby.title', room.code)}">
<div id="lobby-root" data-room="${room.code}" data-csrf="${csrf}">
    <header style="margin-bottom: 1.5rem;">
        <p class="page-subtitle" style="margin-bottom: 0.25rem;">${msg("lobby.title", room.code)}</p>
        <p class="room-badge">${room.code}</p>
        <p class="room-meta">
            ${msg("lobby.game_language")}: <strong>${room.language.code?upper_case}</strong>
            · ${msg("lobby.share_code")}
        </p>
    </header>

    <div id="lobby-error">
        <#if error?? && error?has_content>
            <p class="alert-error" role="alert">${error}</p>
        </#if>
    </div>

    <div class="grid-lobby">
        <section class="panel">
            <h2 class="section-title">${msg("lobby.players")}</h2>
            <ul class="player-list" id="player-list"></ul>
        </section>

        <section class="panel">
            <h2 class="section-title">${msg("lobby.choose_team")}</h2>
            <form method="post" action="/rooms/${room.code}/role" class="form-stack">
                <input type="hidden" name="_csrf" value="${csrf}">
                <div class="form-row form-row--2">
                    <label class="field">
                        <span class="field-label">${msg("lobby.team")}</span>
                        <select name="team" required>
                            <#list teams as t>
                                <option value="${t.value}">${t.label}</option>
                            </#list>
                        </select>
                    </label>
                    <label class="field">
                        <span class="field-label">${msg("lobby.role")}</span>
                        <select name="role" required>
                            <#list roles as r>
                                <option value="${r.value}">${r.label}</option>
                            </#list>
                        </select>
                    </label>
                </div>
                <button type="submit">${msg("lobby.save")}</button>
            </form>
            <div id="roles-actions" class="roles-actions"></div>
            <div id="start-section" class="start-section"></div>
        </section>
    </div>
</div>
<@appData.script/>
<script src="/static/js/lobby.js"></script>
</@layout.layout>
