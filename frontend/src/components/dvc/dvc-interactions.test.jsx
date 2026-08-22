import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { CardStrip } from './CardStrip';
import DvcGameOverModal from './GameOverModal';

describe('DVC keyboard and completion interactions', () => {
  it('moves initial rack tiles with the arrow keys', () => {
    const onReorder = vi.fn();
    render(
      <CardStrip
        cards={[
          { color: 'BLACK', value: 2 },
          { color: 'WHITE', value: 0 },
        ]}
        draggable
        onReorder={onReorder}
      />,
    );

    fireEvent.keyDown(screen.getByRole('button', { name: /Move tile 2/ }), { key: 'ArrowLeft' });
    expect(onReorder).toHaveBeenCalledWith(1, 0);
  });

  it('offers the session summary after a completed game', () => {
    const onSummary = vi.fn();
    render(
      <DvcGameOverModal
        open
        winnerName="PixelHost"
        turns={12}
        onClose={vi.fn()}
        onSummary={onSummary}
      />,
    );

    fireEvent.click(screen.getByRole('button', { name: 'View summary' }));
    expect(onSummary).toHaveBeenCalledOnce();
  });

});
