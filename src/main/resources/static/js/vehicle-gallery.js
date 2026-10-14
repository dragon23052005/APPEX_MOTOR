(() => {
    'use strict';

    class VehicleGallery {
        constructor(root) {
            this.root = root;
            this.shell = root.querySelector('[data-gallery-shell]');
            this.stage = root.querySelector('[data-gallery-stage]');
            this.canvas = root.querySelector('[data-gallery-canvas]');
            this.layers = [...root.querySelectorAll('[data-gallery-layer]')];
            this.dialog = root.querySelector('[data-gallery-dialog]');
            this.filmstrip = root.querySelector('[data-gallery-filmstrip]');
            this.items = [...root.querySelectorAll('[data-gallery-item]')].map(node => ({ ...node.dataset, node }));
            if (!this.items.length) return;
            this.state = { category: 'all', id: this.items[0].id, direction: 1 };
            this.desired = this.state;
            this.layerIndex = 0;
            this.cache = new Map();
            this.pendingLoads = new Set();
            this.abort = new AbortController();
            this.motion = window.matchMedia('(prefers-reduced-motion: reduce)');
            this.zoom = 1;
            this.pan = { x: 0, y: 0 };
            this.busy = false;
            this.destroyed = false;
            const listen = (target, type, handler, options = {}) => target.addEventListener(type, handler,
                { ...options, signal: this.abort.signal });

            root.querySelectorAll('[data-gallery-controls]').forEach(node => { node.hidden = false; });
            root.querySelector('[data-gallery-hint]').textContent = 'Arrow keys or swipe to browse · + / − to zoom';
            this.items.forEach(item => {
                item.node.setAttribute('role', 'button');
                item.node.removeAttribute('target');
                listen(item.node, 'click', event => {
                    // Keep modified clicks as normal links to the untouched original photograph.
                    if (event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
                    event.preventDefault();
                    this.select(item.id);
                });
                listen(item.node, 'keydown', event => {
                    if (event.key === ' ') { event.preventDefault(); this.select(item.id); }
                });
            });
            listen(root, 'click', event => {
                const filter = event.target.closest('button[data-category]');
                if (filter) {
                    const list = this.filtered(filter.dataset.category);
                    const keep = list.find(item => item.id === this.state.id) || list[0];
                    this.request({ category: filter.dataset.category, id: keep.id, direction: 1 });
                }
                const action = event.target.closest('button[data-action]')?.dataset.action;
                if (action === 'prev') this.navigate(-1);
                if (action === 'next') this.navigate(1);
                if (action === 'zoom-in') this.setZoom(this.zoom + .25);
                if (action === 'zoom-out') this.setZoom(this.zoom - .25);
                if (action === 'fullscreen') this.openDialog();
                if (action === 'close') this.dialog.close();
                if (action === 'retry') this.request(this.failedTarget || this.state, true);
            });
            listen(this.shell, 'keydown', event => {
                if (event.ctrlKey || event.metaKey || event.altKey || /INPUT|TEXTAREA|SELECT/.test(event.target.tagName)) return;
                if (event.key === 'ArrowLeft') this.navigate(-1);
                else if (event.key === 'ArrowRight') this.navigate(1);
                else if (event.key === '+' || event.key === '=') this.setZoom(this.zoom + .25);
                else if (event.key === '-') this.setZoom(this.zoom - .25);
                else if (event.key === '0') this.setZoom(1);
                else if (event.key === 'Escape' && !this.dialog.open) this.setZoom(1);
                else return;
                event.preventDefault();
            });
            listen(this.stage, 'pointerdown', event => {
                if (!event.isPrimary || event.button !== 0 || event.target.closest('button')) { this.pointer = null; return; }
                this.pointer = { id: event.pointerId, x: event.clientX, y: event.clientY, pan: { ...this.pan } };
                this.stage.setPointerCapture(event.pointerId);
            });
            listen(this.stage, 'pointermove', event => {
                if (this.pointer?.id !== event.pointerId || this.zoom === 1) return;
                this.pan = { x: this.pointer.pan.x + event.clientX - this.pointer.x,
                    y: this.pointer.pan.y + event.clientY - this.pointer.y };
                this.applyPan();
            });
            listen(this.stage, 'pointerup', event => {
                if (this.pointer?.id !== event.pointerId) return;
                const dx = event.clientX - this.pointer.x;
                const dy = event.clientY - this.pointer.y;
                this.pointer = null;
                if (this.zoom === 1 && Math.abs(dx) > 50 && Math.abs(dx) > Math.abs(dy) * 1.4) this.navigate(dx < 0 ? 1 : -1);
            });
            listen(this.stage, 'pointercancel', () => { this.pointer = null; });
            listen(this.stage, 'lostpointercapture', () => { this.pointer = null; });
            listen(this.dialog, 'close', () => this.closeDialog());
            listen(window, 'resize', () => { this.applyPan(); this.revealSelection(); }, { passive: true });
            listen(this.motion, 'change', () => { if (this.motion.matches) this.finishTransition?.(); });
            listen(document, 'visibilitychange', () => { if (document.hidden) this.finishTransition?.(); });
            listen(window, 'pagehide', event => { if (!event.persisted) this.destroy(); });

            this.renderState(false);
            // Defer full-resolution neighbors until the below-fold gallery is near the viewport.
            if ('IntersectionObserver' in window) {
                this.observer = new IntersectionObserver(entries => {
                    if (entries.some(entry => entry.isIntersecting)) { this.observer.disconnect(); this.initializeImage(); }
                }, { rootMargin: '300px' });
                this.observer.observe(this.stage);
            } else this.initializeImage();
        }

        initializeImage() {
            if (this.initialized) return;
            this.initialized = true;
            this.request(this.desired, true);
        }

        filtered(category) {
            return this.items.filter(item => category === 'all' || item.category === category);
        }

        select(id) {
            const list = this.filtered(this.desired.category);
            if (!list.some(item => item.id === id)) return;
            const direction = list.findIndex(item => item.id === id) >= list.findIndex(item => item.id === this.desired.id) ? 1 : -1;
            this.request({ ...this.desired, id, direction });
        }

        navigate(delta) {
            const list = this.filtered(this.desired.category);
            const index = list.findIndex(item => item.id === this.desired.id);
            const next = Math.max(0, Math.min(list.length - 1, index + delta));
            this.request({ ...this.desired, id: list[next].id, direction: delta });
        }

        request(target, reload = false) {
            if (this.destroyed) return;
            this.initialized = true;
            this.observer?.disconnect();
            this.desired = { ...target, reload };
            this.root.querySelector('[data-gallery-error]').hidden = true;
            this.drain();
        }

        async drain() {
            if (this.busy) return;
            this.busy = true;
            this.stage.setAttribute('aria-busy', 'true');
            const loader = this.root.querySelector('[data-gallery-loading]');
            const loadingTimer = window.setTimeout(() => { loader.hidden = false; }, 180);
            try {
                while (!this.destroyed && (this.desired !== this.state)) {
                    const target = this.desired;
                    const item = this.items.find(entry => entry.id === target.id);
                    try {
                        await this.load(item);
                        if (this.destroyed) break;
                        if (target !== this.desired) continue; // Coalesce obsolete requests before animating.
                        if (target.id !== this.state.id || target.reload) await this.transition(item, target.direction);
                        if (this.destroyed) break;
                        this.state = target;
                        this.failedTarget = null;
                        this.renderState();
                    } catch (error) {
                        if (this.destroyed) break;
                        if (target !== this.desired) continue;
                        this.failedTarget = target;
                        this.desired = this.state;
                        this.root.querySelector('[data-gallery-error]').hidden = false;
                        // Leave image, category, counter and thumbnails on the last successful selection.
                        break;
                    }
                }
            } finally {
                window.clearTimeout(loadingTimer);
                loader.hidden = true;
                this.stage.setAttribute('aria-busy', 'false');
                this.busy = false;
                if (!this.destroyed) this.preloadNeighbors();
            }
        }

        load(item) {
            if (this.cache.has(item.id)) {
                const cached = this.cache.get(item.id);
                this.cache.delete(item.id);
                this.cache.set(item.id, cached);
                return cached;
            }
            const promise = new Promise((resolve, reject) => {
                const image = new Image();
                image.decoding = 'async';
                let settled = false;
                const finish = error => {
                    if (settled) return;
                    settled = true;
                    clearTimeout(timer);
                    image.onload = image.onerror = null;
                    this.pendingLoads.delete(cancel);
                    if (error) { image.src = ''; reject(error); } else resolve(image);
                };
                const cancel = () => finish(new Error('Gallery disposed'));
                const timer = setTimeout(() => finish(new Error('Image load timeout')), 15000);
                this.pendingLoads.add(cancel);
                image.onload = async () => {
                    try {
                        if (image.decode) await image.decode();
                        if (!image.naturalWidth) throw new Error('Empty image');
                        finish();
                    } catch (error) { finish(error); }
                };
                image.onerror = () => finish(new Error('Image unavailable'));
                image.src = item.imageUrl;
            });
            this.cache.set(item.id, promise);
            promise.catch(() => { if (this.cache.get(item.id) === promise) this.cache.delete(item.id); });
            // Bound retained decoded images. Never hold every full-size frame in JS memory.
            while (this.cache.size > 5) this.cache.delete(this.cache.keys().next().value);
            return promise;
        }

        async transition(item, direction) {
            this.setZoom(1);
            const current = this.layers[this.layerIndex];
            const nextIndex = 1 - this.layerIndex;
            const next = this.layers[nextIndex];
            next.src = item.imageUrl;
            next.alt = `${item.title}. ${item.description}`;
            next.width = Number(item.width);
            next.height = Number(item.height);
            // Decode the actual display element too, not just the preloader.
            if (next.decode) await next.decode();
            if (this.destroyed) return;
            next.hidden = false;
            current.style.zIndex = '0';
            next.style.zIndex = '1';
            next.style.opacity = '0';
            next.setAttribute('aria-hidden', 'true');
            const previous = this.items.find(entry => entry.id === this.state.id);
            const changedCategory = previous.category !== item.category;
            const focus = item.category === 'details' && previous.category === 'exterior';
            const reveal = item.transition === 'reveal' || (previous.category === 'interior' && item.category === 'exterior');
            const move = !changedCategory && item.category === 'exterior' && item.transition === 'walkaround' ? 10 * direction : 0;
            const scale = focus ? 1.025 : reveal ? 1.02 : item.category === 'interior' ? 1.004 : 1.012;
            const duration = reveal ? .7 : item.category === 'interior' ? .5 : .6;
            if (!this.motion.matches && !document.hidden) {
                await new Promise(resolve => {
                    if (window.gsap) {
                        const timeline = window.gsap.timeline({ onComplete: resolve });
                        this.finishTransition = () => timeline.progress(1);
                        timeline.fromTo(next, { opacity: 0, x: move, scale },
                            { opacity: 1, x: 0, scale: 1, duration, ease: 'power2.inOut' }, 0);
                        // Current layer remains fully opaque underneath: no mid-fade black flash.
                    } else if (next.animate) {
                        const animation = next.animate([
                            { opacity: 0, transform: `translateX(${move}px) scale(${scale})` },
                            { opacity: 1, transform: 'translateX(0) scale(1)' }
                        ], { duration: duration * 1000, easing: 'ease-in-out', fill: 'forwards' });
                        this.finishTransition = () => animation.finish();
                        animation.finished.catch(() => {}).then(() => {
                            next.style.opacity = '1';
                            animation.cancel();
                            resolve();
                        });
                    } else resolve();
                });
            }
            this.finishTransition = null;
            current.hidden = true;
            current.setAttribute('aria-hidden', 'true');
            next.style.opacity = '1';
            next.style.transform = 'none';
            next.removeAttribute('aria-hidden');
            this.layerIndex = nextIndex;
        }

        renderState(scroll = true) {
            const list = this.filtered(this.state.category);
            const index = list.findIndex(item => item.id === this.state.id);
            const item = list[index];
            const setText = (selector, value) => { this.root.querySelector(selector).textContent = value; };
            setText('[data-gallery-title]', item.title);
            setText('[data-gallery-description]', item.description);
            setText('[data-gallery-category]', item.categoryLabel);
            setText('[data-gallery-index]', String(index + 1).padStart(2, '0'));
            setText('[data-gallery-count]', String(list.length).padStart(2, '0'));
            this.root.querySelectorAll('button[data-category]').forEach(node => {
                node.setAttribute('aria-pressed', String(node.dataset.category === this.state.category));
            });
            this.items.forEach(entry => {
                entry.node.hidden = !list.includes(entry);
                entry.node.setAttribute('aria-current', String(entry.id === item.id));
                entry.node.querySelector('span').textContent = String(list.indexOf(entry) + 1).padStart(2, '0');
                if (!entry.node.hidden) entry.node.setAttribute('aria-label', `${list.indexOf(entry) + 1}. ${entry.title}`);
            });
            this.root.querySelector('[data-action="prev"]').disabled = index === 0;
            this.root.querySelector('[data-action="next"]').disabled = index === list.length - 1;
            const progress = this.root.querySelector('[data-gallery-progress]');
            progress.setAttribute('aria-valuenow', String(index + 1));
            progress.setAttribute('aria-valuemax', String(list.length));
            progress.setAttribute('aria-valuetext', `Image ${index + 1} of ${list.length}`);
            this.root.querySelector('[data-gallery-progress-bar]').style.width = `${(index + 1) / list.length * 100}%`;
            if (scroll) this.revealSelection();
        }

        revealSelection() {
            // Scroll the filmstrip only, including after a resize or moving into the lightbox.
            const item = this.items.find(entry => entry.id === this.state.id);
            const strip = this.filmstrip.getBoundingClientRect();
            const thumb = item.node.getBoundingClientRect();
            if (thumb.left < strip.left || thumb.right > strip.right) {
                this.filmstrip.scrollTo({ left: this.filmstrip.scrollLeft + thumb.left - strip.left - (strip.width - thumb.width) / 2,
                    behavior: this.motion.matches ? 'instant' : 'smooth' });
            }
        }

        preloadNeighbors() {
            const list = this.filtered(this.state.category);
            const index = list.findIndex(item => item.id === this.state.id);
            [list[index - 1], list[index + 1]].filter(Boolean).forEach(item => this.load(item).catch(() => {}));
        }

        setZoom(value) {
            this.zoom = Math.max(1, Math.min(2.5, value));
            if (this.zoom === 1) this.pan = { x: 0, y: 0 };
            this.applyPan();
            this.stage.classList.toggle('is-zoomed', this.zoom > 1);
            this.root.querySelector('[data-zoom-label]').textContent = `${Math.round(this.zoom * 100)}%`;
            this.root.querySelector('[data-action="zoom-out"]').disabled = this.zoom === 1;
            this.root.querySelector('[data-action="zoom-in"]').disabled = this.zoom === 2.5;
        }

        applyPan() {
            const maxX = this.stage.clientWidth * (this.zoom - 1) / 2;
            const maxY = this.stage.clientHeight * (this.zoom - 1) / 2;
            this.pan.x = Math.max(-maxX, Math.min(maxX, this.pan.x));
            this.pan.y = Math.max(-maxY, Math.min(maxY, this.pan.y));
            this.canvas.style.transform = `translate(${this.pan.x}px, ${this.pan.y}px) scale(${this.zoom})`;
        }

        openDialog() {
            if (this.dialog.open) return;
            this.restoreFocus = document.activeElement;
            this.savedOverflow = document.body.style.overflow;
            // A native modal lightbox fills the viewport, including mobile browsers without Fullscreen API.
            this.dialog.append(this.shell);
            this.dialog.showModal();
            document.body.style.overflow = 'hidden';
            this.root.querySelector('[data-action="fullscreen"]').hidden = true;
            const close = this.root.querySelector('[data-action="close"]');
            close.hidden = false;
            close.focus({ preventScroll: true });
            this.applyPan();
            this.revealSelection();
        }

        closeDialog() {
            this.root.querySelector('[data-gallery-home]').append(this.shell);
            document.body.style.overflow = this.savedOverflow ?? '';
            this.root.querySelector('[data-action="fullscreen"]').hidden = false;
            this.root.querySelector('[data-action="close"]').hidden = true;
            this.setZoom(1);
            this.revealSelection();
            this.restoreFocus?.focus({ preventScroll: true });
        }

        destroy() {
            this.destroyed = true;
            this.finishTransition?.();
            if (this.dialog.open) { this.dialog.close(); this.closeDialog(); }
            this.abort.abort();
            this.observer?.disconnect();
            this.pendingLoads.forEach(cancel => cancel());
            this.cache.clear();
        }
    }

    document.querySelectorAll('[data-vehicle-gallery]').forEach(root => new VehicleGallery(root));
})();
