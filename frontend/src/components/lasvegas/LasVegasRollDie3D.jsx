import * as Motion from 'motion/react-m';

const FACE_PIPS = {
  1: [4],
  2: [0, 8],
  3: [0, 4, 8],
  4: [0, 2, 6, 8],
  5: [0, 2, 4, 6, 8],
  6: [0, 2, 3, 5, 6, 8],
};

const RESULT_ROTATIONS = {
  1: { rotateX: -12, rotateY: 18 },
  2: { rotateX: -102, rotateY: 18 },
  3: { rotateX: -12, rotateY: -72 },
  4: { rotateX: -12, rotateY: 108 },
  5: { rotateX: 78, rotateY: 18 },
  6: { rotateX: -12, rotateY: 198 },
};

const FACE_POSITIONS = [
  { face: 1, position: 'front' },
  { face: 6, position: 'back' },
  { face: 3, position: 'right' },
  { face: 4, position: 'left' },
  { face: 2, position: 'top' },
  { face: 5, position: 'bottom' },
];

function DieFace({ face, position }) {
  const pips = FACE_PIPS[face];
  return (
    <span className={`vegas-roll-cube__face vegas-roll-cube__face--${position}`} data-cube-face={face}>
      {Array.from({ length: 9 }, (_, index) => (
        <span className="vegas-roll-cube__pip-cell" key={index}>
          {pips.includes(index) && <span className="vegas-roll-cube__pip" />}
        </span>
      ))}
    </span>
  );
}

function resultRotationFor(face, spinSeed = 1) {
  const target = RESULT_ROTATIONS[face] || RESULT_ROTATIONS[1];
  const turns = 3 + Math.abs(spinSeed % 3);
  const xDirection = spinSeed % 2 === 0 ? 1 : -1;
  const yDirection = spinSeed % 3 === 0 ? -1 : 1;
  const zDirection = spinSeed % 5 === 0 ? -1 : 1;
  return {
    rotateX: target.rotateX + turns * 360 * xDirection,
    rotateY: target.rotateY + (turns + 1) * 360 * yDirection,
    rotateZ: turns * 360 * zDirection,
  };
}

export default function LasVegasRollDie3D({
  face,
  big = false,
  seatIndex = 0,
  settled = false,
  reducedMotion = false,
  spinSeed = 1,
}) {
  const target = resultRotationFor(face, spinSeed);
  const label = settled && face
    ? `${big ? 'Big die' : 'Die'} showing ${face}`
    : 'Rolling die';

  return (
    <span
      className={`vegas-roll-die-3d ${big ? 'vegas-roll-die-3d--big' : ''}`}
      data-seat={seatIndex + 1}
      data-result-face={settled && face ? face : undefined}
      role="img"
      aria-label={label}
    >
      <Motion.span
        className="vegas-roll-cube"
        aria-hidden="true"
        initial={reducedMotion ? false : { rotateX: -12, rotateY: 18, rotateZ: 0 }}
        animate={reducedMotion
          ? (settled ? target : { rotateX: -12, rotateY: 18, rotateZ: 0 })
          : settled
            ? target
            : {
              rotateX: [-12, spinSeed % 2 === 0 ? 708 : -732],
              rotateY: [18, spinSeed % 3 === 0 ? -1062 : 1098],
              rotateZ: [0, spinSeed % 5 === 0 ? -360 : 360],
            }}
        transition={settled
          ? { duration: reducedMotion ? 0 : 0.22, ease: [0.2, 0.86, 0.24, 1] }
          : { duration: 0.62, ease: 'linear', repeat: reducedMotion ? 0 : Infinity }}
      >
        {FACE_POSITIONS.map(({ face: cubeFace, position }) => (
          <DieFace face={cubeFace} position={position} key={cubeFace} />
        ))}
      </Motion.span>
    </span>
  );
}
