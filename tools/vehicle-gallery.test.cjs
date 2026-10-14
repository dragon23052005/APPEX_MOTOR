/* Dependency-free behavioral tests of the production script with a small DOM/Image adapter.
 * Node is a development test runner only, never the application runtime.
 * This is not a replacement for real browser visual/touch-device testing.
 */
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { setMaxListeners } = require('node:events');
const rootDir = path.resolve(__dirname, '..');
const source = fs.readFileSync(path.join(rootDir, 'src/main/resources/static/js/vehicle-gallery.js'), 'utf8');
const manifest = JSON.parse(fs.readFileSync(path.join(rootDir, 'src/main/resources/galleries/ferrari-sf90-stradale.json'), 'utf8'));

function fixture({ reduced = true, animated = false, noGsap = false } = {}) {
    const nodes = new Map(), failures = new Set(), delays = new Map(), loads = [], animations = [];
    const doc = { hidden: false };
    class Node {
        constructor(dataset = {}) {
            this.dataset = dataset; this.style = {}; this.attrs = {}; this.listeners = {};
            this.hidden = false; this.disabled = false; this.textContent = ''; this.tagName = 'DIV';
            this.clientWidth = 800; this.clientHeight = 500; this.scrollLeft = 0;
            this.classList = { toggle: (name, enabled) => { this[name] = enabled; } };
        }
        addEventListener(type, fn, options = {}) {
            (this.listeners[type] ||= new Set()).add(fn);
            options.signal?.addEventListener('abort', () => this.listeners[type].delete(fn), { once: true });
        }
        fire(type, values = {}) {
            const event = { target: this, preventDefault() {}, ...values };
            for (const fn of this.listeners[type] || []) fn(event);
        }
        setAttribute(name, value) { this.attrs[name] = String(value); }
        removeAttribute(name) { delete this.attrs[name]; }
        querySelector(selector) { return this.children?.get(selector) || nodes.get(selector); }
        querySelectorAll(selector) { return lists[selector] || []; }
        closest(selector) {
            if (selector === 'button' && this.tagName === 'BUTTON') return this;
            if (selector === 'button[data-action]' && this.dataset.action) return this;
            if (selector === 'button[data-category]' && this.tagName === 'BUTTON' && this.dataset.category) return this;
            return null;
        }
        getBoundingClientRect() { return { left: 0, right: this.clientWidth, width: this.clientWidth }; }
        scrollTo(options) { this.scrollLeft = options.left; }
        setPointerCapture() {}
        focus() { doc.activeElement = this; }
        append(child) { child.parent = this; }
        showModal() { this.open = true; }
        close() { this.open = false; this.fire('close'); }
        decode() { return failures.has(this.src) ? Promise.reject(Error('Decode error')) : Promise.resolve(); }
        animate(frames, options) {
            animations.push({ frames, options, fallback: true });
            let finish;
            const finished = new Promise(resolve => { finish = resolve; });
            setTimeout(finish, 8);
            return { finished, finish, cancel() {} };
        }
    }
    const selectors = ['shell', 'stage', 'canvas', 'dialog', 'filmstrip', 'home', 'loading', 'error', 'error-message',
        'hint', 'title', 'description', 'category', 'index', 'count', 'progress', 'progress-bar'];
    for (const key of selectors) nodes.set(`[data-gallery-${key}]`, new Node());
    nodes.set('[data-zoom-label]', new Node());
    for (const action of ['prev', 'next', 'zoom-in', 'zoom-out', 'fullscreen', 'close', 'retry']) {
        const node = new Node({ action }); node.tagName = 'BUTTON'; nodes.set(`[data-action="${action}"]`, node);
    }
    const filters = ['all', 'exterior', 'details', 'interior'].map(category => {
        const node = new Node({ category }); node.tagName = 'BUTTON'; return node;
    });
    const items = manifest.images.map(item => {
        const node = new Node({ ...item, categoryLabel: { exterior: 'Exterior', details: 'Details', interior: 'Interior' }[item.category], transition: item.transitionStyle });
        node.children = new Map([['span', new Node()]]); return node;
    });
    const layers = [new Node(), new Node()];
    layers[0].src = items[0].dataset.imageUrl; layers[1].hidden = true;
    const lists = { '[data-gallery-layer]': layers, '[data-gallery-item]': items,
        '[data-gallery-controls]': [], 'button[data-category]': filters };
    const root = new Node();
    const win = new Node(); win.setTimeout = setTimeout; win.clearTimeout = clearTimeout;
    const motion = new Node(); motion.matches = reduced; win.matchMedia = () => motion;
    if (animated && !noGsap) win.gsap = { timeline({ onComplete }) {
        let done = false;
        const complete = () => { if (!done) { done = true; onComplete(); } };
        return { progress: complete, fromTo(element, from, to) {
            animations.push({ element, from, to, zIndex: element.style.zIndex });
            setTimeout(complete, 12);
        } };
    } };
    Object.assign(doc, new Node());
    doc.addEventListener = Node.prototype.addEventListener;
    doc.fire = Node.prototype.fire;
    doc.querySelectorAll = () => [root];
    doc.body = new Node(); doc.activeElement = nodes.get('[data-action="fullscreen"]');
    class Image {
        constructor() { this.naturalWidth = 1536; }
        set src(url) {
            this.url = url;
            if (!url) return;
            loads.push(url);
            setTimeout(() => failures.has(url) ? this.onerror?.() : this.onload?.(), delays.get(url) || 0);
        }
        decode() { return Promise.resolve(); }
    }
    class Controller extends AbortController { constructor() { super(); setMaxListeners(0, this.signal); } }
    const context = vm.createContext({ window: win, document: doc, Image, AbortController: Controller, setTimeout, clearTimeout,
        IntersectionObserver: class { observe() {} disconnect() {} } });
    // Instrument only the test's in-memory copy to inspect lifecycle/cache bounds.
    vm.runInContext(source.replace('new VehicleGallery(root)', '(globalThis.instance = new VehicleGallery(root))'), context);
    const gallery = context.instance;
    const click = action => root.fire('click', { target: nodes.get(`[data-action="${action}"]`) });
    const filter = category => root.fire('click', { target: filters.find(node => node.dataset.category === category) });
    const idle = async () => {
        for (let i = 0; gallery.busy && i < 400; i++) await new Promise(resolve => setTimeout(resolve, 2));
        assert.equal(gallery.busy, false, 'navigation must settle');
    };
    return { gallery, nodes, layers, root, win, doc, motion, items, filters, failures, delays, loads, animations, click, filter, idle };
}

test('manifest order, category counts and preserved selection', async t => {
    const f = fixture(); t.after(() => f.gallery.destroy()); await f.idle();
    assert.equal(f.gallery.items.length, 24);
    assert.equal(f.gallery.filtered('exterior').length, 13);
    f.filter('interior'); await f.idle();
    assert.equal(f.gallery.state.id, 'cockpit');
    assert.equal(f.nodes.get('[data-gallery-count]').textContent, '05');
    assert.equal(f.items.find(n => n.dataset.id === 'cockpit').attrs['aria-label'], '1. Step inside');
    assert.equal(f.nodes.get('[data-gallery-progress]').attrs['aria-valuetext'], 'Image 1 of 5');
    f.click('next'); await f.idle(); f.filter('all'); await f.idle();
    assert.equal(f.gallery.state.id, 'dashboard');
    assert.equal(f.nodes.get('[data-gallery-index]').textContent, '19');
    assert.equal(f.items.filter(n => n.attrs['aria-current'] === 'true').length, 1);
});

test('rapid navigation coalesces and keeps one layer synchronized', async t => {
    const f = fixture({ reduced: false, animated: true }); t.after(() => f.gallery.destroy()); await f.idle();
    for (let i = 0; i < 15; i++) f.click('next');
    await f.idle();
    assert.equal(f.gallery.state.id, 'wheel');
    assert.equal(f.nodes.get('[data-gallery-index]').textContent, '16');
    assert.equal(f.layers.filter(n => !n.hidden).length, 1);
    assert.equal(f.layers[f.gallery.layerIndex].src, f.gallery.items[15].imageUrl);
    assert.ok(f.animations.every(a => a.zIndex === '1'), 'incoming image must be above outgoing image');
});

test('slow loading keeps prior image visible and discards obsolete selections', async t => {
    const f = fixture(); t.after(() => f.gallery.destroy()); await f.idle();
    f.delays.set(f.gallery.items[17].imageUrl, 25);
    const previous = f.layers[f.gallery.layerIndex].src;
    f.gallery.select('cockpit');
    assert.equal(f.layers[f.gallery.layerIndex].src, previous);
    f.gallery.select('console'); await f.idle();
    assert.equal(f.gallery.state.id, 'console');
    assert.equal(f.nodes.get('[data-gallery-title]').textContent, 'Details within reach');
});

test('failed image retains category/caption/selection; retry recovers', async t => {
    const f = fixture(); t.after(() => f.gallery.destroy()); await f.idle();
    const url = f.gallery.items.find(i => i.id === 'cockpit').imageUrl;
    f.failures.add(url); f.filter('interior'); await f.idle();
    assert.equal(f.gallery.state.id, 'first-impression');
    assert.equal(f.gallery.state.category, 'all');
    assert.equal(f.nodes.get('[data-gallery-error]').hidden, false);
    assert.equal(f.layers.filter(n => !n.hidden).length, 1);
    f.failures.delete(url); f.click('retry'); await f.idle();
    assert.equal(f.gallery.state.id, 'cockpit');
    assert.equal(f.gallery.state.category, 'interior');
    assert.equal(f.nodes.get('[data-gallery-error]').hidden, true);
});

test('keyboard navigation, zoom bounds and reset on navigation', async t => {
    const f = fixture(); t.after(() => f.gallery.destroy()); await f.idle();
    const shell = f.nodes.get('[data-gallery-shell]');
    shell.fire('keydown', { key: 'ArrowRight' }); await f.idle();
    assert.equal(f.gallery.state.id, 'front');
    for (let i = 0; i < 10; i++) shell.fire('keydown', { key: '+' });
    assert.equal(f.gallery.zoom, 2.5);
    shell.fire('keydown', { key: 'ArrowLeft' }); await f.idle();
    assert.equal(f.gallery.zoom, 1);
    assert.equal(f.gallery.state.id, 'first-impression');
    shell.fire('keydown', { key: 'ArrowLeft' }); await f.idle();
    assert.equal(f.gallery.state.id, 'first-impression');
});

test('pointer-touch swipe, vertical intent, cancellation and zoom pan', async t => {
    const f = fixture(); t.after(() => f.gallery.destroy()); await f.idle();
    const stage = f.nodes.get('[data-gallery-stage]');
    const touch = { pointerId: 1, pointerType: 'touch', isPrimary: true, button: 0 };
    stage.fire('pointerdown', { ...touch, clientX: 280, clientY: 100 });
    stage.fire('pointerup', { ...touch, clientX: 80, clientY: 110 }); await f.idle();
    assert.equal(f.gallery.state.id, 'front');
    stage.fire('pointerdown', { ...touch, clientX: 280, clientY: 100 });
    stage.fire('pointerup', { ...touch, clientX: 200, clientY: 300 }); await f.idle();
    assert.equal(f.gallery.state.id, 'front');
    stage.fire('pointerdown', { ...touch, clientX: 280, clientY: 100 }); stage.fire('pointercancel');
    stage.fire('pointerup', { ...touch, clientX: 80, clientY: 110 }); await f.idle();
    assert.equal(f.gallery.state.id, 'front');
    f.click('zoom-in');
    stage.fire('pointerdown', { ...touch, clientX: 100, clientY: 100 });
    stage.fire('pointermove', { ...touch, clientX: 900, clientY: 900 });
    stage.fire('pointerup', { ...touch, clientX: 900, clientY: 900 }); await f.idle();
    assert.equal(f.gallery.state.id, 'front');
    assert.equal(f.gallery.pan.x, 100); assert.equal(f.gallery.pan.y, 62.5);
});

test('lightbox restores focus and scroll state', async t => {
    const f = fixture(); t.after(() => f.gallery.destroy()); await f.idle();
    f.doc.body.style.overflow = 'auto';
    f.click('fullscreen');
    assert.equal(f.nodes.get('[data-gallery-dialog]').open, true);
    assert.equal(f.doc.body.style.overflow, 'hidden');
    f.click('close');
    assert.equal(f.nodes.get('[data-gallery-dialog]').open, false);
    assert.equal(f.doc.body.style.overflow, 'auto');
    assert.equal(f.doc.activeElement, f.nodes.get('[data-action="fullscreen"]'));
});

test('reduced motion skips animations and neighboring loads stay bounded', async t => {
    const f = fixture({ reduced: true, animated: true }); t.after(() => f.gallery.destroy()); await f.idle();
    for (let i = 0; i < 8; i++) { f.click('next'); await f.idle(); }
    assert.equal(f.animations.length, 0);
    assert.ok(f.gallery.cache.size <= 5);
    assert.ok(f.loads.length < 24);
    const currentUrl = f.gallery.items.find(i => i.id === f.gallery.state.id).imageUrl;
    const before = f.loads.filter(url => url === currentUrl).length;
    f.click('prev'); await f.idle(); f.click('next'); await f.idle();
    assert.equal(f.loads.filter(url => url === currentUrl).length, before);
});

test('Web Animations fallback works without GSAP', async t => {
    const f = fixture({ reduced: false, noGsap: true }); t.after(() => f.gallery.destroy()); await f.idle();
    f.click('next'); await f.idle();
    assert.equal(f.gallery.state.id, 'front');
    assert.ok(f.animations.some(a => a.fallback));
});

test('hidden document and changed motion preference skip animations', async t => {
    const f = fixture({ reduced: false, animated: true }); t.after(() => f.gallery.destroy()); await f.idle();
    f.doc.hidden = true; f.doc.fire('visibilitychange');
    const count = f.animations.length; f.click('next'); await f.idle();
    assert.equal(f.animations.length, count);
    f.doc.hidden = false; f.motion.matches = true; f.motion.fire('change');
    f.click('next'); await f.idle(); assert.equal(f.animations.length, count);
});

test('destroy cancels pending loads and clears retained resources', async () => {
    const f = fixture(); await f.idle();
    f.delays.set(f.gallery.items[17].imageUrl, 20); f.gallery.select('cockpit');
    f.gallery.destroy(); await f.idle();
    assert.equal(f.gallery.cache.size, 0);
    assert.equal(f.gallery.pendingLoads.size, 0);
    assert.equal(f.gallery.abort.signal.aborted, true);
});
