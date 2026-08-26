import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import UnoGameView from './uno/UnoGameView';
import DvcGameView from './dvc/DvcGameView';
import LasVegasGameView from './lasvegas/LasVegasGameView';
import ChooseColorModal from './uno/ChooseColorModal';
import { dvcFixture, lasVegasFixture, unoFixture } from '../dev/fixtures';

describe('deterministic game views', () => {
  it('shows a playable UNO hand and the maximum player rail', () => {
    render(
      <UnoGameView
        {...unoFixture}
        playableCards={[unoFixture.hand[0], unoFixture.hand[4]]}
        myTurn
        onBack={vi.fn()}
        onPlay={vi.fn()}
        onDraw={vi.fn()}
      />,
    );
    expect(screen.getAllByRole('article')).toHaveLength(10);
    expect(screen.getByRole('button', { name: /red 7, play card/i })).toBeEnabled();
    expect(screen.getByRole('button', { name: /blue rev, not playable/i })).toBeDisabled();
    expect(screen.getByText(/your turn/i)).toBeVisible();
  });

  it('keeps the wild color picker reachable after it is hidden', () => {
    const onOpenColorPicker = vi.fn();
    render(
      <UnoGameView
        {...unoFixture}
        playableCards={[]}
        myTurn
        mustChooseColor
        onOpenColorPicker={onOpenColorPicker}
      />,
    );

    screen.getByRole('button', { name: 'Choose color' }).click();
    expect(onOpenColorPicker).toHaveBeenCalledOnce();
  });

  it('lets a player hide the wild color picker without choosing a color', () => {
    const onHide = vi.fn();
    const onPick = vi.fn();
    render(<ChooseColorModal open onHide={onHide} onPick={onPick} />);

    screen.getByRole('button', { name: 'Hide picker dialog' }).click();

    expect(onHide).toHaveBeenCalledOnce();
    expect(onPick).not.toHaveBeenCalled();
  });

  it('renders the DVC guess phase with four player perspectives', () => {
    render(
      <DvcGameView
        {...dvcFixture}
        publicTokens={new Set()}
        arrangementValid
        isMyTurn
        disabled={false}
        onSelectSelf={vi.fn()}
        onReorder={vi.fn()}
        onOpponentCardClick={vi.fn()}
        onBack={vi.fn()}
        onRefresh={vi.fn()}
        onDrawColor={vi.fn()}
        onContinueReveal={vi.fn()}
        onSelfReveal={vi.fn()}
        onSettle={vi.fn()}
      />,
    );
    expect(screen.getByText(/pick a hidden tile/i)).toBeVisible();
    expect(screen.getAllByTestId(/player-/)).toHaveLength(3);
    expect(screen.getAllByText(/PixelPilot/).length).toBeGreaterThan(0);
    expect(screen.getByRole('log', { name: /game log/i })).toBeVisible();
    expect(screen.getByText(/guessed CipherFox's #3 WHITE tile as 6 — WRONG/i)).toBeVisible();
  });

  it('locks the DVC rack against mouse and keyboard reordering after confirmation', () => {
    const { container } = render(
      <DvcGameView
        {...dvcFixture}
        awaiting="SETTLE_POSITION"
        board={{ ...dvcFixture.board, awaiting: 'SETTLE_POSITION' }}
        pendingCard={null}
        canDragInitial
        settledSubmitted
        arrangementValid
        isMyTurn
        disabled={false}
      />,
    );

    expect(container.querySelector('[draggable="true"]')).not.toBeInTheDocument();
    expect(container.querySelector('.dvc-card-hit')).toBeDisabled();
    expect(screen.getByText(/rack is locked/i)).toBeVisible();
  });

  it('disables an exhausted DVC draw pile without disabling the available color', () => {
    render(
      <DvcGameView
        {...dvcFixture}
        awaiting="DRAW_COLOR"
        board={{ ...dvcFixture.board, awaiting: 'DRAW_COLOR', deckBlackRemaining: 0, deckWhiteRemaining: 7 }}
        publicTokens={new Set()}
        arrangementValid
        isMyTurn
        disabled={false}
        onDrawColor={vi.fn()}
      />,
    );

    expect(screen.getByRole('button', { name: 'Draw black (0)' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Draw white (7)' })).toBeEnabled();
  });

  it('renders ten distinguishable Las Vegas seats and restores legal actions through the roll dialog', () => {
    const onPlace = vi.fn();
    const { container } = render(
      <LasVegasGameView
        sessionId="VEGAS-ROOM"
        gameId="game-1"
        view={lasVegasFixture}
        playerId="P1"
        connectionState="connected"
        onPlace={onPlace}
        onSkip={vi.fn()}
        onToggleAssets={vi.fn()}
        onRefresh={vi.fn()}
        onLeave={vi.fn()}
      />,
    );

    expect(screen.getAllByRole('article')).toHaveLength(10);
    expect(screen.queryByText('Room VEGAS-ROOM')).not.toBeInTheDocument();
    expect(screen.queryByText('Round 2/3')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Scroll player seats/i })).not.toBeInTheDocument();
    expect([...container.querySelectorAll('.arcade-seat__meta')].every((meta) => !meta.textContent.includes('cards'))).toBe(true);
    const toolbar = container.querySelector('.arcade-toolbar');
    const consolePanel = container.querySelector('.vegas-console');
    const revealAssets = screen.getByRole('button', { name: 'Reveal total assets' });
    expect(toolbar).toHaveClass('arcade-toolbar--actions-only');
    expect(toolbar).not.toContainElement(revealAssets);
    expect(consolePanel).toContainElement(revealAssets);
    expect(container.querySelector('.vegas-table-layout > .vegas-seat-track')).toBeInTheDocument();
    expect(container.querySelector('.vegas-table-layout > .vegas-casino-grid')).toBeInTheDocument();
    expect(container.querySelector('.vegas-table-layout > .vegas-side-column')).toBeInTheDocument();
    expect(container.querySelectorAll('.vegas-casino .vegas-die__owner')).toHaveLength(0);
    expect(container.querySelector('.vegas-current-roll')).not.toBeInTheDocument();
    expect(container.querySelectorAll('.vegas-console .vegas-die')).toHaveLength(0);
    expect(screen.queryByRole('button', { name: 'Place all 1s' })).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Show roll & actions' }));

    const dialog = screen.getByRole('dialog', { name: 'Roll complete // choose a casino' });
    expect(within(dialog).getByRole('button', { name: 'Place all 1s' })).toBeEnabled();
    expect(within(dialog).getByRole('button', { name: 'Place all 3s' })).toBeEnabled();
    expect(within(dialog).queryByRole('button', { name: 'Place all 2s' })).not.toBeInTheDocument();
    expect(dialog.querySelectorAll('.vegas-roll-die-3d')).toHaveLength(4);
    expect(dialog.querySelectorAll('.vegas-roll-cube__face')).toHaveLength(24);
    expect(dialog.querySelectorAll('[data-result-face]')).toHaveLength(4);
    within(dialog).getByRole('button', { name: 'Place all 5s' }).click();
    expect(onPlace).toHaveBeenCalledWith(5);
    expect(screen.getAllByRole('img', { name: /big die worth two/i }).length).toBeGreaterThan(0);
    expect(screen.getByText('Revealed $210,000')).toBeVisible();
  });

  it('keeps a server-authoritative 3D roll open for Hide, Show, retry, and confirmed placement', async () => {
    vi.useFakeTimers();
    const onRoll = vi.fn();
    const rollingPlayer = {
      ...lasVegasFixture.players[0],
      remainingRegularDice: 7,
      bigDieRemaining: true,
      remainingDice: 8,
    };
    const waitingView = {
      ...lasVegasFixture,
      phase: 'WAITING_FOR_ROLL',
      currentRoll: [],
      players: [rollingPlayer, ...lasVegasFixture.players.slice(1)],
    };
    const resultDice = [
      { face: 6, big: false },
      { face: 2, big: false },
      { face: 5, big: false },
      { face: 1, big: false },
      { face: 4, big: false },
      { face: 3, big: false },
      { face: 6, big: false },
      { face: 5, big: true },
    ];
    const baseProps = {
      gameId: 'game-1',
      playerId: 'P1',
      connectionState: 'connected',
      onRoll,
      onPlace: vi.fn(),
      onSkip: vi.fn(),
      onToggleAssets: vi.fn(),
      onRefresh: vi.fn(),
      onLeave: vi.fn(),
    };

    try {
      const { container, rerender } = render(<LasVegasGameView {...baseProps} view={waitingView} />);
      fireEvent.click(screen.getByRole('button', { name: 'Roll 8 dice' }));

      expect(onRoll).toHaveBeenCalledOnce();
      expect(screen.getByRole('dialog', { name: 'Rolling the table' })).toBeVisible();
      expect(container.querySelectorAll('.vegas-roll-die-3d')).toHaveLength(8);
      expect(container.querySelectorAll('.vegas-roll-cube__face')).toHaveLength(48);
      expect(screen.queryByRole('button', { name: 'Hide dialog' })).not.toBeInTheDocument();

      const choiceView = { ...waitingView, phase: 'WAITING_FOR_CHOICE', currentRoll: resultDice, stateVersion: waitingView.stateVersion + 1 };
      rerender(<LasVegasGameView {...baseProps} view={choiceView} />);
      expect(screen.queryByRole('button', { name: 'Place all 6s' })).not.toBeInTheDocument();

      await act(async () => { vi.advanceTimersByTime(1999); });
      expect(container.querySelectorAll('[data-result-face]')).toHaveLength(0);
      expect(screen.getByText('0/8 dice locked')).toBeVisible();

      await act(async () => { vi.advanceTimersByTime(1); });
      expect(container.querySelectorAll('[data-result-face]')).toHaveLength(1);
      expect(screen.getByText('1/8 dice locked')).toBeVisible();

      await act(async () => { vi.advanceTimersByTime(999); });
      expect(container.querySelectorAll('[data-result-face]')).toHaveLength(1);

      await act(async () => { vi.advanceTimersByTime(1); });
      expect(container.querySelectorAll('[data-result-face]')).toHaveLength(2);
      expect(screen.getByText('2/8 dice locked')).toBeVisible();

      await act(async () => { vi.advanceTimersByTime(6220); });
      await act(async () => { vi.advanceTimersByTime(20); });

      const completedDialog = screen.getByRole('dialog', { name: 'Roll complete // choose a casino' });
      expect(within(completedDialog).getByRole('button', { name: 'Place all 6s' })).toBeEnabled();
      expect(within(completedDialog).getByRole('button', { name: 'Place all 1s' })).toHaveFocus();
      expect(within(completedDialog).getByRole('button', { name: 'Hide dialog' })).toBeEnabled();
      expect(completedDialog.querySelectorAll('[data-result-face]')).toHaveLength(8);
      expect(container.querySelector('.vegas-current-roll')).not.toBeInTheDocument();

      fireEvent.click(within(completedDialog).getByRole('button', { name: 'Hide dialog' }));
      await act(async () => { vi.advanceTimersByTime(20); });
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Show roll & actions' })).toHaveFocus();

      fireEvent.click(screen.getByRole('button', { name: 'Show roll & actions' }));
      const reopenedDialog = screen.getByRole('dialog', { name: 'Roll complete // choose a casino' });
      expect(reopenedDialog.querySelectorAll('[data-result-face]')).toHaveLength(8);
      fireEvent.click(within(reopenedDialog).getByRole('button', { name: 'Place all 6s' }));
      expect(baseProps.onPlace).toHaveBeenCalledWith(6);
      expect(within(reopenedDialog).getByRole('button', { name: 'Place all 1s' })).toBeDisabled();

      rerender(<LasVegasGameView {...baseProps} view={choiceView} sending />);
      rerender(<LasVegasGameView {...baseProps} view={choiceView} error="The casino rejected this action." />);
      const retryDialog = screen.getByRole('dialog');
      expect(retryDialog).toBeVisible();
      expect(within(retryDialog).getByText('The casino rejected this action.')).toBeVisible();
      expect(screen.getByRole('button', { name: 'Place all 6s' })).toBeEnabled();

      fireEvent.click(screen.getByRole('button', { name: 'Place all 6s' }));
      rerender(<LasVegasGameView
        {...baseProps}
        view={{ ...choiceView, phase: 'WAITING_FOR_ROLL', currentPlayerId: 'P2', currentRoll: [] }}
      />);
      expect(screen.getByRole('dialog')).toBeVisible();
      rerender(<LasVegasGameView
        {...baseProps}
        view={{ ...choiceView, phase: 'WAITING_FOR_ROLL', currentPlayerId: 'P2', currentRoll: [], stateVersion: choiceView.stateVersion + 1 }}
      />);
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      await act(async () => { vi.advanceTimersByTime(20); });
      expect(screen.getByRole('heading', { name: 'Table locked' })).toHaveFocus();
    } finally {
      vi.useRealTimers();
    }
  });

  it('opens a keyboard-accessible casino dialog with every player die and influence', async () => {
    const crowdedPlacements = lasVegasFixture.players.slice(0, 5).map((player, index) => ({
      playerId: player.playerId,
      regularDice: index + 1,
      bigDie: index % 2 === 0,
      influence: index + 1 + (index % 2 === 0 ? 2 : 0),
    }));
    const view = {
      ...lasVegasFixture,
      casinos: lasVegasFixture.casinos.map((casino) => casino.number === 1
        ? { ...casino, placements: crowdedPlacements }
        : casino),
    };
    const { container } = render(
      <LasVegasGameView
        gameId="game-1"
        view={view}
        playerId="P1"
        connectionState="connected"
        onPlace={vi.fn()}
        onSkip={vi.fn()}
        onToggleAssets={vi.fn()}
        onRefresh={vi.fn()}
        onLeave={vi.fn()}
      />,
    );

    const casino = screen.getByRole('button', { name: 'Open Casino 1 details, 5 players' });
    expect(casino.querySelectorAll('.vegas-influence')).toHaveLength(1);
    expect(within(casino).getByText('+4 more players // open details')).toBeVisible();

    casino.focus();
    fireEvent.keyDown(casino, { key: 'Enter' });

    const dialog = screen.getByRole('dialog', { name: 'Casino 1 details' });
    expect(within(dialog).getAllByRole('img')).toHaveLength(18);
    expect(within(dialog).getAllByText(/^Power /)).toHaveLength(5);
    expect(within(dialog).getByText('P5 Player 5')).toBeVisible();
    expect(container.querySelectorAll('.vegas-casino-detail__player')).toHaveLength(5);

    fireEvent.click(within(dialog).getByRole('button', { name: 'Close dialog' }));
    await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Casino 1 details' })).not.toBeInTheDocument());
    expect(casino).toHaveFocus();
  });

  it('hides actions outside the viewer turn and disables Skip when chips are exhausted', () => {
    const otherTurnView = {
      ...lasVegasFixture,
      currentPlayerId: 'P2',
      players: lasVegasFixture.players.map((player) => ({
        ...player,
        current: player.playerId === 'P2',
        chips: player.playerId === 'P1' ? 0 : player.chips,
      })),
    };
    const baseProps = {
      sessionId: 'VEGAS-ROOM',
      gameId: 'game-1',
      playerId: 'P1',
      connectionState: 'connected',
      onPlace: vi.fn(),
      onSkip: vi.fn(),
      onToggleAssets: vi.fn(),
      onRefresh: vi.fn(),
      onLeave: vi.fn(),
    };
    const { rerender } = render(
      <LasVegasGameView
        {...baseProps}
        view={otherTurnView}
      />,
    );

    expect(screen.queryByRole('button', { name: 'Show roll & actions' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Place all 1s' })).not.toBeInTheDocument();
    expect(screen.getByText(/Player 2 is taking their turn/i)).toBeVisible();

    const noChipView = {
      ...lasVegasFixture,
      players: lasVegasFixture.players.map((player) => ({
        ...player,
        chips: player.playerId === 'P1' ? 0 : player.chips,
      })),
    };
    rerender(<LasVegasGameView {...baseProps} view={noChipView} />);
    fireEvent.click(screen.getByRole('button', { name: 'Show roll & actions' }));
    expect(screen.getByRole('button', { name: 'Place all 1s' })).toBeEnabled();
    expect(screen.getByRole('button', { name: 'Spend 1 chip to skip' })).toBeDisabled();
  });

  it('labels a Las Vegas bot turn as CPU and keeps human actions locked', () => {
    const view = {
      ...lasVegasFixture,
      currentPlayerId: 'BOT1',
      players: lasVegasFixture.players.map((player) => ({
        ...player,
        current: player.playerId === 'BOT1',
      })),
    };
    render(
      <LasVegasGameView
        sessionId="VEGAS-ROOM"
        gameId="game-1"
        view={view}
        playerId="P1"
        connectionState="connected"
        onPlace={vi.fn()}
        onSkip={vi.fn()}
        onToggleAssets={vi.fn()}
        onRefresh={vi.fn()}
        onLeave={vi.fn()}
      />,
    );

    expect(screen.getByText('CPU')).toBeVisible();
    expect(screen.getByText(/Bot 1 \(CPU\) is taking their turn/i)).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Place all 1s' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Show roll & actions' })).not.toBeInTheDocument();
  });

  it('closes a failed roll reveal and restores the Roll action', () => {
    const waitingView = { ...lasVegasFixture, phase: 'WAITING_FOR_ROLL', currentRoll: [] };
    const baseProps = {
      gameId: 'game-1',
      view: waitingView,
      playerId: 'P1',
      connectionState: 'connected',
      onRoll: vi.fn(),
      onToggleAssets: vi.fn(),
      onRefresh: vi.fn(),
      onLeave: vi.fn(),
    };
    const { rerender } = render(<LasVegasGameView {...baseProps} />);

    fireEvent.click(screen.getByRole('button', { name: 'Roll 6 dice' }));
    expect(screen.getByRole('dialog', { name: 'Rolling the table' })).toBeVisible();
    rerender(<LasVegasGameView {...baseProps} sending />);
    rerender(<LasVegasGameView {...baseProps} error="The casino rejected this roll." />);

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Roll 6 dice' })).toBeEnabled();
  });

  it('shows Las Vegas roll, reconnecting, busy, and error states', () => {
    const onRoll = vi.fn();
    render(
      <LasVegasGameView
        sessionId="VEGAS-ROOM"
        gameId="game-1"
        view={{ ...lasVegasFixture, phase: 'WAITING_FOR_ROLL', currentRoll: [] }}
        playerId="P1"
        connectionState="reconnecting"
        sending
        error="The table advanced elsewhere."
        onRoll={onRoll}
        onToggleAssets={vi.fn()}
        onRefresh={vi.fn()}
        onLeave={vi.fn()}
      />,
    );

    expect(screen.getByText('Reconnecting')).toBeVisible();
    expect(screen.getByRole('alert')).toHaveTextContent('table advanced elsewhere');
    expect(screen.getByRole('button', { name: /Roll 6 dice/i })).toBeDisabled();
  });

  it('renders shared winners and every final Las Vegas result', () => {
    const results = lasVegasFixture.players.map((player, index) => ({
      playerId: player.playerId,
      name: player.name,
      rank: index < 2 ? 1 : index + 1,
      cashTotal: index < 2 ? 200_000 : 190_000 - index * 10_000,
      chips: 2,
      totalAssets: index < 2 ? 220_000 : 210_000 - index * 10_000,
      tieBreakCount: index < 2 ? 5 : 4,
      winner: index < 2,
    }));
    render(
      <LasVegasGameView
        sessionId="VEGAS-ROOM"
        gameId="game-1"
        view={{ ...lasVegasFixture, phase: 'FINISHED', currentRoll: [], results }}
        playerId="P1"
        connectionState="connected"
        onRefresh={vi.fn()}
        onLeave={vi.fn()}
        onSummary={vi.fn()}
      />,
    );

    expect(screen.getAllByText('#1 WIN')).toHaveLength(2);
    expect(screen.getAllByText('Bot 1').length).toBeGreaterThan(0);
    expect(screen.getByRole('button', { name: 'Open final scoreboard' })).toBeEnabled();
  });
});
