/* Lockify – Main JS */
'use strict';

// Mobile nav toggle
(function () {
  const toggle = document.getElementById('navToggle');
  const nav = document.getElementById('mainNav');
  if (!toggle || !nav) return;

  toggle.addEventListener('click', function () {
    const open = nav.classList.toggle('open');
    toggle.setAttribute('aria-expanded', open);
  });

  // Close on outside click
  document.addEventListener('click', function (e) {
    if (!nav.contains(e.target) && !toggle.contains(e.target)) {
      nav.classList.remove('open');
      toggle.setAttribute('aria-expanded', 'false');
    }
  });

  // Close on Escape
  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') {
      nav.classList.remove('open');
      toggle.setAttribute('aria-expanded', 'false');
      toggle.focus();
    }
  });
})();

// FAQ accordion
(function () {
  const questions = document.querySelectorAll('.faq-question');
  questions.forEach(function (btn) {
    btn.addEventListener('click', function () {
      const expanded = btn.getAttribute('aria-expanded') === 'true';
      // Close all
      questions.forEach(function (q) {
        q.setAttribute('aria-expanded', 'false');
        const ans = document.getElementById(q.getAttribute('aria-controls'));
        if (ans) ans.classList.remove('open');
      });
      // Open clicked if it was closed
      if (!expanded) {
        btn.setAttribute('aria-expanded', 'true');
        const answer = document.getElementById(btn.getAttribute('aria-controls'));
        if (answer) answer.classList.add('open');
      }
    });
  });
})();

// Fade-in on scroll
(function () {
  if (!('IntersectionObserver' in window)) return;
  const els = document.querySelectorAll('.fade-in');
  const observer = new IntersectionObserver(function (entries) {
    entries.forEach(function (entry) {
      if (entry.isIntersecting) {
        entry.target.classList.add('visible');
        observer.unobserve(entry.target);
      }
    });
  }, { threshold: 0.1 });
  els.forEach(function (el) { observer.observe(el); });
})();

// Changelog filter
(function () {
  const filterBtns = document.querySelectorAll('[data-filter]');
  if (!filterBtns.length) return;
  filterBtns.forEach(function (btn) {
    btn.addEventListener('click', function () {
      const filter = btn.getAttribute('data-filter');
      filterBtns.forEach(function (b) { b.classList.remove('active'); });
      btn.classList.add('active');
      const items = document.querySelectorAll('.changelog-item');
      items.forEach(function (item) {
        if (filter === 'all' || item.getAttribute('data-type') === filter) {
          item.style.display = '';
        } else {
          item.style.display = 'none';
        }
      });
    });
  });
})();
