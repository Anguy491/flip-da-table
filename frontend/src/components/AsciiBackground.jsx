import { useEffect, useRef, useState } from 'react';
import HotBalloonOne from '../assets/HotBalloon_1.txt?raw';
import HotBalloonTwo from '../assets/HotBalloon_2.txt?raw';
import HotBalloonThree from '../assets/HotBalloon_3.txt?raw';

const FONT_STACK =
  'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace';

const normalizeTemplate = (value) => value.replace(/\r\n/g, '\n').replace(/\n+$/, '');

const BALLOON_TEMPLATES = [
  normalizeTemplate(HotBalloonOne),
  normalizeTemplate(HotBalloonTwo),
  normalizeTemplate(HotBalloonThree),
];

const DEFAULTS = {
  density: 0.8,
  opacity: 0.12,
  baseSpeed: 6,
  speedMultiplierNear: 2.2,
  amplitude: 6,
  bobPeriod: 14,
  layers: 3,
  color: '#2e3b55',
  gridUnderlay: false,
};

function hashSeed(value) {
  const str = String(value);
  let h = 2166136261;
  for (let i = 0; i < str.length; i += 1) {
    h ^= str.charCodeAt(i);
    h += (h << 1) + (h << 4) + (h << 7) + (h << 8) + (h << 24);
  }
  return h >>> 0;
}

function mulberry32(seed) {
  let t = seed >>> 0;
  return function rand() {
    t += 0x6d2b79f5;
    let r = Math.imul(t ^ (t >>> 15), t | 1);
    r ^= r + Math.imul(r ^ (r >>> 7), r | 61);
    return ((r ^ (r >>> 14)) >>> 0) / 4294967296;
  };
}

function createRng(seed) {
  return mulberry32(hashSeed(seed));
}

function usePrefersReducedMotion() {
  const [reduced, setReduced] = useState(false);

  useEffect(() => {
    if (typeof window === 'undefined' || !window.matchMedia) {
      return undefined;
    }
    const mediaQuery = window.matchMedia('(prefers-reduced-motion: reduce)');
    const update = () => setReduced(mediaQuery.matches);
    update();

    if (mediaQuery.addEventListener) {
      mediaQuery.addEventListener('change', update);
      return () => mediaQuery.removeEventListener('change', update);
    }

    mediaQuery.addListener(update);
    return () => mediaQuery.removeListener(update);
  }, []);

  return reduced;
}

function buildVariantCache(ctx, templates, fontSize, color) {
  const font = `${fontSize}px ${FONT_STACK}`;
  ctx.font = font;
  const charWidth = ctx.measureText('M').width || fontSize * 0.6;
  const lineHeight = Math.round(fontSize * 1.2);

  return templates.map((template) => {
    const lines = template.split('\n');
    const maxChars = Math.max(...lines.map((line) => line.length));
    const width = Math.max(1, Math.ceil(maxChars * charWidth));
    const height = Math.max(1, Math.ceil(lines.length * lineHeight));
    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    const cctx = canvas.getContext('2d');
    cctx.imageSmoothingEnabled = false;
    cctx.font = font;
    cctx.textBaseline = 'top';
    cctx.fillStyle = color;
    lines.forEach((line, index) => {
      cctx.fillText(line, 0, index * lineHeight);
    });

    return {
      canvas,
      width,
      height,
    };
  });
}

function createGridCanvas(width, height, dpr, color, opacity) {
  const canvas = document.createElement('canvas');
  canvas.width = Math.max(1, Math.floor(width * dpr));
  canvas.height = Math.max(1, Math.floor(height * dpr));
  const ctx = canvas.getContext('2d');
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  ctx.imageSmoothingEnabled = false;
  ctx.strokeStyle = color;
  ctx.globalAlpha = opacity;
  ctx.lineWidth = 1;

  const step = 28;
  for (let x = 0; x <= width; x += step) {
    ctx.beginPath();
    ctx.moveTo(Math.round(x) + 0.5, 0);
    ctx.lineTo(Math.round(x) + 0.5, height);
    ctx.stroke();
  }
  for (let y = 0; y <= height; y += step) {
    ctx.beginPath();
    ctx.moveTo(0, Math.round(y) + 0.5);
    ctx.lineTo(width, Math.round(y) + 0.5);
    ctx.stroke();
  }

  ctx.globalAlpha = 1;
  return canvas;
}

function AsciiBackground({
  seed,
  density = DEFAULTS.density,
  opacity = DEFAULTS.opacity,
  baseSpeed = DEFAULTS.baseSpeed,
  speedMultiplierNear = DEFAULTS.speedMultiplierNear,
  amplitude = DEFAULTS.amplitude,
  bobPeriod = DEFAULTS.bobPeriod,
  layers = DEFAULTS.layers,
  color = DEFAULTS.color,
  gridUnderlay = DEFAULTS.gridUnderlay,
  className = '',
  style = {},
}) {
  const canvasRef = useRef(null);
  const animationRef = useRef(null);
  const lastTimeRef = useRef(null);
  const reducedMotion = usePrefersReducedMotion();

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) {
      return undefined;
    }

    const ctx = canvas.getContext('2d');
    if (!ctx) {
      return undefined;
    }

    let isMounted = true;
    let resizeTimer = null;
    const sceneRef = { current: null };

    const buildScene = () => {
      const width = window.innerWidth || 1;
      const height = window.innerHeight || 1;
      const dpr = window.devicePixelRatio || 1;

      canvas.width = Math.max(1, Math.floor(width * dpr));
      canvas.height = Math.max(1, Math.floor(height * dpr));
      canvas.style.width = `${width}px`;
      canvas.style.height = `${height}px`;
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      ctx.imageSmoothingEnabled = false;
      ctx.textBaseline = 'top';
      ctx.textAlign = 'left';

      const layerCount = Math.max(1, layers);
      const weightSum = (layerCount * (layerCount + 1)) / 2;
      const areaUnit = 80000;
      const baseCount =
        density <= 0 ? 0 : Math.max(1, Math.round((width * height * density) / areaUnit));

      const layerData = [];
      for (let layerIndex = 0; layerIndex < layerCount; layerIndex += 1) {
        const depthT = layerCount === 1 ? 1 : layerIndex / (layerCount - 1);
        const fontSize = Math.round(11 + depthT * 6);
        const variants = buildVariantCache(ctx, BALLOON_TEMPLATES, fontSize, color);
        const layerWeight = (layerCount - layerIndex) / weightSum;
        const count = Math.max(0, Math.round(baseCount * layerWeight));
        const speedScale = 1 + depthT * (speedMultiplierNear - 1);
        const ampScale = 0.4 + depthT * 0.6;
        const layerRng = createRng(`${seed}-layer-${layerIndex}`);
        const items = [];

        for (let i = 0; i < count; i += 1) {
          const variantIndex = Math.floor(layerRng() * variants.length);
          const variant = variants[variantIndex];
          const x = layerRng() * (width + 200) - 200;
          const yMax = Math.max(0, height - variant.height);
          const y0 = layerRng() * yMax;
          const phase = layerRng() * Math.PI * 2;
          const speedJitter = 0.85 + layerRng() * 0.3;
          const ampJitter = 0.8 + layerRng() * 0.4;

          items.push({
            x,
            y0,
            phase,
            speed: baseSpeed * speedScale * speedJitter,
            amp: amplitude * ampScale * ampJitter,
            variantIndex,
            width: variant.width,
            height: variant.height,
          });
        }

        const sparkleCount = layerIndex === 0 ? Math.max(0, Math.round(count * 0.2)) : 0;
        const sparkles = [];
        for (let i = 0; i < sparkleCount; i += 1) {
          sparkles.push({
            x: layerRng() * width,
            y: layerRng() * height,
            speed: baseSpeed * 0.35 * (0.8 + layerRng() * 0.4),
          });
        }

        layerData.push({
          variants,
          items,
          sparkles,
          rng: layerRng,
          speedScale,
          ampScale,
        });
      }

      const gridCanvas = gridUnderlay
        ? createGridCanvas(width, height, dpr, color, opacity * 0.25)
        : null;

      sceneRef.current = {
        width,
        height,
        layers: layerData,
        gridCanvas,
      };
    };

    const resetBalloon = (balloon, layer, width, height) => {
      const variantIndex = Math.floor(layer.rng() * layer.variants.length);
      const variant = layer.variants[variantIndex];
      const yMax = Math.max(0, height - variant.height);
      balloon.x = -variant.width - 40;
      balloon.y0 = layer.rng() * yMax;
      balloon.phase = layer.rng() * Math.PI * 2;
      balloon.speed =
        baseSpeed * layer.speedScale * (0.85 + layer.rng() * 0.3);
      balloon.amp =
        amplitude * layer.ampScale * (0.8 + layer.rng() * 0.4);
      balloon.variantIndex = variantIndex;
      balloon.width = variant.width;
      balloon.height = variant.height;
    };

    // Motion model: steady drift + sinusoidal bob, scaled by depth.
    const drawScene = (timeSec, dt) => {
      const scene = sceneRef.current;
      if (!scene) {
        return;
      }

      const { width, height, layers: layerData, gridCanvas } = scene;
      ctx.clearRect(0, 0, width, height);

      if (gridCanvas) {
        ctx.globalAlpha = 1;
        ctx.drawImage(gridCanvas, 0, 0);
      }

      ctx.globalAlpha = opacity;
      ctx.fillStyle = color;

      layerData.forEach((layer) => {
        if (!reducedMotion) {
          layer.items.forEach((balloon) => {
            balloon.x += balloon.speed * dt;
            if (balloon.x > width + 60) {
              resetBalloon(balloon, layer, width, height);
            }
          });

          layer.sparkles.forEach((sparkle) => {
            sparkle.x += sparkle.speed * dt;
            if (sparkle.x > width + 10) {
              sparkle.x = -10;
              sparkle.y = layer.rng() * height;
            }
          });
        }

        layer.items.forEach((balloon) => {
          const variant = layer.variants[balloon.variantIndex];
          const bob =
            Math.sin((timeSec * (Math.PI * 2)) / bobPeriod + balloon.phase) *
            balloon.amp;
          const x = Math.round(balloon.x);
          const y = Math.round(balloon.y0 + bob);
          ctx.drawImage(variant.canvas, x, y);
        });

        if (layer.sparkles.length) {
          ctx.globalAlpha = opacity * 0.5;
          layer.sparkles.forEach((sparkle) => {
            ctx.fillRect(Math.round(sparkle.x), Math.round(sparkle.y), 1, 1);
          });
          ctx.globalAlpha = opacity;
        }
      });
    };

    const animate = (timestamp) => {
      if (!isMounted) {
        return;
      }
      if (lastTimeRef.current == null) {
        lastTimeRef.current = timestamp;
      }
      const dt = Math.min(0.05, (timestamp - lastTimeRef.current) / 1000);
      lastTimeRef.current = timestamp;
      drawScene(timestamp / 1000, dt);
      animationRef.current = requestAnimationFrame(animate);
    };

    const render = () => {
      if (animationRef.current) {
        cancelAnimationFrame(animationRef.current);
        animationRef.current = null;
      }
      buildScene();
      lastTimeRef.current = null;
      drawScene(0, 0);
      if (!reducedMotion) {
        animationRef.current = requestAnimationFrame(animate);
      }
    };

    const handleResize = () => {
      if (resizeTimer) {
        window.clearTimeout(resizeTimer);
      }
      resizeTimer = window.setTimeout(() => {
        if (isMounted) {
          render();
        }
      }, 150);
    };

    render();
    window.addEventListener('resize', handleResize);

    return () => {
      isMounted = false;
      window.removeEventListener('resize', handleResize);
      if (resizeTimer) {
        window.clearTimeout(resizeTimer);
      }
      if (animationRef.current) {
        cancelAnimationFrame(animationRef.current);
      }
    };
  }, [
    seed,
    density,
    opacity,
    baseSpeed,
    speedMultiplierNear,
    amplitude,
    bobPeriod,
    layers,
    color,
    gridUnderlay,
    reducedMotion,
  ]);

  return (
    <canvas
      ref={canvasRef}
      aria-hidden="true"
      className={className}
      style={{
        position: 'fixed',
        inset: 0,
        zIndex: 0,
        pointerEvents: 'none',
        ...style,
      }}
    />
  );
}

export default AsciiBackground;
