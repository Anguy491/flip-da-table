import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import UnoGameView from './uno/UnoGameView';
import DvcGameView from './dvc/DvcGameView';
import ChooseColorModal from './uno/ChooseColorModal';
import { dvcFixture, unoFixture } from '../dev/fixtures';

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
});
