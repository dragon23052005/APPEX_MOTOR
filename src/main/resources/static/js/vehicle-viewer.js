import * as THREE from 'three';
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';
import { RoomEnvironment } from 'three/addons/environments/RoomEnvironment.js';

const initializedStages = new WeakSet();
const modelRequests = new Map();

function loadModelOnce(url, onProgress) {
    if (modelRequests.has(url)) {
        onProgress?.(1, true);
        return modelRequests.get(url);
    }

    const request = new Promise((resolve, reject) => {
        const loader = new GLTFLoader();
        loader.load(
            url,
            resolve,
            (event) => {
                const progress = event.total > 0 ? event.loaded / event.total : null;
                onProgress?.(progress, false);
            },
            reject
        );
    });

    modelRequests.set(url, request);
    request.catch(() => modelRequests.delete(url));
    return request;
}

function disposeMaterial(material) {
    Object.values(material).forEach((value) => {
        if (value?.isTexture) value.dispose();
    });
    material.dispose();
}

class VehicleViewer {
    constructor(stage) {
        this.stage = stage;
        this.host = stage.querySelector('[data-viewer-canvas]');
        this.loading = stage.querySelector('[data-viewer-loading]');
        this.loadingText = stage.querySelector('[data-viewer-loading-text]');
        this.errorPanel = stage.querySelector('[data-viewer-error]');
        this.errorMessage = stage.querySelector('[data-viewer-error-message]');
        this.toolbar = stage.querySelector('[data-viewer-toolbar]');
        this.hint = stage.querySelector('[data-viewer-hint]');
        this.modelUrl = stage.dataset.modelUrl;
        this.initialYaw = Number(stage.dataset.initialYaw || 0);
        this.reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
        this.abortController = new AbortController();
        this.frameId = null;
        this.inViewport = true;
        this.disposed = false;
        this.model = null;

        this.handleReset = this.handleReset.bind(this);
        this.handleVisibility = this.handleVisibility.bind(this);
        this.handleContextLost = this.handleContextLost.bind(this);
        this.animate = this.animate.bind(this);
    }

    async init() {
        if (!this.host || !this.modelUrl) return;

        try {
            this.createRenderer();
            this.createScene();
            this.bindEvents();
            this.setLoadingProgress(null);

            const gltf = await loadModelOnce(this.modelUrl, (progress, cached) => {
                if (this.disposed) return;
                if (cached) {
                    this.loadingText.textContent = 'Restoring 3D studio';
                } else {
                    this.setLoadingProgress(progress);
                }
            });

            if (this.disposed) return;
            this.mountModel(gltf.scene);
            this.showReadyState();
            this.resize();
            this.startRendering();
        } catch (error) {
            console.error('APEX 3D viewer failed.', error);
            this.showError('Your browser could not open the interactive studio. The studio image remains available.');
        }
    }

    createRenderer() {
        const canvas = document.createElement('canvas');
        canvas.className = 'vehicle-viewer__webgl';
        canvas.setAttribute('aria-hidden', 'true');
        canvas.addEventListener('webglcontextlost', this.handleContextLost, { signal: this.abortController.signal });
        this.host.appendChild(canvas);

        this.renderer = new THREE.WebGLRenderer({
            canvas,
            alpha: true,
            antialias: !window.matchMedia('(max-width: 767px)').matches,
            powerPreference: 'high-performance'
        });
        this.renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, window.innerWidth < 768 ? 1.5 : 2));
        this.renderer.outputColorSpace = THREE.SRGBColorSpace;
        this.renderer.toneMapping = THREE.ACESFilmicToneMapping;
        this.renderer.toneMappingExposure = 1.08;
        this.renderer.shadowMap.enabled = true;
        this.renderer.shadowMap.type = THREE.PCFSoftShadowMap;
    }

    createScene() {
        this.scene = new THREE.Scene();
        this.camera = new THREE.PerspectiveCamera(38, 1, 0.05, 100);
        this.defaultCameraPosition = new THREE.Vector3(6.8, 3.15, 7.8);
        this.defaultTarget = new THREE.Vector3(0, 1.05, 0);
        this.camera.position.copy(this.defaultCameraPosition);

        this.controls = new OrbitControls(this.camera, this.renderer.domElement);
        this.controls.enableDamping = !this.reducedMotion;
        this.controls.dampingFactor = 0.06;
        this.controls.enablePan = false;
        this.controls.minDistance = 4.2;
        this.controls.maxDistance = 12;
        this.controls.minPolarAngle = Math.PI * 0.23;
        this.controls.maxPolarAngle = Math.PI * 0.49;
        this.controls.target.copy(this.defaultTarget);
        this.controls.update();

        const pmrem = new THREE.PMREMGenerator(this.renderer);
        const room = new RoomEnvironment();
        this.environmentTexture = pmrem.fromScene(room, 0.04).texture;
        this.scene.environment = this.environmentTexture;
        pmrem.dispose();
        room.traverse((child) => {
            child.geometry?.dispose();
            if (Array.isArray(child.material)) child.material.forEach(disposeMaterial);
            else if (child.material) disposeMaterial(child.material);
        });

        const ambient = new THREE.HemisphereLight(0xe9f0ff, 0x16100f, 1.25);
        this.scene.add(ambient);

        const key = new THREE.DirectionalLight(0xffffff, 4.2);
        key.position.set(5, 8, 5);
        key.castShadow = true;
        key.shadow.mapSize.set(window.innerWidth < 768 ? 1024 : 2048, window.innerWidth < 768 ? 1024 : 2048);
        key.shadow.camera.near = 0.5;
        key.shadow.camera.far = 30;
        key.shadow.camera.left = -7;
        key.shadow.camera.right = 7;
        key.shadow.camera.top = 7;
        key.shadow.camera.bottom = -7;
        key.shadow.bias = -0.00035;
        this.scene.add(key);

        const fill = new THREE.DirectionalLight(0x8ea4c8, 2.1);
        fill.position.set(-6, 3, -4);
        this.scene.add(fill);

        const rim = new THREE.SpotLight(0xd3131b, 85, 24, Math.PI * 0.2, 0.55, 1.2);
        rim.position.set(-4, 5, 6);
        rim.target.position.set(0, 1, 0);
        this.scene.add(rim, rim.target);

        this.ground = new THREE.Mesh(
            new THREE.PlaneGeometry(24, 24),
            new THREE.ShadowMaterial({ color: 0x000000, opacity: 0.42 })
        );
        this.ground.rotation.x = -Math.PI / 2;
        this.ground.receiveShadow = true;
        this.ground.position.y = -0.012;
        this.scene.add(this.ground);
    }

    bindEvents() {
        const signal = this.abortController.signal;
        this.stage.querySelector('[data-viewer-reset]')?.addEventListener('click', this.handleReset, { signal });
        this.stage.querySelector('[data-viewer-retry]')?.addEventListener('click', () => window.location.reload(), { signal });
        document.addEventListener('visibilitychange', this.handleVisibility, { signal });
        window.addEventListener('pagehide', () => this.dispose(), { once: true, signal });

        this.controls.addEventListener('start', () => this.stage.classList.add('is-viewer-interacting'));
        this.controls.addEventListener('end', () => this.stage.classList.remove('is-viewer-interacting'));

        this.resizeObserver = new ResizeObserver(() => this.resize());
        this.resizeObserver.observe(this.stage);

        this.intersectionObserver = new IntersectionObserver(([entry]) => {
            this.inViewport = entry.isIntersecting;
            if (this.inViewport) this.startRendering();
            else this.stopRendering();
        }, { threshold: 0.02 });
        this.intersectionObserver.observe(this.stage);
    }

    mountModel(scene) {
        this.model = scene;
        this.model.rotation.y = THREE.MathUtils.degToRad(this.initialYaw);

        const originalBox = new THREE.Box3().setFromObject(this.model);
        const originalSize = originalBox.getSize(new THREE.Vector3());
        const maxDimension = Math.max(originalSize.x, originalSize.y, originalSize.z);
        const scale = maxDimension > 0 ? 5.25 / maxDimension : 1;
        this.model.scale.setScalar(scale);

        const box = new THREE.Box3().setFromObject(this.model);
        const center = box.getCenter(new THREE.Vector3());
        this.model.position.x -= center.x;
        this.model.position.z -= center.z;
        this.model.position.y -= box.min.y;

        this.model.traverse((child) => {
            if (!child.isMesh) return;
            child.castShadow = true;
            child.receiveShadow = true;
            const materials = Array.isArray(child.material) ? child.material : [child.material];
            materials.filter(Boolean).forEach((material) => {
                if ('envMapIntensity' in material) material.envMapIntensity = 1.15;
            });
        });

        const mountedBox = new THREE.Box3().setFromObject(this.model);
        const mountedSize = mountedBox.getSize(new THREE.Vector3());
        this.controls.target.set(0, Math.max(0.8, mountedSize.y * 0.38), 0);
        this.defaultTarget.copy(this.controls.target);
        this.scene.add(this.model);
        this.controls.update();
    }

    setLoadingProgress(progress) {
        if (!this.loadingText) return;
        this.loadingText.textContent = progress == null
            ? 'Preparing 3D studio'
            : `Preparing 3D studio · ${Math.min(100, Math.round(progress * 100))}%`;
    }

    showReadyState() {
        this.loading.hidden = true;
        this.errorPanel.hidden = true;
        this.toolbar.hidden = false;
        this.hint.hidden = false;
        this.host.setAttribute('aria-hidden', 'false');
        this.stage.classList.remove('has-viewer-error');
        this.stage.classList.add('is-3d-ready');
    }

    showError(message) {
        if (this.loading) this.loading.hidden = true;
        if (this.errorMessage) this.errorMessage.textContent = message;
        if (this.errorPanel) this.errorPanel.hidden = false;
        if (this.toolbar) this.toolbar.hidden = true;
        if (this.hint) this.hint.hidden = true;
        this.stage.classList.remove('is-3d-ready');
        this.stage.classList.add('has-viewer-error');
        this.stopRendering();
    }

    handleReset() {
        this.camera.position.copy(this.defaultCameraPosition);
        this.controls.target.copy(this.defaultTarget);
        this.controls.update();
        this.renderer.render(this.scene, this.camera);
        this.stage.classList.add('has-just-reset');
        window.setTimeout(() => this.stage.classList.remove('has-just-reset'), 450);
    }

    handleContextLost(event) {
        event.preventDefault();
        this.showError('WebGL context was lost. Reload the page to reopen the interactive studio.');
    }

    handleVisibility() {
        if (document.hidden) this.stopRendering();
        else if (this.inViewport) this.startRendering();
    }

    resize() {
        if (!this.renderer || !this.camera || this.disposed) return;
        const width = Math.max(1, this.stage.clientWidth);
        const height = Math.max(1, this.stage.clientHeight);
        this.camera.aspect = width / height;
        this.camera.fov = width < 768 ? 52 : 38;
        this.camera.updateProjectionMatrix();
        this.renderer.setSize(width, height, false);
        this.renderer.render(this.scene, this.camera);
    }

    startRendering() {
        if (this.frameId != null || !this.renderer || !this.model || this.disposed || document.hidden || !this.inViewport) return;
        this.frameId = requestAnimationFrame(this.animate);
    }

    stopRendering() {
        if (this.frameId != null) cancelAnimationFrame(this.frameId);
        this.frameId = null;
    }

    animate() {
        if (this.disposed || !this.inViewport || document.hidden) {
            this.frameId = null;
            return;
        }
        this.controls.update();
        this.renderer.render(this.scene, this.camera);
        this.frameId = requestAnimationFrame(this.animate);
    }

    dispose() {
        if (this.disposed) return;
        this.disposed = true;
        this.stopRendering();
        this.abortController.abort();
        this.resizeObserver?.disconnect();
        this.intersectionObserver?.disconnect();
        this.controls?.dispose();

        this.model?.traverse((child) => {
            child.geometry?.dispose();
            if (Array.isArray(child.material)) child.material.forEach(disposeMaterial);
            else if (child.material) disposeMaterial(child.material);
        });
        this.ground?.geometry.dispose();
        this.ground?.material.dispose();
        this.environmentTexture?.dispose();
        this.renderer?.dispose();
        this.renderer?.forceContextLoss();
        this.host?.replaceChildren();
    }
}

export function initializeVehicleViewers() {
    document.querySelectorAll('[data-three-stage][data-model-url]').forEach((stage) => {
        if (initializedStages.has(stage)) return;
        initializedStages.add(stage);

        const viewer = new VehicleViewer(stage);
        stage.apexVehicleViewer = viewer;
        viewer.init();
    });
}
