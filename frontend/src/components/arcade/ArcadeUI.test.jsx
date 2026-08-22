import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { ArcadeButton, ArcadeDialog, ArcadeInput, ConnectionBadge } from './ArcadeUI';

describe('Arcade UI primitives', () => {
  it('keeps a loading button labelled and disabled', () => {
    render(<ArcadeButton loading>Save player</ArcadeButton>);
    const button = screen.getByRole('button', { name: /save player/i });
    expect(button).toBeDisabled();
    expect(button).toHaveAttribute('aria-busy', 'true');
  });

  it('labels input errors for assistive technology', () => {
    render(<ArcadeInput label="Invite code" error="Code not found" />);
    const input = screen.getByRole('textbox', { name: /invite code/i });
    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(input).toHaveAccessibleDescription('Code not found');
  });

  it('focuses and closes a dismissible dialog with Escape', () => {
    const onClose = vi.fn();
    render(<ArcadeDialog open title="Join room" onClose={onClose}><ArcadeInput label="Code" /></ArcadeDialog>);
    expect(screen.getByRole('textbox', { name: 'Code' })).toHaveFocus();
    fireEvent.keyDown(document, { key: 'Escape' });
    expect(onClose).toHaveBeenCalledOnce();
  });

  it('exposes connection state as text, not color alone', () => {
    render(<ConnectionBadge state="reconnecting" />);
    expect(screen.getByText('Reconnecting')).toBeVisible();
  });
});
