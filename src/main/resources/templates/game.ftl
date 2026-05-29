<#import "_layout.ftl" as layout>
<#import "_app-data.ftl" as appData>
<@layout.layout title="${msg('game.title', roomCode)}" wide=true>
<div class="game-layout" id="game-root" data-room="${roomCode}">
    <aside class="panel game-sidebar">
        <h2 class="section-title">${msg("lobby.players")}</h2>
        <ul class="player-list" id="player-list"></ul>
    </aside>

    <section class="panel game-panel">
        <div id="status-bar" class="status-bar"></div>
        <div id="clue-panel" class="clue-panel"></div>
        <div id="board" class="board"></div>
        <div id="controls" class="controls"></div>
        <div id="game-over" class="game-over hidden"></div>
    </section>
</div>
<@appData.script/>
<script src="/static/js/game.js"></script>
</@layout.layout>
