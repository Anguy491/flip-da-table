import { render, screen } from '@testing-library/react';
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

  it('renders ten distinguishable Las Vegas seats and only legal face actions', () => {
    const onPlace = vi.fn();
    render(
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
    expect(screen.getByRole('button', { name: 'Place all 1s' })).toBeEnabled();
    expect(screen.getByRole('button', { name: 'Place all 3s' })).toBeEnabled();
    expect(screen.queryByRole('button', { name: 'Place all 2s' })).not.toBeInTheDocument();
    screen.getByRole('button', { name: 'Place all 5s' }).click();
    expect(onPlace).toHaveBeenCalledWith(5);
    expect(screen.getAllByRole('img', { name: /big die worth two/i }).length).toBeGreaterThan(0);
    expect(screen.getByText('Revealed $210,000')).toBeVisible();
  });

  it('disables Las Vegas actions outside the viewer turn and when chips are exhausted', () => {
    const view = {
      ...lasVegasFixture,
      currentPlayerId: 'P2',
      players: lasVegasFixture.players.map((player) => ({
        ...player,
        current: player.playerId === 'P2',
        chips: player.playerId === 'P1' ? 0 : player.chips,
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

    expect(screen.getByRole('button', { name: 'Place all 1s' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Spend 1 chip to skip' })).toBeDisabled();
    expect(screen.getByText(/Player 2 is taking their turn/i)).toBeVisible();
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
    expect(screen.getByRole('button', { name: 'Place all 1s' })).toBeDisabled();
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
