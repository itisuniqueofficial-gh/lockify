# Lockify Official Website

Official website for the **Lockify** Android app — built with Jekyll and hosted on GitHub Pages.

**Live URL:** https://lockify.itisuniqueofficial.com/

---

## What This Is

A complete static website for the Lockify Android app, including:
- Home, About, Features, Download pages
- Changelog and Updates pages
- FAQ, Privacy Policy, Terms, Contact pages
- 404 page
- SEO meta tags, Open Graph, sitemap, robots.txt
- PWA manifest

---

## Folder Structure

```
web-app/
├── _config.yml          # Jekyll site config
├── _layouts/
│   └── default.html     # Base HTML layout
├── _includes/
│   ├── header.html      # Site header / navbar
│   └── footer.html      # Site footer
├── assets/
│   ├── css/main.css     # All styles
│   ├── js/main.js       # Nav, FAQ accordion, animations
│   ├── icons/           # Favicon + PWA icons (add your own)
│   └── images/          # OG image + screenshots (add your own)
├── index.html           # Home page
├── about.html
├── features.html
├── download.html
├── changelog.html
├── updates.html
├── faq.html
├── privacy-policy.html
├── terms.html
├── contact.html
├── 404.html
├── robots.txt
├── sitemap.xml
├── manifest.json
└── Gemfile
```

---

## How to Deploy on GitHub Pages

1. Push the `web-app/` folder contents to a GitHub repository (or a `gh-pages` branch).
2. Go to **Settings → Pages** in your GitHub repo.
3. Set source to the branch/folder containing these files.
4. Set a custom domain if needed (`lockify.itisuniqueofficial.com`).
5. GitHub Pages will build with Jekyll automatically.

---

## How to Update the Play Store Link

Search for `YOUR_PLAY_STORE_LINK_HERE` in:
- `index.html`
- `features.html`
- `download.html`

Replace with your actual Play Store URL:
```
https://play.google.com/store/apps/details?id=com.itisuniqueofficial.lockify
```

---

## How to Update the App Version

1. Open `_config.yml`
2. Update `brand.version: "1.0.0"` to the new version
3. Update `download.html` — the version info card
4. Add a new entry to `changelog.html`
5. Add a new card to `updates.html`
6. Update `sitemap.xml` lastmod dates if needed

---

## How to Add a Changelog Entry

Open `changelog.html` and add a new `<div class="changelog-entry">` block before the existing ones:

```html
<div class="changelog-entry fade-in">
  <div class="changelog-version">v1.1.0 — Feature Update</div>
  <div class="changelog-date">Month Year · Stable</div>
  <div class="changelog-list">
    <div class="changelog-item" data-type="added">
      <span class="changelog-tag tag-added">Added</span>
      Your new feature description
    </div>
    <div class="changelog-item" data-type="fixed">
      <span class="changelog-tag tag-fixed">Fixed</span>
      Bug fix description
    </div>
  </div>
</div>
```

Available tag types: `tag-added`, `tag-improved`, `tag-fixed`, `tag-security`, `tag-performance`

---

## How to Add an Update/News Post

Open `updates.html` and add a new `<article class="update-card">` at the top of the grid:

```html
<article class="update-card fade-in">
  <div class="update-meta">
    <span class="update-badge badge-release">Release</span>
    <span class="update-date">Month Year</span>
  </div>
  <h2 class="update-title">Your Update Title</h2>
  <p class="update-desc">Description of the update.</p>
  <a href="/changelog" class="btn btn-outline btn-sm mt-16">View Changelog →</a>
</article>
```

Badge types: `badge-release`, `badge-feature`, `badge-security`, `badge-fix`

---

## How to Customize Branding

- **Colors:** Edit CSS variables at the top of `assets/css/main.css` (`:root` block)
- **Logo:** Update the emoji/text in `_includes/header.html` and `_includes/footer.html`
- **Icons:** Replace files in `assets/icons/` with your own PNG icons
- **OG Image:** Replace `assets/images/og-image.png` (1200×630px recommended)

---

## Local Development

```bash
cd web-app
bundle install
bundle exec jekyll serve
# Open http://localhost:4000
```

Requires Ruby and Bundler installed.

---

## Contact Form Activation

The contact form in `contact.html` is static by default. To enable submissions:

1. Sign up at [Formspree](https://formspree.io)
2. Create a form and get your endpoint URL
3. Replace `action="#"` in `contact.html` with `action="https://formspree.io/f/YOUR_ID"`
4. Remove the static note below the submit button
