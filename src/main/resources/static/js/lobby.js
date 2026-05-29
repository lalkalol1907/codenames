// @ts-check
/// <reference path="./types.js" />

(function () {
    const root = document.getElementById('lobby-root');
    if (!root) return;

    /** @type {I18n} */
    const i18n = window.i18n || {};
    const roomCode = root.dataset.room;
    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${location.host}/ws/rooms/${roomCode}`;

    /** @type {RoomViewDto|null} */
    let view = window.initialView || null;

    /** @type {WebSocket|null} */
    let ws;

    /** @type {ReturnType<typeof setTimeout>|undefined} */
    let reconnectTimer;

    let leaveSent = false;

    /**
     * @param {RoomViewDto|null|undefined} state
     * @returns {boolean}
     */
    function isStillInRoom(state) {
        if (!state || !Array.isArray(state.players) || !state.viewerId) return true;
        const viewerId = state.viewerId.toLowerCase();
        return state.players.some((p) => (p.id || '').toLowerCase() === viewerId);
    }

    /**
     * @param {string} template
     * @param {...(string|number|null|undefined)} args
     * @returns {string}
     */
    function fmt(template, ...args) {
        return template.replace(/\{(\d+)\}/g, (_, index) => args[Number(index)] ?? '');
    }

    /**
     * @param {string} team
     * @returns {string}
     */
    function teamLabel(team) {
        return (i18n.teams && i18n.teams[team]) || team;
    }

    /**
     * @param {string} role
     * @returns {string}
     */
    function roleLabel(role) {
        return (i18n.roles && i18n.roles[role]) || role;
    }

    /**
     * @param {TeamId|string|null|undefined} team
     * @returns {string}
     */
    function teamBadgeClass(team) {
        if (team === 'RED') return 'badge--red';
        if (team === 'BLUE') return 'badge--blue';
        return 'badge--muted';
    }

    function connect() {
        if (leaveSent) return;
        ws = new WebSocket(wsUrl);
        ws.onmessage = (event) => {
            /** @type {WsServerMessage} */
            const msg = JSON.parse(event.data);
            if (msg.view) {
                view = msg.view;
                render();
                if (view.status === 'PLAYING' || view.status === 'FINISHED') {
                    window.location.assign(`/rooms/${roomCode}/game`);
                    return;
                }
                if (!isStillInRoom(view)) {
                    window.location.assign('/');
                }
            } else if (msg.type === 'error' || msg.message) {
                alert(msg.message);
            }
        };
        ws.onclose = (event) => {
            if (leaveSent || event.code === 1008) return;
            clearTimeout(reconnectTimer);
            reconnectTimer = setTimeout(connect, 2000);
        };
    }

    window.addEventListener('pagehide', () => {
        leaveSent = true;
        clearTimeout(reconnectTimer);
        if (ws && ws.readyState === WebSocket.OPEN) {
            ws.close();
        }
    });

    /**
     * @param {PlayerViewDto} p
     * @returns {string}
     */
    function renderPlayer(p) {
        const initial = escapeHtml((p.name || '?').charAt(0).toUpperCase());
        const hostBadge = (p.isHost || p.host)
            ? `<span class="badge badge--host">${escapeHtml(i18n.host || 'Host')}</span>`
            : '';
        let roleBadge;
        if (p.team && p.role) {
            roleBadge = `<span class="badge ${teamBadgeClass(p.team)}">${escapeHtml(teamLabel(p.team))} · ${escapeHtml(roleLabel(p.role))}</span>`;
        } else {
            roleBadge = `<span class="badge badge--muted">${escapeHtml(i18n.choosingRole || 'choosing role')}</span>`;
        }
        return `<li class="player-item">
            <span class="player-avatar">${initial}</span>
            <div class="player-info">
                <div class="player-name">${escapeHtml(p.name)}</div>
                <div class="player-meta">${hostBadge}${roleBadge}</div>
            </div>
        </li>`;
    }

    function render() {
        const list = document.getElementById('player-list');
        if (!view || !Array.isArray(view.players)) {
            if (list) list.innerHTML = '';
            return;
        }
        list.innerHTML = view.players.map(renderPlayer).join('');

        const rolesActions = document.getElementById('roles-actions');
        const isHost = view.hostPlayerId === view.viewerId;
        if (rolesActions) {
            if (isHost) {
                const disabled = view.canRandomizeTeams ? '' : ' disabled';
                rolesActions.innerHTML = `
                    <form method="post" action="/rooms/${roomCode}/randomize">
                        <button type="submit" class="btn--secondary"${disabled}>${escapeHtml(i18n.randomizeTeams || 'Randomize teams')}</button>
                        <p class="hint">${escapeHtml(i18n.randomizeHint || '')}</p>
                    </form>
                `;
            } else {
                rolesActions.innerHTML = '';
            }
        }

        const startSection = document.getElementById('start-section');
        if (!startSection) return;
        if (isHost) {
            const disabled = view.canStart ? '' : ' disabled';
            const hint = view.canStart
                ? (i18n.startHint || '')
                : fmt(i18n.waitingHint || 'Waiting ({0}/4)', view.players.length);
            startSection.innerHTML = `
                <form method="post" action="/rooms/${roomCode}/start">
                    <button type="submit" class="primary"${disabled}>${escapeHtml(i18n.startGame || 'Start game')}</button>
                    <p class="hint">${escapeHtml(hint)}</p>
                </form>
            `;
        } else {
            startSection.innerHTML = '';
        }
    }

    /**
     * @param {string} text
     * @returns {string}
     */
    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    render();
    connect();
})();
