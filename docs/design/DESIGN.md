# Identity: Design System

_Status: **draft concept (M0)**. Will be refined and implemented as a Compose theme in M1. Changes are recorded in [../PROJECT_LOG.md](../PROJECT_LOG.md)._

## 1. Brand idea
**Calm, confident, private.** Identity should feel like a steady friend, not a warning sign. No shame language, no aggressive red, no "church-generic" imagery.

## 2. The mark: the I-cross
A clean capital **I** whose crossbar sits about a third of the way down and is slightly wider than the stem. It reads as a confident "I" first and a cross second.

- **App icon:** parchment stem on ink navy, crossbar in dawn amber. See [brand/identity-icon-concept.svg](brand/identity-icon-concept.svg).
- **Wordmark:** the mark as the first letter, followed by "dentity" in Inter Medium. See [brand/identity-wordmark-concept.svg](brand/identity-wordmark-concept.svg). The final version will convert the text to outlines so it doesn't depend on installed fonts.
- **Small sizes:** at 24 dp and below, the crossbar thickens slightly so it doesn't disappear.
- **Don't:** add rays, halos or doves; tilt the mark; use the amber crossbar on amber.

## 3. Colour tokens

| Token | Light theme | Dark theme | Use |
|---|---|---|---|
| `background` | `#F5F0E6` parchment | `#121826` ink navy | App background |
| `surface` | `#FFFCF6` | `#1B2333` | Cards, sheets |
| `onBackground` / `onSurface` | `#121826` | `#F5F0E6` | Main text |
| `muted` | `#5B6475` | `#A3AABA` | Secondary text |
| `accent` (dawn) | `#B07A35` | `#D9A15B` | Primary buttons, highlights, Scripture First |
| `protected` (sage) | `#3F7D5A` | `#7FBF98` | Protection active |
| `attention` (amber) | `#A86B12` | `#E3B05C` | Needs attention (e.g. Always-on VPN off) |
| `interrupted` (brick) | `#A4483C` | `#E08A7E` | Protection interrupted; calm, not alarming |

Every text/background pair must reach WCAG AA contrast (4.5:1 for body text). This will be verified with a contrast check when the theme is coded in M1. The light accent is darker than the dark-theme accent for that reason.

## 4. Typography
- **Interface:** Inter (SIL Open Font Licence).
- **Scripture reading:** Literata (SIL OFL), a serif designed for long-form screen reading.
- Both are **bundled in the app** (no runtime download from Google Fonts).
- Scale (sp): Display 32 · Title 22 · Body 16 · Scripture body 19 (user-adjustable 16–28) · Label 14 · Caption 12.
- Text respects the system font-size setting.

## 5. Spacing and shape
- 4 dp base grid. Common steps: 4, 8, 12, 16, 24, 32.
- Screen side padding 16 dp (20 dp on wide phones).
- Corner radius: 12 dp cards, 20 dp buttons and sheets.
- Touch targets ≥ 48 dp.

## 6. Navigation
Bottom bar with four tabs: **Home** (protection status) · **Scripture** · **Partner** · **Settings**.
"I'm struggling" is always one tap away: a prominent card on Home, an action on Scripture, and a launcher long-press shortcut.

## 7. Voice and copy rules
- Describe what was **observed**, not what we assume: "Protection was interrupted at 21:40 (cause unknown)", never "You bypassed protection".
- Scripture is offered as support, never as a consequence.
- Explain each permission before Android asks for it.
- Say plainly what Identity can't do (see [architecture §5](../architecture.md#5-what-identity-can-and-cannot-enforce)).

## 8. States every screen must design for
Loading · empty · error (with retry) · permission needed · offline · protection interrupted.
