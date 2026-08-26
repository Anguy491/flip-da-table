import { useEffect, useMemo, useRef, useState } from 'react';
import { LazyMotion, useReducedMotion } from 'motion/react';
import * as Motion from 'motion/react-m';
import { ArcadeButton, ArcadeDialog, StatusBanner } from '../arcade/ArcadeUI';
import LasVegasRollDie3D from './LasVegasRollDie3D';

const ROLL_REVEAL_TIMING = {
  spinLeadMs: 2000,
  staggerMs: 1000,
  impactMs: 220,
  reducedHoldMs: 100,
};

const loadMotionFeatures = () => import('./motionFeatures').then((module) => module.default);

function seededRandom(seed) {
  let value = seed >>> 0;
  return () => {
    value = (value * 1664525 + 1013904223) >>> 0;
    return value / 4294967296;
  };
}

function SparkCanvas({ burst, disabled }) {
  const canvasRef = useRef(null);
  const particlesRef = useRef([]);
  const frameRef = useRef(null);
  const lastTimeRef = useRef(null);
  const sizeRef = useRef({ width: 0, height: 0, dpr: 1 });

  useEffect(() => {
    if (disabled || typeof CanvasRenderingContext2D === 'undefined') return undefined;
    const canvas = canvasRef.current;
    const stage = canvas?.parentElement;
    if (!canvas || !stage) return undefined;

    const resize = () => {
      const rect = stage.getBoundingClientRect();
      const dpr = Math.min(window.devicePixelRatio || 1, 2);
      sizeRef.current = { width: rect.width, height: rect.height, dpr };
      canvas.width = Math.max(1, Math.round(rect.width * dpr));
      canvas.height = Math.max(1, Math.round(rect.height * dpr));
    };

    resize();
    const observer = typeof ResizeObserver === 'undefined' ? null : new ResizeObserver(resize);
    observer?.observe(stage);
    window.addEventListener('resize', resize);
    return () => {
      observer?.disconnect();
      window.removeEventListener('resize', resize);
    };
  }, [disabled]);

  useEffect(() => {
    if (!burst || disabled || typeof CanvasRenderingContext2D === 'undefined') return undefined;
    const canvas = canvasRef.current;
    const stage = canvas?.parentElement;
    const die = stage?.querySelector(`[data-roll-index="${burst.index}"]`);
    const context = canvas?.getContext('2d');
    if (!canvas || !stage || !die || !context) return undefined;

    const stageRect = stage.getBoundingClientRect();
    const dieRect = die.getBoundingClientRect();
    const originX = dieRect.left - stageRect.left + dieRect.width / 2;
    const originY = dieRect.top - stageRect.top + dieRect.height * 0.72;
    const styles = getComputedStyle(stage);
    const colors = [
      styles.getPropertyValue('--vegas-gold').trim(),
      styles.getPropertyValue('--vegas-cream').trim(),
      styles.getPropertyValue('--vegas-coral').trim(),
    ];
    const random = seededRandom(burst.seed);
    const bornAt = performance.now();
    const particleCount = burst.big ? 18 : 13;

    for (let index = 0; index < particleCount; index += 1) {
      const direction = index % 2 === 0 ? -1 : 1;
      const speed = 90 + random() * 150;
      particlesRef.current.push({
        x: originX + (random() - 0.5) * 12,
        y: originY + (random() - 0.5) * 8,
        vx: direction * speed * (0.55 + random() * 0.65),
        vy: -40 - random() * 150,
        gravity: 290 + random() * 170,
        drag: 0.975 - random() * 0.015,
        length: 5 + random() * (burst.big ? 18 : 13),
        width: 1 + random() * 1.8,
        life: 300 + random() * 330,
        bornAt,
        color: colors[index % colors.length],
      });
    }

    if (frameRef.current != null) return undefined;

    const draw = (time) => {
      const { width, height, dpr } = sizeRef.current;
      const previousTime = lastTimeRef.current ?? time;
      const deltaSeconds = Math.min((time - previousTime) / 1000, 0.034);
      lastTimeRef.current = time;
      context.setTransform(dpr, 0, 0, dpr, 0, 0);
      context.clearRect(0, 0, width, height);
      context.globalCompositeOperation = 'lighter';

      particlesRef.current = particlesRef.current.filter((particle) => {
        const age = time - particle.bornAt;
        if (age >= particle.life) return false;
        particle.vx *= particle.drag;
        particle.vy = particle.vy * particle.drag + particle.gravity * deltaSeconds;
        particle.x += particle.vx * deltaSeconds;
        particle.y += particle.vy * deltaSeconds;
        const opacity = Math.max(0, 1 - age / particle.life);
        const speed = Math.max(1, Math.hypot(particle.vx, particle.vy));
        context.beginPath();
        context.moveTo(particle.x, particle.y);
        context.lineTo(
          particle.x - (particle.vx / speed) * particle.length,
          particle.y - (particle.vy / speed) * particle.length,
        );
        context.strokeStyle = particle.color;
        context.globalAlpha = opacity;
        context.lineWidth = particle.width;
        context.shadowColor = particle.color;
        context.shadowBlur = 7;
        context.stroke();
        return true;
      });

      context.globalAlpha = 1;
      context.shadowBlur = 0;
      context.globalCompositeOperation = 'source-over';
      if (particlesRef.current.length) {
        frameRef.current = requestAnimationFrame(draw);
      } else {
        frameRef.current = null;
        lastTimeRef.current = null;
        context.clearRect(0, 0, width, height);
      }
    };

    frameRef.current = requestAnimationFrame(draw);
    return undefined;
  }, [burst, disabled]);

  useEffect(() => () => {
    if (frameRef.current != null) cancelAnimationFrame(frameRef.current);
    particlesRef.current = [];
  }, []);

  return <canvas ref={canvasRef} className="vegas-roll-sparks" aria-hidden="true" />;
}

export default function RollRevealDialog({
  open,
  rollId,
  phase = 'waiting-result',
  pendingDice = [],
  resultDice,
  seatIndex = 0,
  playerName = 'Player',
  legalFaces = [],
  chips = 0,
  sending = false,
  pendingAction,
  error,
  onRevealComplete,
  onHide,
  onPlace,
  onSkip,
}) {
  const reducedMotion = useReducedMotion();
  const [settledCount, setSettledCount] = useState(0);
  const [burst, setBurst] = useState(null);
  const firstChoiceRef = useRef(null);
  const completionRef = useRef(onRevealComplete);
  const resultReady = Boolean(resultDice?.length);
  const ready = phase === 'ready';
  const displayDice = resultReady ? resultDice : pendingDice;
  const resultSignature = useMemo(
    () => (resultDice || []).map((die) => `${die.face}:${Boolean(die.big)}`).join('|'),
    [resultDice],
  );

  useEffect(() => { completionRef.current = onRevealComplete; }, [onRevealComplete]);

  useEffect(() => {
    if (phase === 'waiting-result') {
      setSettledCount(0);
      setBurst(null);
      return undefined;
    }
    if (!resultReady || ready) {
      if (ready) setSettledCount(resultDice?.length || 0);
      return undefined;
    }

    if (reducedMotion) {
      setSettledCount(resultDice.length);
      const completionTimer = window.setTimeout(
        () => completionRef.current?.(),
        ROLL_REVEAL_TIMING.reducedHoldMs,
      );
      return () => window.clearTimeout(completionTimer);
    }

    setSettledCount(0);
    const timers = resultDice.map((die, index) => window.setTimeout(() => {
      setSettledCount(index + 1);
      setBurst({
        index,
        big: Boolean(die.big),
        seed: (Number(rollId) || 1) * 97 + die.face * 31 + index * 17,
        token: `${rollId}-${index}`,
      });
    }, ROLL_REVEAL_TIMING.spinLeadMs + index * ROLL_REVEAL_TIMING.staggerMs));
    const finishAt = ROLL_REVEAL_TIMING.spinLeadMs
      + Math.max(0, resultDice.length - 1) * ROLL_REVEAL_TIMING.staggerMs
      + ROLL_REVEAL_TIMING.impactMs;
    timers.push(window.setTimeout(() => completionRef.current?.(), finishAt));
    return () => timers.forEach((timer) => window.clearTimeout(timer));
  }, [phase, ready, reducedMotion, resultDice, resultReady, resultSignature, rollId]);

  useEffect(() => {
    if (!open || !ready || !firstChoiceRef.current) return undefined;
    const frame = window.requestAnimationFrame(() => firstChoiceRef.current?.focus());
    return () => window.cancelAnimationFrame(frame);
  }, [open, ready, rollId]);

  return (
    <ArcadeDialog
      open={open}
      wide
      dismissible={ready}
      closeLabel="Hide"
      onClose={ready ? onHide : undefined}
      className="vegas-roll-dialog"
      eyebrow={`${playerName} // all remaining dice`}
      title={ready ? 'Roll complete // choose a casino' : 'Rolling the table'}
    >
      <div className="sr-only" role="status" aria-live="polite">
        {ready
          ? `Roll complete: ${resultDice.map((die) => `${die.big ? 'big ' : ''}${die.face}`).join(', ')}`
          : `Rolling ${displayDice.length} dice`}
      </div>
      <LazyMotion features={loadMotionFeatures} strict>
        <div className="vegas-roll-stage" aria-hidden="true">
          <Motion.div
            className="vegas-roll-wheel"
            animate={reducedMotion
              ? { opacity: 0.55 }
              : resultReady
                ? { rotate: 675, scale: [1, 1.04, 0.98, 1] }
                : { rotate: 360 }}
            transition={resultReady
              ? { duration: 0.72, ease: [0.18, 0.78, 0.22, 1] }
              : { duration: 0.8, ease: 'linear', repeat: Infinity }}
          >
            <span className="vegas-roll-wheel__hub" />
          </Motion.div>

          <div className="vegas-roll-dice" data-count={displayDice.length}>
            {displayDice.map((die, index) => {
              const settled = ready || index < settledCount;
              const spinSeed = (Number(rollId) || 1) * 97 + (die.face || index + 1) * 31 + index * 17;
              return (
                <Motion.span
                  className={`vegas-roll-die-wrap ${settled ? 'vegas-roll-die-wrap--settled' : ''}`}
                  data-roll-index={index}
                  key={`${rollId}-${index}`}
                  initial={reducedMotion ? false : { opacity: 0, scale: 0.72, y: 18 }}
                  animate={reducedMotion
                    ? { opacity: 1, scale: 1, y: 0 }
                    : settled
                      ? {
                        opacity: 1,
                        scale: [1.08, 0.92, 1],
                        y: [0, 7, -2, 0],
                      }
                      : {
                        opacity: 1,
                        scale: [0.94, 1.06, 0.94],
                        y: [0, -14, 0],
                      }}
                  transition={settled
                    ? {
                      opacity: { duration: 0.1 },
                      scale: { duration: 0.22, ease: [0.2, 0.86, 0.24, 1] },
                      y: { duration: 0.22, ease: [0.2, 0.86, 0.24, 1] },
                    }
                    : {
                      opacity: { duration: 0.12 },
                      scale: { duration: 0.5, ease: 'linear', repeat: reducedMotion ? 0 : Infinity },
                      y: { duration: 0.5, ease: 'linear', repeat: reducedMotion ? 0 : Infinity },
                    }}
                >
                  <LasVegasRollDie3D
                    big={die.big}
                    face={die.face}
                    seatIndex={seatIndex}
                    settled={settled}
                    reducedMotion={reducedMotion}
                    spinSeed={spinSeed}
                  />
                </Motion.span>
              );
            })}
          </div>

          <SparkCanvas burst={burst} disabled={reducedMotion} />
          <div className="vegas-roll-friction" />
        </div>
      </LazyMotion>
      <p className="vegas-roll-caption">
        {ready ? `${displayDice.length}/${displayDice.length} dice locked // choose your move`
          : resultReady ? `${settledCount}/${displayDice.length} dice locked`
            : 'The house is reading the roll...'}
      </p>
      {ready && (
        <section className="vegas-roll-controls" aria-label="Roll actions">
          {error && <StatusBanner tone="error" live>{error}</StatusBanner>}
          <div className="vegas-roll-face-actions" aria-label="Legal casino choices">
            {legalFaces.map((face, index) => (
              <ArcadeButton
                key={face}
                ref={index === 0 ? firstChoiceRef : undefined}
                size="small"
                loading={pendingAction === `place-${face}`}
                disabled={sending || Boolean(pendingAction)}
                onClick={() => onPlace?.(face)}
              >
                Place all {face}s
              </ArcadeButton>
            ))}
          </div>
          <ArcadeButton
            block
            size="small"
            variant="secondary"
            loading={pendingAction === 'skip'}
            disabled={sending || Boolean(pendingAction) || !chips}
            onClick={onSkip}
          >
            Spend 1 chip to skip
          </ArcadeButton>
        </section>
      )}
    </ArcadeDialog>
  );
}
