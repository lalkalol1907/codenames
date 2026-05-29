// @ts-check
/// <reference path="./types.js" />

(function () {
    const root = document.getElementById('game-root');
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
     * @param {string} phase
     * @returns {string}
     */
    function phaseLabel(phase) {
        return (i18n.phases && i18n.phases[phase]) || phase;
    }

    /**
     * @param {string} text
     * @param {string} [extraClass]
     * @returns {string}
     */
    function pill(text, extraClass) {
        return `<span class="stat-pill${extraClass ? ' ' + extraClass : ''}">${text}</span>`;
    }

    function connect() {
        ws = new WebSocket(wsUrl);
        ws.onmessage = (event) => {
            /** @type {WsServerMessage} */
            const msg = JSON.parse(event.data);
            if (msg.view) {
                view = msg.view;
                render();
            } else if (msg.type === 'error' || msg.message) {
                alert(msg.message);
            }
        };
        ws.onclose = () => {
            setTimeout(connect, 2000);
        };
    }

    /**
     * @param {WsClientMessage} payload
     */
    function send(payload) {
        if (ws && ws.readyState === WebSocket.OPEN) {
            ws.send(JSON.stringify(payload));
        }
    }

    /** @returns {PlayerViewDto|undefined} */
    function viewer() {
        if (!view) return undefined;
        return view.players.find((p) => p.id === view.viewerId);
    }

    function isSpymaster() {
        return viewer()?.role === 'SPYMASTER';
    }

    /**
     * @param {boolean} disabled
     * @returns {string}
     */
    function clueFormHtml(disabled) {
        const dis = disabled ? ' disabled' : '';
        return `
            <label class="field">
                <span class="field-label">${escapeHtml(i18n.clueWord || 'Clue word')}</span>
                <input type="text" id="clue-word" maxlength="64"${dis}>
            </label>
            <label class="field field--narrow">
                <span class="field-label">${escapeHtml(i18n.count || 'Count')}</span>
                <input type="number" id="clue-count" min="1" max="9" value="1"${dis}>
            </label>
            <button type="button" id="give-clue-btn"${dis}>${escapeHtml(i18n.giveClue || 'Give clue')}</button>
        `;
    }

    function bindClueForm() {
        document.getElementById('give-clue-btn').onclick = () => {
            const word = document.getElementById('clue-word').value.trim();
            const count = parseInt(document.getElementById('clue-count').value, 10);
            if (word) send({ type: 'give_clue', word, count });
        };
    }

    /**
     * @param {GameViewDto} game
     */
    function renderCluePanel(game) {
        const cluePanel = document.getElementById('clue-panel');
        cluePanel.classList.remove('clue-panel--your-turn', 'hidden');

        if (game.clueWord) {
            cluePanel.innerHTML = `<span class="clue-word">${escapeHtml(game.clueWord)}</span> · ${escapeHtml(fmt(i18n.clue || 'Clue: {0} ({1}) · Guesses left: {2}', game.clueWord, game.clueCount, game.guessesRemaining))}`;
        } else if (game.phase === 'CLUE') {
            if (view.canGiveClue) {
                cluePanel.innerHTML = `<span class="clue-panel-wait clue-panel-wait--yours">${escapeHtml(i18n.waitingYourClue || 'Your turn — give a clue')}</span>`;
                cluePanel.classList.add('clue-panel--your-turn');
            } else {
                cluePanel.textContent = i18n.waitingClue || '';
            }
        } else {
            cluePanel.textContent = '';
            cluePanel.classList.add('hidden');
        }
    }

    /**
     * @param {GameViewDto} game
     */
    function renderControls(game) {
        const controls = document.getElementById('controls');
        controls.innerHTML = '';
        controls.classList.remove('controls--disabled', 'hidden');

        if (view.canGiveClue) {
            controls.innerHTML = clueFormHtml(false);
            bindClueForm();
        } else if (view.canEndTurn) {
            controls.innerHTML = `<button type="button" id="end-turn-btn">${escapeHtml(i18n.endTurn || 'End turn')}</button>`;
            document.getElementById('end-turn-btn').onclick = () => send({ type: 'end_turn' });
        } else if (isSpymaster() && !game.winner) {
            controls.innerHTML = clueFormHtml(true);
            controls.classList.add('controls--disabled');
        } else {
            controls.classList.add('hidden');
        }
    }

    function renderPlayers() {
        const list = document.getElementById('player-list');
        if (!list || !view || !Array.isArray(view.players)) {
            if (list) list.innerHTML = '';
            return;
        }

        const teamOrder = { RED: 0, BLUE: 1 };
        const roleOrder = { SPYMASTER: 0, OPERATIVE: 1 };
        const sorted = [...view.players].sort((a, b) => {
            const byTeam = (teamOrder[a.team] ?? 2) - (teamOrder[b.team] ?? 2);
            if (byTeam !== 0) return byTeam;
            return (roleOrder[a.role] ?? 2) - (roleOrder[b.role] ?? 2);
        });

        list.innerHTML = sorted.map((p) => {
            const initial = escapeHtml((p.name || '?').charAt(0).toUpperCase());
            const isSelf = p.id === view.viewerId;
            const team = (p.team || '').toLowerCase();
            const hostTag = p.isHost
                ? `<span class="player-host">${escapeHtml(i18n.host || 'Host')}</span>`
                : '';
            const roleText = (p.team && p.role)
                ? escapeHtml(roleLabel(p.role))
                : '—';

            return `<li class="player-item player-item--${team}${isSelf ? ' player-item--self' : ''}">
                <span class="player-avatar player-avatar--${team}">${initial}</span>
                <div class="player-info">
                    <div class="player-name-row">
                        <span class="player-name">${escapeHtml(p.name)}</span>
                        ${hostTag}
                    </div>
                    <span class="player-role">${roleText}</span>
                </div>
            </li>`;
        }).join('');
    }

    /**
     * @param {CardView} card
     * @returns {string}
     */
    function frontClasses(card) {
        if (card.revealed) return 'hidden-type';
        if (isSpymaster() && card.type) return 'type-' + card.type;
        return 'hidden-type';
    }

    /**
     * @param {CardView} card
     * @returns {string}
     */
    function backClasses(card) {
        const classes = ['revealed', card.type || 'NEUTRAL'];
        if (isSpymaster()) classes.push('spymaster-revealed');
        return classes.join(' ');
    }

    /** @type {CardSnapshot[]|null} */
    let prevCardsState = null;

    /**
     * @param {CardView} card
     * @param {number} index
     * @param {boolean} animateEnter
     * @returns {HTMLDivElement}
     */
    function buildCardSlot(card, index, animateEnter) {
        const slot = document.createElement('div');
        slot.className = 'card-slot' + (animateEnter ? ' card-slot--enter' : '');
        slot.dataset.index = String(card.position);
        if (animateEnter) slot.style.animationDelay = `${index * 45}ms`;

        const flipper = document.createElement('div');
        flipper.className = 'card-flipper';
        if (card.revealed) {
            flipper.classList.add('card-flipper--revealed', 'card-flipper--instant');
        }

        const front = document.createElement('div');
        front.className = 'card card-face card-front ' + frontClasses(card);
        front.textContent = card.word;

        const back = document.createElement('div');
        back.className = 'card card-face card-back ' + backClasses(card);
        back.textContent = card.word;

        flipper.appendChild(front);
        flipper.appendChild(back);
        slot.appendChild(flipper);

        if (card.revealed) {
            requestAnimationFrame(() => flipper.classList.remove('card-flipper--instant'));
        }

        slot.classList.toggle('clickable', view.canGuess && !card.revealed);
        return slot;
    }

    /**
     * @param {HTMLDivElement} slot
     * @param {CardView} card
     * @param {boolean} newlyRevealed
     */
    function updateCardSlot(slot, card, newlyRevealed) {
        const flipper = slot.querySelector('.card-flipper');
        const front = /** @type {HTMLDivElement} */ (slot.querySelector('.card-front'));
        const back = /** @type {HTMLDivElement} */ (slot.querySelector('.card-back'));

        front.textContent = card.word;
        back.textContent = card.word;
        front.className = 'card card-face card-front ' + frontClasses(card);
        back.className = 'card card-face card-back ' + backClasses(card);

        if (card.revealed) {
            if (newlyRevealed) {
                flipper.classList.remove('card-flipper--instant');
                flipper.classList.add('card-flipper--revealed');
            } else if (!flipper.classList.contains('card-flipper--revealed')) {
                flipper.classList.add('card-flipper--revealed', 'card-flipper--instant');
                requestAnimationFrame(() => flipper.classList.remove('card-flipper--instant'));
            }
        } else {
            flipper.classList.remove('card-flipper--revealed', 'card-flipper--instant');
        }

        slot.classList.toggle('clickable', view.canGuess && !card.revealed);
    }

    /**
     * @param {GameViewDto} game
     */
    function renderBoard(game) {
        const board = document.getElementById('board');
        if (!board) return;
        const animateEnter = !board.querySelector('.card-slot');
        const sorted = [...game.cards].sort((a, b) => a.position - b.position);

        sorted.forEach((card, index) => {
            const prev = prevCardsState?.find((c) => c.position === card.position);
            const newlyRevealed = Boolean(prev && !prev.revealed && card.revealed);
            let slot = /** @type {HTMLDivElement|null} */ (
                board.querySelector(`[data-index="${card.position}"]`)
            );

            if (!slot) {
                slot = buildCardSlot(card, index, animateEnter);
                board.appendChild(slot);
            } else {
                updateCardSlot(slot, card, newlyRevealed);
            }
        });

        board.querySelectorAll('.card-slot').forEach((el) => {
            if (el.classList.contains('clickable')) {
                el.onclick = () => send({ type: 'guess', index: parseInt(el.dataset.index, 10) });
            } else {
                el.onclick = null;
            }
        });

        prevCardsState = game.cards.map((c) => ({
            position: c.position,
            revealed: c.revealed,
        }));
    }

    function render() {
        if (!view) return;
        const game = view.game;
        const statusBar = document.getElementById('status-bar');
        const gameOver = document.getElementById('game-over');

        if (!game) return;

        if (game.winner) {
            const winner = teamLabel(game.winner);
            statusBar.innerHTML =
                pill(escapeHtml(i18n.gameOver || 'Game over')) +
                pill(escapeHtml(fmt(i18n.wins || '{0} wins!', winner)), 'stat-pill--turn team-' + game.winner);
            document.getElementById('clue-panel').innerHTML = '';
            document.getElementById('clue-panel').classList.add('hidden');
            document.getElementById('controls').innerHTML = '';
            document.getElementById('controls').classList.add('hidden');
            gameOver.classList.remove('hidden');
            gameOver.textContent = fmt(i18n.teamWins || '{0} team wins!', winner);
        } else {
            gameOver.classList.add('hidden');
            statusBar.innerHTML = [
                pill(escapeHtml(fmt(i18n.turn || 'Turn: {0}', teamLabel(game.currentTeam))), 'stat-pill--turn team-' + game.currentTeam),
                pill(escapeHtml(fmt(i18n.redLeft || 'Red left: {0}', game.redRemaining)), 'team-RED'),
                pill(escapeHtml(fmt(i18n.blueLeft || 'Blue left: {0}', game.blueRemaining)), 'team-BLUE'),
                pill(escapeHtml(fmt(i18n.phase || 'Phase: {0}', phaseLabel(game.phase)))),
            ].join('');
            renderCluePanel(game);
        }

        renderBoard(game);

        renderControls(game);
        renderPlayers();
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
