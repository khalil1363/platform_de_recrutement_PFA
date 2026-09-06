"""Upscale report images below print-friendly width (2400 px)."""
from pathlib import Path

from PIL import Image

IMG_DIR = Path(__file__).resolve().parent / "img"
MIN_WIDTH = 2400

# ASCII filenames referenced from LaTeX (avoid duplicate French-named copies)
NAMES = [
    "uc-global-hires.jpg",
    "gant.png",
    "arch-three-tiers.png",
    "arch-logic.png",
    "arch-physical.png",
    "uc-sprint1.png",
    "class-sprint1.png",
    "seq-login.png",
    "seq-profile.png",
    "seq-add-user.png",
    "ui-login.png",
    "ui-users.png",
    "ui-profile.png",
    "uc-sprint2.png",
    "class-sprint2.png",
    "seq-add-zone.png",
    "seq-add-company.png",
    "seq-assign-rh.png",
    "ui-zones.png",
    "ui-companies-1.png",
    "ui-companies-2.png",
    "ui-rh-assign.png",
    "uc-sprint3.png",
    "class-sprint3.png",
    "seq-create-offer.png",
    "seq-apply.png",
    "seq-process-app.png",
    "ui-rh-recruitments.png",
    "ui-rh-recruitment-form.png",
    "ui-jobs.png",
    "ui-apply.png",
    "ui-rh-candidates.png",
    "uc-sprint4.jpg",
    "class-sprint4.jpg",
    "seq-online-interview.jpg",
    "seq-hire.jpg",
    "seq-dashboard.jpg",
    "ui-rh-decisions.png",
    "ui-interview-modal.png",
    "ui-calendar.png",
    "ui-hire-modal.png",
    "ui-dashboard.png",
    "ui-candidate-interview.png",
]


def upscale(path: Path) -> None:
    with Image.open(path) as im:
        im.load()
        if im.width >= MIN_WIDTH:
            print(f"OK  {path.name}: {im.width}x{im.height}")
            return
        ratio = MIN_WIDTH / im.width
        new_size = (MIN_WIDTH, max(1, round(im.height * ratio)))
        resized = im.resize(new_size, Image.Resampling.LANCZOS)
        if path.suffix.lower() in {".jpg", ".jpeg"}:
            resized.save(path, quality=95, optimize=True)
        else:
            resized.save(path, optimize=True)
        print(f"UP  {path.name}: {im.width}x{im.height} -> {new_size[0]}x{new_size[1]}")


def main() -> None:
    for name in NAMES:
        path = IMG_DIR / name
        if not path.exists():
            print(f"MISSING {name}")
            continue
        upscale(path)


if __name__ == "__main__":
    main()
