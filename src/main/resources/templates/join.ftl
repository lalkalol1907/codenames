<#import "_layout.ftl" as layout>
<@layout.layout title="${msg('join.title', code)}">
<section class="panel page-centered">
    <p class="page-subtitle page-centered__label">${msg("join.title", code)}</p>
    <p class="room-badge">${code}</p>

    <#if error??>
        <p class="alert-error" role="alert">${error}</p>
    </#if>

    <form method="post" action="/rooms/${code}/join" class="form-stack page-centered__form">
        <input type="hidden" name="_csrf" value="${csrf}">
        <label class="field">
            <span class="field-label">${msg("join.your_name")}</span>
            <input type="text" name="name" required maxlength="64" autofocus autocomplete="nickname">
        </label>
        <button type="submit" class="primary">${msg("join.enter_lobby")}</button>
    </form>
</section>
</@layout.layout>
