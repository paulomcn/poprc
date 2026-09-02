// Run with page.evaluate(inspectResponsiveLayout) or the browser tool's read-only evaluate.
export function inspectResponsiveLayout() {
  const rect = (element) => {
    const { x, y, width, height, right, bottom } = element.getBoundingClientRect();
    return { x, y, width, height, right, bottom };
  };
  const visible = (element) => element.getClientRects().length > 0 && getComputedStyle(element).visibility !== 'hidden';
  const header = document.querySelector('[data-app-header]') || document.querySelector('#root header');
  const layoutMain = document.querySelector('[data-layout-main]');
  const dialogs = [...document.querySelectorAll('[role="dialog"]')].filter(visible);
  const legacy = [...document.querySelectorAll('main .fixed')]
    .filter((element) => element.className.includes('bg-black') && visible(element))
    .map((element) => element.firstElementChild);
  return {
    viewport: { width: innerWidth, height: innerHeight },
    documentWidth: document.documentElement.scrollWidth,
    main: layoutMain ? { width: layoutMain.clientWidth, scrollWidth: layoutMain.scrollWidth } : null,
    header: header ? rect(header) : null,
    dialogs: [...new Set([...dialogs, ...legacy])].map(rect),
    brokenImages: [...document.images].filter((element) => visible(element) && element.complete && !element.naturalWidth)
      .map((element) => element.alt || element.src),
  };
}

export function responsiveFailures(snapshot, { appHeader = true } = {}) {
  const failures = [];
  const { width, height } = snapshot.viewport;
  if (snapshot.documentWidth > width + 1) failures.push('Horizontal page overflow');
  if (snapshot.main && snapshot.main.scrollWidth > snapshot.main.width + 1) failures.push('Horizontal main overflow');
  if (appHeader && snapshot.header?.height < 56) failures.push('Application header shorter than 56px');
  for (const dialog of snapshot.dialogs) {
    if (dialog.x < -1 || dialog.y < -1 || dialog.right > width + 1 || dialog.bottom > height + 1) {
      failures.push('Dialog outside viewport');
    }
  }
  if (snapshot.brokenImages.length) failures.push('Broken visible images');
  return failures;
}
