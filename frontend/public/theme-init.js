(function () {
  var preference;
  try { preference = localStorage.getItem('utaped-theme'); } catch { /* Storage may be unavailable. */ }
  document.documentElement.dataset.theme = preference === 'light' || preference === 'dark'
    ? preference : window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
})();
