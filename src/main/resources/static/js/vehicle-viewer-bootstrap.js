const showBootstrapError = () => {
    document.querySelectorAll('[data-three-stage][data-model-url]').forEach((stage) => {
        const loading = stage.querySelector('[data-viewer-loading]');
        const error = stage.querySelector('[data-viewer-error]');
        const message = stage.querySelector('[data-viewer-error-message]');

        if (loading) loading.hidden = true;
        if (message) message.textContent = 'Three.js could not be loaded. The studio image remains available.';
        if (error) error.hidden = false;
        stage.classList.add('has-viewer-error');
    });
};

import('/js/vehicle-viewer.js')
    .then(({ initializeVehicleViewers }) => initializeVehicleViewers())
    .catch((error) => {
        console.error('APEX 3D viewer bootstrap failed.', error);
        showBootstrapError();
    });
