(() => {
    'use strict';

    const SELECTOR = '[data-image-sequence][data-viewer-enabled="true"]';
    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    class ImageSequenceViewer {
        constructor(root) {
            this.root = root;
            this.stage = root.querySelector('[data-sequence-stage]');
            this.frameImage = root.querySelector('[data-sequence-frame]');
            this.loader = root.querySelector('[data-sequence-loader]');
            this.progress = root.querySelector('[data-sequence-progress]');
            this.progressBar = root.querySelector('[data-sequence-progress-bar]');
            this.toolbar = root.querySelector('[data-sequence-toolbar]');
            this.errorPanel = root.querySelector('[data-sequence-error]');
            this.status = root.querySelector('[data-sequence-status]');
            this.toggleButton = root.querySelector('[data-sequence-toggle]');
            this.playIcon = root.querySelector('[data-icon-play]');
            this.pauseIcon = root.querySelector('[data-icon-pause]');
            this.frameCount = Number.parseInt(root.dataset.frameCount || '0', 10);
            this.pattern = root.dataset.framePattern || '';
            this.vehicleName = root.dataset.vehicleName || 'Vehicle';
            this.autoRotateRequested = root.dataset.autoRotate === 'true' && !reduceMotion;
            this.cache = new Map();
            this.pending = new Map();
            this.currentFrame = 0;
            this.zoom = 1;
            this.userPlaying = this.autoRotateRequested;
            this.inViewport = true;
            this.pointerPositions = new Map();
            this.dragOriginX = 0;
            this.dragOriginFrame = 0;
            this.pinchOriginDistance = 0;
            this.pinchOriginZoom = 1;
            this.autoTimer = null;
            this.idleHandle = null;
            this.renderToken = 0;
            this.abortController = new AbortController();
            this.observer = null;
        }

        async init() {
            if (!this.isValidConfiguration()) {
                this.fail('The 360° image metadata is incomplete.');
                return;
            }

            this.bindEvents();
            this.observeVisibility();

            await this.prepareFrames();
        }

        async prepareFrames() {
            try {
                const initialFrames = this.uniqueIndices([0, 1, 2, this.frameCount - 1, this.frameCount - 2]);
                await Promise.all(initialFrames.map((index) => this.loadFrame(index)));
                await this.showFrame(0, false);
                this.setReady();
                this.preloadNearby(0);
                this.syncAutoRotation();
            } catch (error) {
                this.fail('The 360° images could not be loaded.');
            }
        }

        isValidConfiguration() {
            return this.frameCount >= 2 && this.pattern.includes('{frame}') && this.stage && this.frameImage;
        }

        frameUrl(index) {
            const frameNumber = String(this.wrapIndex(index) + 1).padStart(3, '0');
            return this.pattern.replace('{frame}', frameNumber);
        }

        wrapIndex(index) {
            return ((index % this.frameCount) + this.frameCount) % this.frameCount;
        }

        uniqueIndices(indices) {
            return [...new Set(indices.map((index) => this.wrapIndex(index)))];
        }

        loadFrame(index) {
            const normalized = this.wrapIndex(index);
            if (this.cache.has(normalized)) {
                return Promise.resolve(this.cache.get(normalized));
            }
            if (this.pending.has(normalized)) {
                return this.pending.get(normalized);
            }

            const request = new Promise((resolve, reject) => {
                const image = new Image();
                image.decoding = 'async';
                image.onload = async () => {
                    try {
                        if (typeof image.decode === 'function') {
                            await image.decode();
                        }
                    } catch (_) {
                        // The image is already usable after onload even when decode() is unavailable.
                    }
                    this.pending.delete(normalized);
                    this.cache.set(normalized, image);
                    this.updateProgress();
                    resolve(image);
                };
                image.onerror = () => {
                    this.pending.delete(normalized);
                    reject(new Error(`Unable to load frame ${normalized + 1}`));
                };
                image.src = this.frameUrl(normalized);
            });

            this.pending.set(normalized, request);
            return request;
        }

        async showFrame(index, announce = true) {
            const normalized = this.wrapIndex(index);
            const renderToken = ++this.renderToken;
            try {
                const image = await this.loadFrame(normalized);
                if (renderToken !== this.renderToken) {
                    return;
                }
                this.currentFrame = normalized;
                this.frameImage.src = image.src;
                this.frameImage.alt = `${this.vehicleName}, 360 degree view, angle ${normalized + 1} of ${this.frameCount}`;
                if (announce) {
                    this.status.textContent = `Angle ${normalized + 1} of ${this.frameCount}`;
                }
                this.preloadNearby(normalized);
            } catch (_) {
                this.fail('A required 360° image is unavailable.');
            }
        }

        preloadNearby(index) {
            const neighbours = [];
            for (let distance = 1; distance <= 3; distance += 1) {
                neighbours.push(index + distance, index - distance);
            }
            this.uniqueIndices(neighbours).forEach((frame) => {
                this.loadFrame(frame).catch(() => {});
            });

            const schedule = window.requestIdleCallback || ((callback) => window.setTimeout(callback, 180));
            if (this.idleHandle !== null) {
                const cancel = window.cancelIdleCallback || window.clearTimeout;
                cancel(this.idleHandle);
            }
            this.idleHandle = schedule(() => {
                const directionFrames = [index + 4, index + 5, index + 6];
                this.uniqueIndices(directionFrames).forEach((frame) => {
                    this.loadFrame(frame).catch(() => {});
                });
                this.idleHandle = null;
            });
        }

        updateProgress() {
            const percentage = Math.round((this.cache.size / this.frameCount) * 100);
            if (this.progress) {
                this.progress.textContent = `${percentage}%`;
            }
            if (this.progressBar) {
                this.progressBar.style.transform = `scaleX(${percentage / 100})`;
            }
        }

        setReady() {
            this.root.classList.add('is-sequence-ready');
            if (this.loader) {
                this.loader.hidden = true;
            }
            if (this.toolbar) {
                this.toolbar.hidden = false;
            }
            this.status.textContent = `Interactive 360 degree view ready. Angle 1 of ${this.frameCount}.`;
            this.updateToggleState();
        }

        fail(message) {
            this.stopAutoRotation();
            this.root.classList.add('has-sequence-error');
            if (this.loader) {
                this.loader.hidden = true;
            }
            if (this.toolbar) {
                this.toolbar.hidden = true;
            }
            if (this.errorPanel) {
                this.errorPanel.hidden = false;
            }
            this.status.textContent = `${message} Showing the studio photograph instead.`;
        }

        async retry() {
            this.root.classList.remove('has-sequence-error');
            if (this.errorPanel) {
                this.errorPanel.hidden = true;
            }
            if (this.loader) {
                this.loader.hidden = false;
            }
            this.cache.clear();
            this.pending.clear();
            this.updateProgress();
            await this.prepareFrames();
        }

        bindEvents() {
            const signal = this.abortController.signal;
            const listen = (target, type, handler, options = {}) => {
                target?.addEventListener(type, handler, { ...options, signal });
            };

            listen(this.stage, 'pointerdown', (event) => this.onPointerDown(event));
            listen(this.stage, 'pointermove', (event) => this.onPointerMove(event));
            listen(this.stage, 'pointerup', (event) => this.onPointerUp(event));
            listen(this.stage, 'pointercancel', (event) => this.onPointerUp(event));
            listen(this.stage, 'dragstart', (event) => event.preventDefault());
            listen(this.root, 'keydown', (event) => this.onKeyDown(event));
            listen(this.toggleButton, 'click', () => this.toggleAutoRotation());
            listen(this.root.querySelector('[data-sequence-zoom-in]'), 'click', () => this.setZoom(this.zoom + .15));
            listen(this.root.querySelector('[data-sequence-zoom-out]'), 'click', () => this.setZoom(this.zoom - .15));
            listen(this.root.querySelector('[data-sequence-reset]'), 'click', () => this.reset());
            listen(this.root.querySelector('[data-sequence-fullscreen]'), 'click', () => this.toggleFullscreen());
            listen(this.root.querySelector('[data-sequence-retry]'), 'click', () => this.retry());
            listen(document, 'visibilitychange', () => this.syncAutoRotation());
            listen(document, 'fullscreenchange', () => this.syncFullscreenLabel());
            listen(window, 'pagehide', () => this.dispose(), { once: true });
        }

        onPointerDown(event) {
            this.stage.setPointerCapture?.(event.pointerId);
            this.pointerPositions.set(event.pointerId, { x: event.clientX, y: event.clientY });
            if (this.pointerPositions.size === 1) {
                this.dragOriginX = event.clientX;
                this.dragOriginFrame = this.currentFrame;
                this.root.classList.add('is-sequence-dragging');
            } else if (this.pointerPositions.size === 2) {
                this.pinchOriginDistance = this.pointerDistance();
                this.pinchOriginZoom = this.zoom;
            }
            this.stopAutoRotation();
        }

        onPointerMove(event) {
            if (!this.pointerPositions.has(event.pointerId)) {
                return;
            }
            this.pointerPositions.set(event.pointerId, { x: event.clientX, y: event.clientY });

            if (this.pointerPositions.size === 2) {
                const distance = this.pointerDistance();
                if (this.pinchOriginDistance > 0) {
                    this.setZoom(this.pinchOriginZoom * (distance / this.pinchOriginDistance));
                }
                return;
            }

            const pixelsPerFrame = Math.max(7, this.stage.clientWidth / this.frameCount / 1.8);
            const frameDelta = Math.round((event.clientX - this.dragOriginX) / pixelsPerFrame);
            this.showFrame(this.dragOriginFrame - frameDelta, false);
        }

        onPointerUp(event) {
            this.pointerPositions.delete(event.pointerId);
            if (this.pointerPositions.size === 0) {
                this.root.classList.remove('is-sequence-dragging');
                this.status.textContent = `Angle ${this.currentFrame + 1} of ${this.frameCount}`;
                this.syncAutoRotation();
            } else if (this.pointerPositions.size === 1) {
                const remaining = [...this.pointerPositions.values()][0];
                this.dragOriginX = remaining.x;
                this.dragOriginFrame = this.currentFrame;
            }
        }

        pointerDistance() {
            const [first, second] = [...this.pointerPositions.values()];
            return Math.hypot(second.x - first.x, second.y - first.y);
        }

        onKeyDown(event) {
            switch (event.key) {
                case 'ArrowLeft':
                    event.preventDefault();
                    this.showFrame(this.currentFrame - 1);
                    break;
                case 'ArrowRight':
                    event.preventDefault();
                    this.showFrame(this.currentFrame + 1);
                    break;
                case '+':
                case '=':
                    event.preventDefault();
                    this.setZoom(this.zoom + .15);
                    break;
                case '-':
                case '_':
                    event.preventDefault();
                    this.setZoom(this.zoom - .15);
                    break;
                case ' ':
                    event.preventDefault();
                    this.toggleAutoRotation();
                    break;
                case 'Home':
                    event.preventDefault();
                    this.reset();
                    break;
                case 'f':
                case 'F':
                    event.preventDefault();
                    this.toggleFullscreen();
                    break;
                default:
                    break;
            }
        }

        setZoom(value) {
            this.zoom = Math.min(2.2, Math.max(1, value));
            this.frameImage.style.setProperty('--sequence-zoom', this.zoom.toFixed(2));
            this.status.textContent = `Zoom ${Math.round(this.zoom * 100)} percent`;
        }

        reset() {
            this.setZoom(1);
            this.showFrame(0);
            this.userPlaying = this.autoRotateRequested;
            this.syncAutoRotation();
            this.root.classList.remove('is-sequence-dragging');
        }

        toggleAutoRotation() {
            this.userPlaying = !this.userPlaying;
            this.syncAutoRotation();
        }

        syncAutoRotation() {
            const shouldPlay = this.userPlaying && this.inViewport && !document.hidden && !reduceMotion;
            if (shouldPlay) {
                this.startAutoRotation();
            } else {
                this.stopAutoRotation();
            }
            this.updateToggleState();
        }

        startAutoRotation() {
            if (this.autoTimer !== null) {
                return;
            }
            this.autoTimer = window.setInterval(() => this.showFrame(this.currentFrame + 1, false), 110);
        }

        stopAutoRotation() {
            if (this.autoTimer !== null) {
                window.clearInterval(this.autoTimer);
                this.autoTimer = null;
            }
        }

        updateToggleState() {
            const playing = this.autoTimer !== null;
            this.toggleButton?.setAttribute('aria-label', playing ? 'Pause automatic rotation' : 'Start automatic rotation');
            if (this.playIcon) {
                this.playIcon.hidden = playing;
            }
            if (this.pauseIcon) {
                this.pauseIcon.hidden = !playing;
            }
        }

        observeVisibility() {
            if (!('IntersectionObserver' in window)) {
                return;
            }
            this.observer = new IntersectionObserver(([entry]) => {
                this.inViewport = entry.isIntersecting;
                this.syncAutoRotation();
            }, { threshold: .15 });
            this.observer.observe(this.root);
        }

        async toggleFullscreen() {
            try {
                if (document.fullscreenElement === this.root) {
                    await document.exitFullscreen();
                } else if (this.root.requestFullscreen) {
                    await this.root.requestFullscreen();
                }
            } catch (_) {
                this.status.textContent = 'Fullscreen mode is not available in this browser.';
            }
        }

        syncFullscreenLabel() {
            const button = this.root.querySelector('[data-sequence-fullscreen]');
            if (button) {
                button.setAttribute('aria-label', document.fullscreenElement === this.root ? 'Exit fullscreen' : 'Enter fullscreen');
            }
        }

        dispose() {
            this.stopAutoRotation();
            this.abortController.abort();
            this.observer?.disconnect();
            this.cache.clear();
            this.pending.clear();
        }
    }

    document.querySelectorAll(SELECTOR).forEach((root) => {
        const viewer = new ImageSequenceViewer(root);
        viewer.init();
    });
})();
